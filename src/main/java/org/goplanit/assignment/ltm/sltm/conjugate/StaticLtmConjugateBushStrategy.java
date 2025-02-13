package org.goplanit.assignment.ltm.sltm.conjugate;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.network.transport.ConjugateTransportModelNetworkUtils;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.network.transport.TransportModelNetworkUtils;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.functionalinterface.TriFunction;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.ojalgo.array.Array1D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmConjugateBushStrategy
        extends StaticLtmBushStrategyBase<ConjugateDirectedVertex, ConjugateEdgeSegment,ConjugateDestinationBush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmConjugateBushStrategy.class.getCanonicalName());

  /**
   * Given non conjugate costs for link segments, expand to concjugate segments (turns)
   * TODO: when everything is conjugate, avoid calling this multiple times as we do now as it is costly
   *   at that point process flow can just use conjugate costs rather than non-conjugate costs.
   *
   * @param theMode to use
   * @param nonConjugateLinkSegmentCosts original costs
   * @return conjugate projected costs
   */
  private double[] expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
          Mode theMode, double[] nonConjugateLinkSegmentCosts){
    final double[] conjugateSegmentCosts =
            new double[conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers()];

    // Function to expand from original entry segment costs to conjugate link segment costs
    Consumer<ConjugateEdgeSegment> adoptOriginalEdgeSegmentCostFunc = cs -> {
      double conjugateCost = 0.0;
      if(cs.getOriginalAdjacentEdgeSegments().first() != null){
        conjugateCost = nonConjugateLinkSegmentCosts[(int)cs.getOriginalAdjacentEdgeSegments().first().getId()];
      }
      conjugateSegmentCosts[(int)cs.getId()] = conjugateCost;
    };

    // apply to physical layer...
    var conjugatePhysicalLayer =
            conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    conjugatePhysicalLayer.getLinkSegments().forEach(adoptOriginalEdgeSegmentCostFunc);
    // and apply to virtual layer...
    var conjugateVirtualLayer = conjugateTransportModelNetwork.getVirtualNetwork().getLayer();
    conjugateVirtualLayer.getConnectoidSegments().forEach(adoptOriginalEdgeSegmentCostFunc);

    // Now account for zero flow discontinuity by rerunning all nodes in turn based mode to obtain
    // turn level acceptance factors which we can then use to update the turn costs for zero flow
    // turns such that they become (realistically) unattractive as options for when finding new PASs
    // todo: costly, so ideally only do once per iteration, but we now do it on the fly
    updateZeroFlowDiscontinuityCongestedTurnCosts(theMode, conjugateSegmentCosts);

    return conjugateSegmentCosts;
  }

  /**
   * Populate with initial demand for given OD and shortest conjugate bush DAG
   *
   * @param conjugateDestinationBush  to populate
   * @param  originConjugateVertex     to use
   * @param odDemandPcuH     to use
   * @param destinationOriginInvertedDag            to use
   *
   */
  private void initialiseConjugateBushForOrigin(
          final ConjugateDestinationBush conjugateDestinationBush,
          final ConjugateDirectedVertex originConjugateVertex,
          final Double odDemandPcuH,
          final ACyclicSubGraph destinationOriginInvertedDag) {

    /* get topological sorted vertices to process from origin-to-destination in direction of odDag, so invert iterator since it runs
       from destination to origin currently */
    var vertexIter = destinationOriginInvertedDag.getTopologicalIterator(true, true);

    /* proceed until we arrive at our origin */
    DirectedVertex currVertex = null;
    while (vertexIter.hasNext() && !originConjugateVertex.equals(currVertex)) {
      currVertex = vertexIter.next();
    }

    /* populate initial demand on links of shortest path */
    var helper = ConjugateBushSimpleInitialiserHelper.create(
            conjugateDestinationBush, destinationOriginInvertedDag);
    helper.executeOdBushInitialisation(currVertex, odDemandPcuH, vertexIter);
  }

  /** because the bushes will be created and tracked in conjugate network form, we create a conjugate version of the
   * entire network from which the bushes draw */
  protected final ConjugateTransportModelNetwork conjugateTransportModelNetwork;

  /** inverse mapping from centroid vertices to their conjugate node */
  protected final Map<CentroidVertex, ConjugateConnectoidNode> centroid2ConjugateNodeMapping;

  /** inverse mapping from turn edge segments (double key) to conjugate edge segment */
  protected final  MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  /**
   * Create a shortest bush search algorithm for the conjugate bushes based on conjugate edge segments and costs
   *
   * @param nonConjugateLinkSegmentCosts to use
   * @return create shortest bush algorithm
   */
  @Override
  protected ShortestBushGeneralised createNetworkShortestBushAlgo(Mode theMode, double[] nonConjugateLinkSegmentCosts) {
    //todo: once base implementation works, replace nonConjugateLinkSegment costs with turn based costs throughout
    // implementation. For now project non conjugate link segment costs to conjugate segments by using the entry segment
    // as the point of reference
    double[] conjugateSegmentCosts =
            expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, nonConjugateLinkSegmentCosts);
    final int numberOfVertices = this.conjugateTransportModelNetwork.getNumberOfVerticesAllLayers();
    return new ShortestBushGeneralised(conjugateSegmentCosts, numberOfVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] conjugateLinkSegmentCosts) {
    final int numberOfVertices = this.conjugateTransportModelNetwork.getNumberOfVerticesAllLayers();
    return new ShortestPathDijkstra(conjugateLinkSegmentCosts, numberOfVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updatePasCosts(Mode theMode, double[] originalNetworkLinkSegmentCosts) {
    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, originalNetworkLinkSegmentCosts);

    // execute cost update based on conjugate costs
    pasManager.updateActivePassCosts(conjSegmentCosts);
    pasManager.updateInactivePassCosts(conjSegmentCosts);

  }

  /**
   * For all congested nodes, recompute node model in turn based setting to obtain turn costs considering zero flow
   * discontinuities. For any such turns with: (i) currently uncongested, (ii) having zero flow, (iii) leading into a
   * congested link (iv) having non-zero flow would cause the incoming link become congested: we replace the current
   * cost with one based on the limit towards zero flow cost produced (which is higher) to ensure a PAS is not
   * considered unless it is attractive even under this situation.
   *
   * @param theMode to use
   * @param conjSegmentCosts to update zero-flow turns at a discontinuity to utilise most restricting cost rather than
   *                         least restricting cost
   */
  private void updateZeroFlowDiscontinuityCongestedTurnCosts(final Mode theMode, double[] conjSegmentCosts) {
    if(getLoading().getSplittingRateData() == null){
      return; // avoid null pointer at initialisation
    }

    //1. identify congested nodes
    var trackedNodes = getLoading().getSplittingRateData().getTrackedNodes();

    // prep
    final LongAdder numDiscontinuitiesUpdated = new LongAdder();
    final AbstractPhysicalCost physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    final AbstractVirtualCost virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);
    var flowAcceptanceFactors = getLoading().getCurrentFlowAcceptanceFactors();
    var linkSendingFlows = getLoading().getCurrentInflowsPcuH();
    Function<EdgeSegment, Array1D<Double>> getEntrySegmentSplittingRates =
        es -> getLoading().getSplittingRateData().getSplittingRates(es).copy();
    // end prep

    // Prep Lambda Function that will ultimately perform the cost update after the node model calculation
    TriConsumer<EdgeSegment, EdgeSegment, Double> discontinuityTurnCostReplacementConsumer = (entry, exit, alpha) ->
      {
        if(entry.getOppositeDirectionSegment() == exit){
          return;
        }
        var nlAppliedFlowAcceptanceFactor = flowAcceptanceFactors[(int)entry.getId()];
        if(Precision.greaterEqual(alpha, nlAppliedFlowAcceptanceFactor, Precision.EPSILON_9)){
          return;
        }
        // discontinuity found since the turn acceptance factor is more restricting than the link based one applied in loading
        // this only happens at a zero flow discontinuity.

        //HACK: because the cost calculation hides its internal workings for now we modify the link outflow locally
        //      based on the changed alphas.
        // TODO: create a nice fix so we can compute generalised cost on-the-fly for a given flow acceptance factor
        double originalNlOutflow = getLoading().getCurrentOutflowsPcuH()[(int)entry.getId()];
        double outflowConsistentWithNonZeroTurnFlow = getLoading().getCurrentInflowsPcuH()[(int)entry.getId()] * alpha;
        getLoading().getCurrentOutflowsPcuH()[(int)entry.getId()] = outflowConsistentWithNonZeroTurnFlow;
        double disContinuitySegmentCost = (entry instanceof ConnectoidSegment) ?
            virtualCost.getGeneralisedCost(theMode, (ConnectoidSegment) entry):
            physicalCost.getGeneralisedCost(theMode, (MacroscopicLinkSegment) entry);
        getLoading().getCurrentOutflowsPcuH()[(int)entry.getId()] = originalNlOutflow; // place original cost back
        assert(originalNlOutflow > outflowConsistentWithNonZeroTurnFlow);

        //3. overwrite existing costs for turns where discontinuity was found
        var conjugateSegment = turn2ConjugateSegmentMapping.get(entry, exit);
        assert (conjSegmentCosts[(int)conjugateSegment.getId()] <= disContinuitySegmentCost);
        conjSegmentCosts[(int)conjugateSegment.getId()] = disContinuitySegmentCost;
        numDiscontinuitiesUpdated.increment();
      };
    //

    //2. for each congested node rerun node in turn based form
    Predicate<DirectedVertex> hasCongestedEntrySegment = n -> IterableUtils.asStream(
        n.getEntryEdgeSegments()).anyMatch(es -> flowAcceptanceFactors[(int)es.getId()] < 1 );
    for(var node : trackedNodes){
      if(!hasCongestedEntrySegment.test(node)){
        continue;
      }
      var inCapacities = TampereNodeModelUtils.createIncomingCapacities(node);
      var receivingFlows = TampereNodeModelUtils.createOutgoingReceivingFlows(node);
      var turnSendingFlows = TampereNodeModelUtils.createTurnSendingFlowsUsingSplittingRates(
          node, linkSendingFlows, getEntrySegmentSplittingRates);

      // run node model in turn aware setup
      var turnBasedFlowAcceptanceFactors =
          TampereNodeModel.of(inCapacities, receivingFlows, turnSendingFlows).runTurnBased();
      TampereNodeModelUtils.forEachTurnBasedResult(
          node, turnBasedFlowAcceptanceFactors, discontinuityTurnCostReplacementConsumer);
    }

    if(numDiscontinuitiesUpdated.intValue()>0) {
      LOGGER.info(String.format("Updated costs for %d zero-flow turns with a discontinuous cost function",
          numDiscontinuitiesUpdated.intValue()));
    }
  }

  /**
   * Check if an existing PAS exists that terminates/starts (depending on bush config) at the given bush vertex. If so,
   * it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush
   * (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * </ul>
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper
   * segment might or might not overlap with, as long as it is close enough to the potential reduced cost we'll
   * take it to avoid exponential growth of PASs)
   *
   * @param bush        to consider
   * @param reducedCostVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   *
   * @return PAS when a match is found and null otherwise (PAS is already registered as part of this call)
   */
  protected Pas<ConjugateDirectedVertex,ConjugateEdgeSegment> extendConjugateBushWithSuitableExistingPas(
          final ConjugateDestinationBush bush,
          final ConjugateDirectedVertex reducedCostVertex,
          final double reducedCost) {

    // disable effectiveness check: at some point it was considered effective for some bush, therefore we allow
    // for it to be reactivated
    boolean checkEffectiveness = false;
    boolean inActive = false;
    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    var suitablePas =
            pasManager.findFirstSuitableActivePas(bush, reducedCostVertex, alphas, reducedCost, false);
    if (suitablePas == null) {
      suitablePas =
              pasManager.findFirstSuitableInactivePas(
                      bush, reducedCostVertex, alphas, reducedCost, false);
      inActive = true;
    }
    if (suitablePas == null) {
      return null;
    }

    /*
     * found -> register bush for future flow shifting, if PAS was previously inactive, reactivate it so it is
     * considered again.
     */
    if(inActive){
      pasManager.reactivatePas(suitablePas);
    }
    boolean newlyRegistered = suitablePas.registerBush(bush);
    if (newlyRegistered && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("Destination %s added to PAS %s",
              bush.getRootZoneVertex().getParent().getParentZone().getXmlId(), suitablePas));
    }
    return suitablePas;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that
   * it is considered sufficiently effective the bush is registered on it.
   *
   * @param bush              to identify new PAS for
   * @param reducedCostVertex to use for creating the PAS as a cheaper path to the root exists at this vertex
   * @param conjugateNetworkMinPaths   the current conjugate network shortest path tree
   * @param reducedCost       to check if new PAS is considered effective
   * @param conjugateLinkSegmentCosts  to check if new PAS is considered effective
   * @return new created PAS if successfully created, null otherwise, the boolean indicates if it indeed is a brand
   * new PAS or for some reason we still reused an existing one
   */
  protected Pas<ConjugateDirectedVertex,ConjugateEdgeSegment> extendConjugateBushWithNewPas(
          final ConjugateDestinationBush bush,
          final ConjugateDirectedVertex reducedCostVertex,
          final ShortestPathResult conjugateNetworkMinPaths,
          double reducedCost,
          double[] conjugateLinkSegmentCosts) {

    /* Label all vertices on shortest path root-reducedCostVertex as -1, and PAS reference vertex itself as 1 */
    final short[] conjAlternativeSegmentVertexLabels =
            new short[conjugateTransportModelNetwork.getNumberOfVerticesAllLayers()];
    conjAlternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    int numShortestPathEdgeSegments = conjugateNetworkMinPaths.forEachNextEdgeSegment(
            bush.getRootVertex(),
            reducedCostVertex,
            (edgeSegment) -> conjAlternativeSegmentVertexLabels[(int)
                    conjugateNetworkMinPaths.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    /* Identify when it coincides again with bush (closer to root) using back link tree BF search */
    var highCostSubPathResultPair =
            bush.findBushAlternativeSubpathByBackLinkTree(
                    reducedCostVertex,
                    (ConjugateEdgeSegment) conjugateNetworkMinPaths.getNextEdgeSegmentForVertex(reducedCostVertex),
                    conjAlternativeSegmentVertexLabels);
    if (highCostSubPathResultPair == null || highCostSubPathResultPair.first() == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS */
      LOGGER.info(String.format(
              "Unable to create new PAS for conjugate bush rooted at vertex (%s), despite shorter path found on " +
                      "network to reduced cost vertex (%s)",
              bush.getRootVertex().getIdsAsString(), reducedCostVertex.getIdsAsString()));
      return null;
    }

    /* create the PAS and register bush on it */
    boolean truncateSpareArrayCapacity = true;
    var coincideCloserToRootVertex = highCostSubPathResultPair.first();
    Map<ConjugateDirectedVertex, ConjugateEdgeSegment> backLinkTreeAsMap = highCostSubPathResultPair.second();

    /* S1 */
    ConjugateEdgeSegment[] s1 = PasManager.createSubPathArrayFrom(
            coincideCloserToRootVertex,
            reducedCostVertex,
            conjugateNetworkMinPaths,
            new ConjugateEdgeSegment[numShortestPathEdgeSegments],
            truncateSpareArrayCapacity);
    var cycleInducingSegment = bush.determineIntroduceCycle(s1);
    if (cycleInducingSegment != null) {
      /*
       * this can happen if the merge vertex can only be reached by traversing the bush in opposite direction of
       * existing edge segment on the bush. In which case, an alternative PAS further upstream should be considered,
       * now identify this and ignore PAS as it is sub-optimal and cycle inducing
       */
//      LOGGER.info(String.format("Newly identified PAS alternative for bush rooted at vertex (%s) would introduce cycle on low cost alternative (edge segment [%s]), ignore",
//          bush.getRootVertex().getIdsAsString(), cycleInducingSegment.getIdsAsString()));
      return null;
    }

    /* S2 */
    ConjugateEdgeSegment[] s2 = PasManager.createSubPathArrayFrom(
            coincideCloserToRootVertex,
            reducedCostVertex,
            bush.getShortestSearchType(),
            backLinkTreeAsMap,
            new ConjugateEdgeSegment[highCostSubPathResultPair.second().size()],
            truncateSpareArrayCapacity);

    var existingPas = pasManager.findMatchingActivePas(s1, s2);
    if (existingPas != null) {
      // exists already but was discarded as option for this bush (otherwise we would not ask for a new PAS to be
      // created not able to create new PAS using this S1/S2 alternative
      //todo: consider alternative S1/S2 creation using DFS for example to see if an alternative partially overlapping PAS
      // could help. For now we just accept no new PAS is to be created for this bush at this vertex
      return null;
    }
    existingPas = pasManager.findMatchingInactivePas(s1, s2);
    if (existingPas != null) {
      // same reason as above
      return null;
    }

    double highCostAlternativeCost = PasManager.computeCost(s2, conjugateLinkSegmentCosts);
    double lowCostAlternativeCost = PasManager.computeCost(s1, conjugateLinkSegmentCosts);
    if (!PasManager.isPasEffectiveForBush(
            s2,
            highCostAlternativeCost,
            lowCostAlternativeCost,
            bush,
            getLoading().getCurrentFlowAcceptanceFactors(),
            reducedCost)) {
      return null;
    }

    /* New pas */
    var pas = pasManager.createAndRegisterNewPas(bush, s1, s2);
    pas.updateCost(conjugateLinkSegmentCosts);
    /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance
     * factor information */
    getLoading().activateNodeTrackingFor(pas);
    if(getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("Created new PAS: %s", pas));
    }
    return pas;
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected ConjugateDestinationBush[] createEmptyBushes(Mode mode) {

    var conjugateNetworkLayer =
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    Zoning zoning = getTransportNetwork().getZoning();
    ConjugateDestinationBush[] conjugateBushes = new ConjugateDestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands(mode);
    for (var destination : zoning.getOdZones()) {
      ConjugateDestinationBush bush = null;
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          /* centroid vertex to which demand will be mapped */
          var destinationCentroidVertex = findDestinationCentroidVertex(destination);
          if(destinationCentroidVertex == null){
            LOGGER.severe(String.format("Destination zone (%s) without centroid vertex to connect to network, " +
                "this shouldn't happen", destination.getIdsAsString()));
            continue;
          }

          /* collect conjugate root node for this conjugate destination bush */
          var rootConjugateConnectoidNode =
              centroid2ConjugateNodeMapping.get(destinationCentroidVertex);

          /* register new bush */
          bush = new ConjugateDestinationBush(
              conjugateNetworkLayer.getLayerIdGroupingToken(),
              destinationCentroidVertex,
              rootConjugateConnectoidNode,
              /* all "real" turns as conjugate segment is a turn */
              conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers(),
              turn2ConjugateSegmentMapping);
          conjugateBushes[(int) destination.getOdZoneId()] = bush;
          break;
        }
      }
    }
    return conjugateBushes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialiseBush(
          ConjugateDestinationBush bush,
          Zoning zoning,
          OdDemands odDemands,
          ShortestBushGeneralised shortestBushAlgorithm) {

    final var destinationCentroidVertex = bush.getRootZoneVertex();
    final OdZone destination = (OdZone) destinationCentroidVertex.getParent().getParentZone();
    final var destinationConjugateReferenceVertex = centroid2ConjugateNodeMapping.get(destinationCentroidVertex);
    ShortestBushResult allToOneResult = null;

    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        /* find all-to-one shortest paths */
        if (allToOneResult == null) {
          allToOneResult = shortestBushAlgorithm.executeAllToOne(destinationConjugateReferenceVertex);
        }

        /* initialise conjugate bush with this origin shortest path(s) */
        var originConjugateReferenceVertex = centroid2ConjugateNodeMapping.get(findOriginCentroidVertex(origin));
        var destinationOriginInvertedDag =
                allToOneResult.createDirectedAcyclicSubGraph(
                        getIdGroupingToken(), originConjugateReferenceVertex, destinationConjugateReferenceVertex);
        if (destinationOriginInvertedDag.isEmpty()) {
          LOGGER.severe(String.format("Unable to create conjugate bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
          continue;
        }

        bush.addOriginDemandPcuH(originConjugateReferenceVertex, currOdDemand);
        initialiseConjugateBushForOrigin(
                bush, originConjugateReferenceVertex, currOdDemand, destinationOriginInvertedDag);
        LOGGER.info(bush.toString());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment> createPasFlowShiftExecutor(
          final Pas<ConjugateDirectedVertex, ConjugateEdgeSegment> pas, final StaticLtmSettings settings) {
    return new PasFlowShiftConjugateDestinationBasedExecutor(pas, settings);
  }

  /**
   * Create conjugate bush based network loading implementation
   *
   * @return created loading implementation supporting conjugate bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushConjugate createNetworkLoading() {
    return new StaticLtmLoadingBushConjugate(
            getIdGroupingToken(),
            getAssignmentId(),
            turn2ConjugateSegmentMapping,
            this.conjugateTransportModelNetwork,
            getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushConjugate getLoading() {
    return (StaticLtmLoadingBushConjugate) super.getLoading();
  }

  /**
   * Based on provided original network link segment costs see if we can update the existing collection of PASs
   *
   * @param mode             to use
   * @param nonConjugateLinkSegmentCosts to use
   * @param updateGap        flag
   * @param logAll           flag
   * @return newly created PASs
   */
  @Override
  protected Pair<Collection<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>, Collection<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>>
  updateBushPass(Mode mode, double[] nonConjugateLinkSegmentCosts, boolean updateGap, boolean logAll){

    double totalMinCostForGap = 0; // track during bush traversal to get min OD costs based on shortest paths
    double totalRealisedCostForGap = 0;
    if(updateGap) {
      // costs as they currently are utilising the unconstrained demand as a point of reference
      for (var linkSegment : getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode).getLinkSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
      for (var linkSegment : getTransportNetwork().getVirtualNetwork().getLayer().getConnectoidSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
    }

    //todo --> should be sets
    List<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> newPass = new ArrayList<>();
    List<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> existingPassWithNewBushes = new ArrayList<>();

    // method overridden for conjugate implementation resulting in conjugate compatible shortest path search using
    // conjugate link segment costs. For maintainability/readability expansion to conjugate costs occurs within method for now...
    final var conjLinkSegmentCosts =
            expandNonConjugateLinkSegmentCostToConjugateSegmentCost(mode, nonConjugateLinkSegmentCosts);
    final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
    for (var conjBush : getBushes()) {
      if (conjBush == null) {
        continue;
      }

      /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root
       * is destination) */
      //todo: we do not yet account for if the path is used --> we should because we will likely get unused max cost paths now!
      var conjBushMinMaxPaths = conjBush.computeMinMaxShortestPaths(
              conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
      if (conjBushMinMaxPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain conjugate min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }
      conjBushMinMaxPaths.setMinPathState(false);

      /* network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush
       * where destination is root) */
      // todo below rewritten but not yet tested, so continue here
      var conjNetworkMinPaths =
              conjNetworkShortestPathAlgo.execute(conjBush.getShortestSearchType(), conjBush.getRootVertex());
      if (conjNetworkMinPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain conjugate network min paths for conjugate bush, " +
                        "this shouldn't happen, skip updateBushPass"));
        continue;
      }

      if(updateGap) {
        // update/track total min cost across bushes(Ods) for gap calculation
        var odDemands = getOdDemands(mode);
        var destination = conjBush.getDestination().getParent().getParentZone();
        for (var originVertex : conjBush.getOriginVertices()) {
          var origin = ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone();
          double odDemand = odDemands.getValue(origin, destination);
          double minOdCost = conjNetworkMinPaths.getCostToReach(originVertex);
          totalMinCostForGap += minOdCost * odDemand;
        }
      }

      /* find (new) matching PASs - start with new PAS close to origin exploration first
       *  todo: this is a choice, could choose differently and going with close to destination seems safer */
      var bushVertexIter = conjBush.isInverted() ?
              conjBush.getInvertedTopologicalIterator() : conjBush.getTopologicalIterator();
      while(bushVertexIter.hasNext()) {
        ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();
        ConjugateEdgeSegment reducedCostSegment =
                (ConjugateEdgeSegment) conjNetworkMinPaths.getNextEdgeSegmentForVertex(conjBushVertex);
        if(reducedCostSegment == null) {
          continue;
        }

        double reducedCost =
                conjBushMinMaxPaths.getCostToReach(conjBushVertex) - conjNetworkMinPaths.getCostToReach(conjBushVertex);
        if(reducedCost <= 0){
          continue;
        }

        if(conjBushMinMaxPaths.getNextEdgeSegmentForVertex(conjBushVertex).equals(
                conjNetworkMinPaths.getNextEdgeSegmentForVertex(conjBushVertex))){
          // not the location that min path and bush min path split, so do not create the start point of PAS here yet
          continue;
        }

        /* when bush does not contain the opposite direction which would cause a cycle it is worth checking,
        *  we also do not allow u-turns (on original network), so disallow that as well */
        boolean disallowUTurns = true;
        boolean viableSearch =
                (reducedCostSegment.isOriginalEdgeSegmentsUTurn() && disallowUTurns) ||
                reducedCostSegment.getOppositeDirectionSegment()==null ||
                        !conjBush.contains(reducedCostSegment.getOppositeDirectionSegment());
        if (!viableSearch) {
          // preferred alternative cannot be added due to bush triggering a cycle, or u-turn inclusion if we would
          continue;
        }

        var existingRegisteredPas = extendConjugateBushWithSuitableExistingPas(conjBush, conjBushVertex, reducedCost);
        if (existingRegisteredPas != null) {
          if(isDestinationTrackedForLogging(conjBush) || logAll){
            LOGGER.info(String.format("Registered suitable existing PAS (%s) on bush (%s)",
                    existingRegisteredPas, conjBush));
          }
          existingPassWithNewBushes.add(existingRegisteredPas);
          continue;
        }

        /* no suitable match, attempt creating an entirely new PAS */
        var newPas = extendConjugateBushWithNewPas(
                conjBush, conjBushVertex, conjNetworkMinPaths, reducedCost, conjLinkSegmentCosts);
        if (newPas == null) {
          continue;
        }

        // truly new PAS
        newPass.add(newPas);
        newPas.updateCost(conjLinkSegmentCosts);
        if(isDestinationTrackedForLogging(conjBush) || logAll){
          LOGGER.info(String.format("Registered new PAS (%s) on conjugate bush (%s)",
                  newPas, conjBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }

        // BRANCH SHIFT
        {
          // NOTE: since we will perform an update on all PASs it seems illogical to also explicitly register the
          // required branch shifts since they will be carried out regardless. Hence we do not log a warning nor
          // implement the branch shift until it appears necessary

          /* no suitable new or existing PAS could be found given the conditions applied, do a branch shift instead */
          // LOGGER.info("No existing/new PAS found that satisfies flow/cost effective conditions for origin bush %s, consider branch shift - not yet implemented");
          // TODO: currently not implemented yet -> requires shifting flow on existing bush with the given vertex as the end point
        }

      }
    }

    if(updateGap){
      var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);
      // both costs have already been normalised to demand so use unity to transfer as is
      // ideally we'd use a link based gap but this is not ideal with the path based implementation we also support
      // for sLTM
      gapFunction.increaseMinimumPathCosts(totalMinCostForGap,1);
      gapFunction.increaseAbsolutePathGap(totalRealisedCostForGap, 1, totalMinCostForGap);
    }

    return Pair.of(newPass,existingPassWithNewBushes);
  }

  /**
   * Constructor
   *
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  public StaticLtmConjugateBushStrategy(
          final IdGroupingToken idGroupingToken,
          long assignmentId,
          final TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportModelNetwork,
          final StaticLtmSettings settings,
          final TrafficAssignmentComponentAccessee taComponents) {
    /* destination based bushes are inverted, so PASs are to be registered based on vertex farthest from root,
     * i.e, farthest from destination, so at the upstream point of the PAS at its diverge (hence true at end of super)*/
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents, true);

    // construct conjugate version of original transport model network, to be used by all conjugate bushes
    this.conjugateTransportModelNetwork = transportModelNetwork.createConjugate(
            TransportModelNetworkUtils.generateDerivedConjugateIdGroupingToken(transportModelNetwork));
    conjugateTransportModelNetwork.logInfo("");

    centroid2ConjugateNodeMapping =
            VirtualNetworkUtils.createCentroidVertexToConjugateNodeMapping(
                    conjugateTransportModelNetwork.getVirtualNetwork().getLayer());
    turn2ConjugateSegmentMapping =
            ConjugateTransportModelNetworkUtils.createOriginalSegmentsToConjugateSegmentsMapping(
                    conjugateTransportModelNetwork);

    // todo: remove at some point as for large networks this will mean a lot of logging!
    boolean logMapping = false;
    if(logMapping) {
      conjugateTransportModelNetwork.logConjugateToOriginalMapping();
    }
  }

  /**
   * Access to conjugate transport model network this strategy relies on
   *
   * @return conjugate transport model network
   */
  public ConjugateTransportModelNetwork getConjugateTransportModelNetwork() {
    return conjugateTransportModelNetwork;
  }

  /**
   *
   * @return description of this strategy for sLTM
   */
  @Override
  public String getDescription() {
    return "Conjugate destination-based Bush";
  }

}
