package org.goplanit.assignment.ltm.sltm;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.algorithms.shortest.*;
import org.goplanit.assignment.common.bush.ConjugateBushTurnData;
import org.goplanit.assignment.common.bush.ConjugateDestinationBush;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmSimulationData;
import org.goplanit.assignment.ltm.sltm.consumer.nodemodel.DiscontinuityTurnCostReplacementConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.assignment.common.pas.Pas;
import org.goplanit.assignment.common.pas.PasStatus;
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
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.ojalgo.array.Array1D;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.goplanit.assignment.ltm.sltm.util.ConjugateBushUtils.isEligibleForAdding;

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

  private double prevNetworkRealisedCost = Double.MAX_VALUE; // use as an additional way to update bush smoothing steps

  /** access to original bush turn flows. To be used for constraining identifying available sending flows
   * for flow shifts. Requires updating when constraining further as part of this instance conducting
   * flow shifts, so it is available to other PASs as a constraint.
   * Note: it should NOT contain all original turn flows per bush, only those where the original turn flow
   * was at some point was reduced to save memory.
   * NOTE: not owned by this executor, owned by parent strategy
   * todo: this injected approach is ugly, needs refactoring at some point
   */
  private Map<ConjugateDestinationBush, ConjugateBushTurnData> originalBushTurnFlowTracker;

  /**
   * Update PAS status based on flow acceptance factors (without considering impact of any potential flow shifts)
   *
   * @param conjugatePas to update
   * @param nonConjAcceptanceFactors to use
   */
  private void updatePasStatusBeforeFlowShift(
          Pas<ConjugateDirectedVertex, ConjugateEdgeSegment> conjugatePas,
          double[] nonConjAcceptanceFactors) {
    // test if conj segment is congested by considering original entry segment acceptance factor
    Predicate<ConjugateEdgeSegment> congestedPred = es -> es.hasOriginalEntryEdgeSegment() &&
            Precision.smaller(
                    nonConjAcceptanceFactors[(int)es.getOriginalAdjacentEdgeSegments().first().getId()],
                    1,
                    Precision.EPSILON_9);

    if( conjugatePas.anyMatch(congestedPred,false) || conjugatePas.anyMatch( congestedPred,true)){
      conjugatePas.updateStatus(PasStatus.CONGESTED);
    }else{
      conjugatePas.updateStatus(PasStatus.UNCONGESTED_WITHOUT_SHIFT);
    }
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
  protected ShortestPathGeneralised createInitialNetworkShortestSearchTreeAlgo(
      Mode theMode, double[] nonConjugateLinkSegmentCosts) {

    // for initialisation there is no flow, so no point in considering discontinuities
    boolean considerDiscontinuities = false;
    //todo: once base implementation works, replace nonConjugateLinkSegment costs with turn based costs throughout
    // implementation. For now project non conjugate link segment costs to conjugate segments by using the entry segment
    // as the point of reference
    double[] conjugateSegmentCosts = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
        theMode, nonConjugateLinkSegmentCosts, considerDiscontinuities);
    return createNetworkShortestPathAlgo(conjugateSegmentCosts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] conjugateLinkSegmentCosts) {
    final int numberOfVertices = this.conjugateTransportModelNetwork.getNumberOfVerticesAllLayers();
    return new ShortestPathDijkstra(conjugateLinkSegmentCosts, numberOfVertices);
  }

  @Override
  protected void hookBeforePasUpdate(
      Collection<PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment>> pasExecutors) {
    // reset and inject empty tracking container for original turn flows that may get adjusted and therefore need to
    // be preserved and accessible as a constraint on available flow to shift
    this.originalBushTurnFlowTracker = new HashMap<>();
    pasExecutors.forEach(
        pe -> ((PasFlowShiftConjugateDestinationBasedExecutor)pe).injectOriginalBushTurnFlowAccess(
            originalBushTurnFlowTracker));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updatePasCosts(Mode theMode, double[] originalNetworkLinkSegmentCosts) {
    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
        theMode, originalNetworkLinkSegmentCosts, true);

    // execute cost update based on conjugate costs
    pasManager.updateActivePassCosts(conjSegmentCosts);
    pasManager.updateInactivePassCosts(conjSegmentCosts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updatePasStatusBeforeFlowShifts(Mode theMode, double[] networkLinkSegmentFlowAcceptanceFactors) {
    // execute status update without considering any flow shift information
    pasManager.forEachActivePas( p -> updatePasStatusBeforeFlowShift(p, networkLinkSegmentFlowAcceptanceFactors));
    pasManager.forEachInactivePas(p -> updatePasStatusBeforeFlowShift(p, networkLinkSegmentFlowAcceptanceFactors));
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
    DiscontinuityTurnCostReplacementConsumer discontinuityTurnCostReplacementConsumer =
        new DiscontinuityTurnCostReplacementConsumer(
            getLoading(), theMode, physicalCost, virtualCost, turn2ConjugateSegmentMapping, conjSegmentCosts);

    //2. for each congested node rerun node in turn based form
    Predicate<DirectedVertex> hasCongestedEntrySegment = n -> IterableUtils.asStream(
        n.getEntryEdgeSegments()).anyMatch(es -> (flowAcceptanceFactors[(int)es.getId()] + Precision.EPSILON_9) < 1);
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

    if(getSettings().isDetailedLogging() && discontinuityTurnCostReplacementConsumer.getNumDiscontinuitiesUpdated()>0) {
      LOGGER.info(String.format("Updated costs for %d zero-flow turns with a discontinuous cost function",
          discontinuityTurnCostReplacementConsumer.getNumDiscontinuitiesUpdated()));
    }
  }

  /**
   * calculate realised cost for gap based on non-discontinuous costs....so normal realised network level costs
   *
   * @param theMode                      to use
   * @param nonConjugateLinkSegmentCosts to use
   * @return realised total cost
   */
  private double calculateRealisedCostForNetworkGap(Mode theMode, double[] nonConjugateLinkSegmentCosts) {
    double totalPhysicalRealisedCost = 0;
    double totalVirtualRealisedCost = 0;
    // costs as they currently are utilising the unconstrained demand as a point of reference
    var networkLayer = getTransportNetwork().getInfrastructureNetwork().getLayerByMode(theMode);
    for (var linkSegment : networkLayer.getLinkSegments()) {
      double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
      if(linkDemand <= 0.0){
        if(this.getLoading().getCurrentInflowsPcuH()[(int) linkSegment.getId()] > 0) {
          throw new PlanItRunTimeException("CANNOT HAPPEN");
        }
        continue;
      }
      double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
      totalPhysicalRealisedCost += linkCost * linkDemand;
      //LOGGER.warning(String.format("link [%s] - cost %.4f - demand %.1f - gapcost %.4f", linkSegment.getXmlId(), linkCost, linkDemand, linkCost*linkDemand));
    }
    var virtualLayer = getTransportNetwork().getVirtualNetwork().getLayer();
    for (var linkSegment : virtualLayer.getConnectoidSegments()) {
      double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
      if(linkDemand <= 0.0){
        if(this.getLoading().getCurrentInflowsPcuH()[(int) linkSegment.getId()] > 0) {
          throw new PlanItRunTimeException("CANNOT HAPPEN");
        }
        continue;
      }
      double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
      totalVirtualRealisedCost += linkCost * linkDemand;
    }
    double totalRealisedCostForGap = totalPhysicalRealisedCost + totalVirtualRealisedCost;
    return totalRealisedCostForGap;
  }

  /**
   * calculate min cost for network gap based on non-discontinuous costs....so normal realised network level costs.
   * In addition, we also compute the mincost of each O-D per bush, so we can construct a per bush gap as well to decide
   * what bushes will be considered for updating in the upcoming iteration.
   *
   * @param theMode                      to use
   * @param nonConjugateLinkSegmentCosts to use
   * @param considerTurnDiscontinuities  to use when considerTurnDiscontinuities is true, otherwise use link based costs
   * @return min total cost
   */
  private double calculateMinCostForNetworkAndBushGap(
      Mode theMode,
      double[] nonConjugateLinkSegmentCosts,
      double[] conjugateLinkSegmentCosts,
      boolean considerTurnDiscontinuities) {

    double totalMinCostForGap = 0;

    var conjLinkSegmentCostsToUse = conjugateLinkSegmentCosts;
    if(!considerTurnDiscontinuities) {
      conjLinkSegmentCostsToUse = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
          theMode, nonConjugateLinkSegmentCosts, false);
    }

    final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCostsToUse);
    for (var conjBush : getBushes()) {
      double scaledMinCostBush = 0;
      if (conjBush == null) {
        continue;
      }

      // network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush
      // where destination is root)
      var networkMinPaths = conjNetworkShortestPathAlgo.execute(
          conjBush.getShortestSearchType(), conjBush.getRootVertex());
      if (networkMinPaths == null) {
        LOGGER.severe("Unable to obtain network min paths for bush, this shouldn't happen, skip updateBushPass");
        continue;
      }

      // update/track total min cost across bushes(Ods) for gap calculation
      var odDemands = getOdDemands(theMode);
      var destination = conjBush.getDestination().getParent().getParentZone();
      for (var originVertex : conjBush.getOriginVertices()) {
        var origin = ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone();
        double odDemand = odDemands.getValue(origin, destination);
        if(odDemand <= 0.0){
          continue;
        }
        double minOdCost = networkMinPaths.getCostToReach(originVertex);
        double scaledMinCostBushOd = minOdCost * odDemand;
        totalMinCostForGap += scaledMinCostBushOd;
        scaledMinCostBush += scaledMinCostBushOd;
      }
      // we'll set the measured cost when traversing the bushes in search fo pass.
      conjBush.setMinCostForGap(scaledMinCostBush);
    }
    return totalMinCostForGap;
  }

  /**
   * Try to create a new PAS for the given bush and the provided diverge vertex. We do so using the bush min-max path
   * tree and the newly added segment.
   * First, we mark the newly added segment, and then all bush min path links back to the destination.
   * Second, we traverse the max path from the diverge vertex back to the destination.
   * Third, when the max path coincides with the min path, we have found our new PAS
   * <p>
   * TODO: we should revisit what is deemed sufficiently efficient cost/flow based for adding a PAS
   *
   * @param bush                          to identify new PAS for
   * @param reducedCostVertex             to use for creating the PAS as a cheaper path to the root exists at this vertex
   * @param startSegmentForS1Alternative  to use as the start segment of the S1 alternative
   * @param reducedCost                   to check if new PAS is considered effective
   * @param bushMinMaxPathResult          used for PAS construction
   * @param conjugateLinkSegmentCosts     to check if new PAS is considered effective
   * @return new created PAS if successfully created, null otherwise, the boolean indicates if it indeed is a brand
   * new PAS or for some reason we still reused an existing one
   */
  protected Pair<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>, Boolean> extendConjugateBushWithPas(
      final ConjugateDestinationBush bush,
      final ConjugateDirectedVertex reducedCostVertex,
      ConjugateEdgeSegment startSegmentForS1Alternative,
      double reducedCost,
      MinMaxPathResult bushMinMaxPathResult,
      double[] conjugateLinkSegmentCosts) {

    // TODO: we now only consider one of the two options: max bush path, but when adding a new link in
    //  the bush min path from the vertex is ALSO an alternative. Probably better to check both and then choose
    //  the shortest of the two!

    /* Label all vertices on max path from reference vertex to root as -1, and PAS reference vertex itself as 1 */
    final short[] conjAlternativeSegmentVertexLabels =
            new short[conjugateTransportModelNetwork.getNumberOfVerticesAllLayers()];
    conjAlternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    // choose max path because it is the most likely to have flow
    bushMinMaxPathResult.setMinPathState(false);
    int numShortestPathEdgeSegments = bushMinMaxPathResult.forEachNextEdgeSegment(
            bush.getRootVertex(),
            reducedCostVertex,
            (edgeSegment) -> conjAlternativeSegmentVertexLabels[(int)
                bushMinMaxPathResult.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    // construct s1 alternative now that we can find where it terminates when intersecting with Max path (-1)
    bushMinMaxPathResult.setMinPathState(true);
    var s1Alternative = new LinkedList<ConjugateEdgeSegment>();
    var currVertex = startSegmentForS1Alternative.getDownstreamVertex();
    s1Alternative.add(startSegmentForS1Alternative);
    ConjugateEdgeSegment nextS1Segment = null;
    do {
      nextS1Segment = (ConjugateEdgeSegment) bushMinMaxPathResult.getNextEdgeSegmentForVertex(currVertex);
      if(nextS1Segment == null){
        LOGGER.info(String.format("Unable to create new PAS (S1) for conjugate bush (%s), " +
            "despite reduced cost, should not happen", bush.getRootZone().getIdsAsString()));
        s1Alternative.clear();
        break;
      }
      s1Alternative.add(nextS1Segment);
      currVertex = nextS1Segment.getDownstreamVertex();
    }while(conjAlternativeSegmentVertexLabels[(int)currVertex.getId()] != -1);

    if (s1Alternative.isEmpty()) {
      return null;
    }

    // Identify S2 now that we know where it coincides with S1 alternative
    bushMinMaxPathResult.setMinPathState(false);
    var s2Alternative = new LinkedList<ConjugateEdgeSegment>();
    ConjugateEdgeSegment nextS2Segment;
    ConjugateDirectedVertex mergeVertex = currVertex;
    currVertex = reducedCostVertex;
    do {
      nextS2Segment = (ConjugateEdgeSegment) bushMinMaxPathResult.getNextEdgeSegmentForVertex(currVertex);
      if(nextS2Segment == null){
        LOGGER.info(String.format("Unable to create new PAS (S2) for conjugate bush (%s), " +
            "despite reduced cost, should not happen", bush.getRootZone().getIdsAsString()));
        s2Alternative.clear();
        break;
      }
      s2Alternative.add(nextS2Segment);
      currVertex = nextS2Segment.getDownstreamVertex();
    }while(!currVertex.idEquals(mergeVertex));

    if (s2Alternative.isEmpty()) {
      return null;
    }

    var s1 = s1Alternative.toArray(new ConjugateEdgeSegment[0]);
    var s2 = s2Alternative.toArray(new ConjugateEdgeSegment[0]);

//    //todo revisit this. IS it still needed? Or maybe bad to actually use it!
//    double highCostAlternativeCost = PasManager.computeCost(s2, conjugateLinkSegmentCosts);
//    double lowCostAlternativeCost = PasManager.computeCost(s1, conjugateLinkSegmentCosts);
//    if (!PasManager.isPasEffectiveForBush(
//            s2,
//            highCostAlternativeCost,
//            lowCostAlternativeCost,
//            bush,
//            getLoading().getCurrentFlowAcceptanceFactors(),
//            reducedCost)) {
//      return null;
//    }

    // find or create new PAS for this bush. If PAS exists for other bush, we reuse it.
    boolean isNewPas = false;
    var pas = pasManager.findMatchingActivePas(s1, s2);
    if (pas == null) {
      pas = pasManager.findMatchingInactivePas(s1, s2);
      if (pas == null) {
        pas = pasManager.createAndRegisterNewPas(bush, s1, s2);
        // prep for usage
        pas.updateCost(conjugateLinkSegmentCosts);
        updatePasStatusBeforeFlowShift(pas, getLoading().getCurrentFlowAcceptanceFactors());
        getLoading().activateNodeTrackingFor(pas);
        isNewPas = true;
      }else{
        // existing PAS, register bush on it and reactivate
        pas.registerBush(bush);
        pasManager.reactivatePas(pas);
      }
    }else{
      // existing activated PAS, register bush on it
      pas.registerBush(bush);
    }

    return Pair.of(pas, isNewPas);
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected TreeSet<ConjugateDestinationBush> createEmptyBushes(Mode mode) {

    var conjugateNetworkLayer =
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    Zoning zoning = getTransportNetwork().getZoning();
    TreeSet<ConjugateDestinationBush> conjugateBushes = new TreeSet<>();

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
          conjugateBushes.add(bush);
          break;
        }
      }
    }
    return conjugateBushes;
  }

  /**
   * {@inheritDoc}
   *
   */
  @Override
  protected boolean initialiseBush(
          ConjugateDestinationBush bush,
          Zoning zoning,
          OdDemands odDemands,
          ShortestPathGeneralised shortestTreeAlgorithm) {
    // prep
    final var destinationCentroidVertex = bush.getRootZoneVertex();
    final OdZone destination = (OdZone) destinationCentroidVertex.getParent().getParentZone();
    final var destinationConjugateReferenceVertex =
        centroid2ConjugateNodeMapping.get(destinationCentroidVertex);

    var shortestPathAlgorithm = (ShortestPathDijkstra) shortestTreeAlgorithm;
    ShortestPathResult allToOneResult = shortestPathAlgorithm.executeAllToOne(destinationConjugateReferenceVertex);

    // shortest path search + spanning tree creation
    allToOneResult.populateDirectedAcyclicSubGraphSpanningTree(bush.getDag());

    // demand to OD-shortest paths
    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {
        var originConjugateReferenceVertex =
            centroid2ConjugateNodeMapping.get(findOriginCentroidVertex(origin));

        /* add demand along conjugate bush's shortest path from destination back to origin */
        // todo: could be more efficient, if we'd only added the demands and then walk topologicially using the next
        // backlinks to add the demand
        bush.addOriginDemandPcuH(originConjugateReferenceVertex, currOdDemand);
        int numLinksInPath = allToOneResult.forEachNextEdgeSegment(destinationConjugateReferenceVertex, originConjugateReferenceVertex,
            es -> bush.addTurnSendingFlow((ConjugateEdgeSegment) es, currOdDemand));
        if(numLinksInPath == 0){
          LOGGER.warning(String.format("Origin (%s) has demand to Destination (%s), but no viable path could be created" +
                  ", reset demand to zero"
              , origin.getIdsAsString(), destination.getIdsAsString()));
          bush.removeOriginDemandPcuH(originConjugateReferenceVertex);
        }
      }
    }
    return !bush.getDag().isEmpty();
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

  private Set<ConjugateEdgeSegment> improveZeroFlowBushConnectivity(
      ConjugateDestinationBush conjBush,
      double[] conjLinkSegmentCosts){

    double magicFlowThresholdForPruningIfInconsequential = 0.01;         // 0.01 flow
    double magicSplittingRateFactorForPruningIfInconsequential = 0.01; // 1%

    Set<ConjugateEdgeSegment> addedSegments = new HashSet<>();

    // for truly optimal links
    var shortestTreeAlgorithm = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
    var conjRootVertex = centroid2ConjugateNodeMapping.get(conjBush.getRootZoneVertex());
    ShortestPathResult allToOneResult = shortestTreeAlgorithm.executeAllToOne(conjRootVertex);

    // for bush optimal links if truly optimal is not possible
    boolean excludeZeroFlowLinksFromMaxPaths = false;
    var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
        conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
    bushMinMaxTree.setMinPathState(true);

    ConjugateEdgeSegment[] allSegments = Stream.concat(
            conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
            conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst().getLinkSegments().stream()).
        toArray(ConjugateEdgeSegment[]::new);

    for(var edgeSegment :allSegments) {
      if (conjBush.getDag().containsEdgeSegment(edgeSegment)){

        if(conjBush.getSendingFlowPcuH(edgeSegment)>0 &&
            conjBush.getSendingFlowPcuH(edgeSegment) < magicFlowThresholdForPruningIfInconsequential) {
          double splittingRate = conjBush.getSplittingRate(edgeSegment);
          if (splittingRate < magicSplittingRateFactorForPruningIfInconsequential) {
            conjBush.bushData.removeTurnData(edgeSegment);
//            LOGGER.info(String.format("     [NEAR zero flow (%s)- truncate to zero flow on bush (%s)]",
//                edgeSegment.getIdsAsString(), conjBush.getRootZone().getIdsAsString()));
          }
        }

        // remove zero flow segments
        //  if dangling, make sure it is the on the least-cost network path
        //    if not allowed, make sure it is the on the least-cost bush path (could be that nothing changes)
        //    if not allowed, make sure it is the on the best possible max cost path (could be that nothing changes)
        if(conjBush.getSendingFlowPcuH(edgeSegment) <= 0.0) {
          var minCostSegment = allToOneResult.getNextEdgeSegmentForVertex(edgeSegment.getUpstreamVertex());
          if (edgeSegment != minCostSegment) {
//            LOGGER.info(String.format("     [Zero flow - Not on Min Path --> remove : (%s) from bush (%s)]",
//                edgeSegment.getIdsAsString(),
//                conjBush.getRootZone().getIdsAsString()));
            conjBush.remove(edgeSegment);

            if (!conjBush.hasRegisteredExitSegments(edgeSegment.getUpstreamVertex()) && minCostSegment!=null) {
              var result = isEligibleForAdding((ConjugateEdgeSegment) minCostSegment, conjLinkSegmentCosts, bushMinMaxTree);
              if(result.first()){
                conjBush.getDag().addEdgeSegment((ConjugateEdgeSegment) minCostSegment);
                addedSegments.add((ConjugateEdgeSegment) minCostSegment);
//                LOGGER.info(String.format("        [Upstream node disconnected  - inject NETWORK min Path segment (%s) on bush (%s)]",
//                    minCostSegment.getIdsAsString(),
//                    conjBush.getRootZone().getIdsAsString()));
              }else{
                var bushMinSegment =
                    (ConjugateEdgeSegment) bushMinMaxTree.getNextEdgeSegmentForVertex(edgeSegment.getUpstreamVertex());
                result = isEligibleForAdding(bushMinSegment, conjLinkSegmentCosts, bushMinMaxTree);
                if(result.first() || bushMinSegment == edgeSegment) {
                  conjBush.getDag().addEdgeSegment(bushMinSegment);
                  if(bushMinSegment != edgeSegment) {
                    addedSegments.add((ConjugateEdgeSegment) minCostSegment);
                  }
//                  LOGGER.info(String.format("        [Upstream node disconnected  - inject BUSH min Path segment (%s) on bush (%s) - network min cost segment rejected]",
//                      minCostSegment.getIdsAsString(),
//                      conjBush.getRootZone().getIdsAsString()));
                }else{
                  // find cheapest max path instead (could be the same we had before)
                  bushMinMaxTree.setMinPathState(false);
                  // find cheapest exit available
                  var cheapestMaxOption = edgeSegment;
                  //todo: I think this is wrong, we should still be using the min cost, only we should check all of them!
                  double cheapestMaxCost = bushMinMaxTree.getMaxCostToReach(edgeSegment.getUpstreamVertex());
                  boolean p2OnlyAllowed = true;
                  for(var alExit : edgeSegment.getUpstreamVertex().getExitEdgeSegments()){
                    if(alExit == edgeSegment){
                      continue;
                    }
                    result = isEligibleForAdding(alExit, conjLinkSegmentCosts, bushMinMaxTree, p2OnlyAllowed);
                    if(result.first() && result.second()<cheapestMaxCost){
                      cheapestMaxOption = alExit;
                      cheapestMaxCost = result.second();
                    }
                  }
//                  LOGGER.info(String.format("        [Upstream node now disconnected  - replaced with Path segment (%s) on bush (%s) - network and bush min cost segment rejected]",
//                      cheapestMaxOption.getIdsAsString(),
//                      conjBush.getRootZone().getIdsAsString()));
                  conjBush.getDag().addEdgeSegment(cheapestMaxOption);
                  if(cheapestMaxOption != edgeSegment) {
                    addedSegments.add(cheapestMaxOption);
                  }
                  bushMinMaxTree.setMinPathState(true);
                }
              }
            }
          }
        }
      }
    }
    return addedSegments;
  }

  /**
   * In case restricted update resulted in no adding of any new segments, apply relaxed approach where
   * we allow for adding P2 based (max cost) satisfying edge segments. if set to false run in restricted mode
   * where both P1&P2 must be satisfied (note that relaxed approach only holds for non-zero vertices)
   *
   * @param conjBush
   * @param conjLinkSegmentCosts
   * @return added segments and a boolean indicating if any P1 (low cost) segment was eligible
   */
  private Pair<Set<ConjugateEdgeSegment>,Boolean> improveNonZeroFlowBushConnectivity(
      ConjugateDestinationBush conjBush,
      double[] conjLinkSegmentCosts,
      boolean allowEligibilityBasedOnOnlyP2) {

    Set<ConjugateEdgeSegment> addedSegments = new HashSet<>();

    /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root
     * is destination) */
    boolean excludeZeroFlowLinksFromMaxPaths = false;
    var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
        conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
    if (bushMinMaxTree == null) {
      LOGGER.severe(String.format(
          "Unable to obtain conjugate min-max paths for bush, this shouldn't happen, skip updateBushPass"));
      return Pair.of(addedSegments, false);
    }

    // fix: ONLY update non-zero flow vertices by adding segments for PAS updates
    boolean anyP1Eligible = false;
    var bushVertexIter = conjBush.getTopologicalIterator();
    while (bushVertexIter.hasNext()) {
      ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();

      bushMinMaxTree.setMinPathState(true);
      for (var outgoingSegment : conjBushVertex.getExitEdgeSegments()) {
        if (!conjBush.contains(outgoingSegment)) {
          var result = isEligibleForAdding(
              outgoingSegment, conjLinkSegmentCosts, bushMinMaxTree, allowEligibilityBasedOnOnlyP2);
          anyP1Eligible = anyP1Eligible || result.third();
          if (!result.first() || outgoingSegment.isOriginalEdgeSegmentsUTurn()) {
            continue;
          }
          conjBush.getDag().addEdgeSegment(outgoingSegment);
          addedSegments.add(outgoingSegment);
//          LOGGER.info(String.format("ADDING NEW LINK (%s) TO BUSH (%s)",
//              outgoingSegment, conjBush.getRootZone().getIdsAsString()));
        }
      }
    }
    return Pair.of(addedSegments,anyP1Eligible);
  }

  private Set<ConjugateEdgeSegment> improveNonZeroFlowBushConnectivity(
      ConjugateDestinationBush conjBush, double[] conjLinkSegmentCosts) {

    //***********************************************************************************************
    // make sure (near) zero flow links are all on network shortest paths and remove otherwise
    var addedZeroFlowSegments = improveZeroFlowBushConnectivity(conjBush, conjLinkSegmentCosts);

    //***********************************************************************************************
    // add new link segments that potentially will improve bush convergence based on (P1&P2)
    boolean allowRelaxedAddingMode = false;
    var result =  improveNonZeroFlowBushConnectivity(conjBush, conjLinkSegmentCosts, allowRelaxedAddingMode);
    boolean anyP1Eligible = result.second();
    if(result.first().isEmpty() && anyP1Eligible){
      allowRelaxedAddingMode = true;
      result =  improveNonZeroFlowBushConnectivity(
          conjBush, conjLinkSegmentCosts, allowRelaxedAddingMode);
      LOGGER.info(String.format(
          "BUSH (%s) unable to add new segments - switched to relaxed P2 mode and found %s new segments",
          conjBush.getRootZone().getIdsAsString(), result.first()));
    }
    addedZeroFlowSegments.addAll(result.first());
    return addedZeroFlowSegments;
  }

  /**
   * Based on provided original network link segment costs see if we can update the existing collection of PASs
   *
   * @param mode                         to use
   * @param nonConjugateLinkSegmentCosts to use
   * @param updateGap                    flag
   * @param simulationData               to use
   * @param logAll                       flag
   * @return newly created PASs
   */
  @Override
  protected Map<Long,Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>
  updateBushPass(
      Mode mode,
      double[] nonConjugateLinkSegmentCosts,
      boolean updateGap,
      StaticLtmSimulationData simulationData,
      boolean logAll){


    // rationale, any gap multiplies cost with flow. Flow upper bound generally does not exceed 10k in PCU/h per link,
    // so any route will be less than that. Hence, any gap in terms of normalised cost to this flow should be considered
    // and will aid in bringing the network gap down.
    final double MIN_REDUCED_COST = getGapFunction().getStopCriterion().getEpsilon()/10000.0;

    Map<Long, Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> passToConsider = new TreeMap<>();

    // method overridden for conjugate implementation resulting in conjugate compatible shortest path search using
    // conjugate link segment costs. For maintainability/readability expansion to conjugate costs occurs within method for now...
    // here we do use discontinuity costs because when considering new PASs it must be taken into account
    final var conjLinkSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(mode, nonConjugateLinkSegmentCosts, true);

    double totalMinCostForGap = 0; // track during bush traversal to get min OD costs based on shortest paths
    double totalRealisedCostForGap = 0;
    if(updateGap) {
      boolean considerTurnDiscontinuities = true;
      totalRealisedCostForGap = calculateRealisedCostForNetworkGap(mode, nonConjugateLinkSegmentCosts);
      totalMinCostForGap = calculateMinCostForNetworkAndBushGap(
          mode, nonConjugateLinkSegmentCosts, conjLinkSegmentCosts, considerTurnDiscontinuities);

      // finalise gap part
      var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);
      // both costs have already been normalised to demand so use unity to transfer as is
      // ideally we'd use a link based gap but this is not ideal with the path based implementation we also support
      // for sLTM
      gapFunction.increaseMinimumPathCosts(totalMinCostForGap,1);
      gapFunction.increaseAbsolutePathGap(totalRealisedCostForGap, 1, totalMinCostForGap);
      if(getSettings().isDetailedLogging()){
        LOGGER.severe(String.format("Total Realised cost: (%.16f)", totalRealisedCostForGap));
      }
    }

    // ALTERNATIVE BUSH PRUNING BASED ON BUSH GAP CALC
    int countGapSkippedBushes = 0;
    Set<ConjugateDestinationBush> eligibleBushes = new TreeSet<>();
    int totalConvergedBushes = 0;
    int totalCycleLimitedBushes = 0;
    int totalRegularUnconvergedBushes = 0;
    int totalNonImprovingBushes = 0;
    double summedInternalBushGaps = 0;
    for (var conjBush : getBushes()) {
      if (conjBush == null) {
        continue;
      }

      // for each bush determine if we're converging, moving away from convergence or have converged
      // to determine its step size for smoothing
      final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
      double scaledMinCostBush = conjBush.updateBushRealisedGapInformation(
          conjugateTransportModelNetwork, conjNetworkShortestPathAlgo, getOdDemands(mode), conjLinkSegmentCosts);

      double bushUpperBoundGap =
          (conjBush.getRealisedCostForGap() - conjBush.getMinCostForGap())/conjBush.getMinCostForGap();
      double bushLowerBoundGap =
          (scaledMinCostBush - conjBush.getMinCostForGap())/conjBush.getMinCostForGap();

      double bushInternalGap = (conjBush.getRealisedCostForGap() - scaledMinCostBush)/scaledMinCostBush;
      summedInternalBushGaps += bushInternalGap;

      boolean bushReachedNetworkGapConvergence = bushUpperBoundGap <= getGapFunction().getStopCriterion().getEpsilon();
      boolean bushReachMaxConvergenceUnderCycleLimitation = bushLowerBoundGap>0 &&
          (bushUpperBoundGap-bushLowerBoundGap)/bushLowerBoundGap < Precision.EPSILON_3;

      if(bushReachedNetworkGapConvergence || bushReachMaxConvergenceUnderCycleLimitation) {
        if (bushReachedNetworkGapConvergence) {
          // make sure we keep tracking gap even if we're not updating and skipping the rest of the gap/step update
          conjBush.prevIterationInitialGap = bushUpperBoundGap;
          ++totalConvergedBushes;
          continue;
        } else {
          // cannot add certain shortest paths due to cycle detection, unable to fully converge (for now)
          eligibleBushes.add(conjBush);
          ++totalCycleLimitedBushes;
        }
      }else{
        eligibleBushes.add(conjBush);
        ++totalRegularUnconvergedBushes;
      }
      conjBush.prevIterationInitialGap = bushUpperBoundGap;

    }
    LOGGER.info(String.format("++++ Updating %d bushes (%.2f%%): %d cycleLimited - %d not improving - %d converged",
      eligibleBushes.size(), ((double)eligibleBushes.size()*100)/getBushes().size(),
        totalCycleLimitedBushes, totalNonImprovingBushes, totalConvergedBushes));
    prevNetworkRealisedCost = totalRealisedCostForGap;

    // todo: very ugly because we manually calculate gap here -- fix (also we use global tracked min network gap, ugly
    double costDiff = totalRealisedCostForGap - totalMinCostForGap;
    double gap = costDiff/ Math.abs(totalMinCostForGap);

    boolean createNewPass = true;
    boolean updateBushStructure = true ;
    if(updateBushStructure) {
      LOGGER.info(String.format("CURR NETWORK GAP (%.10f) IMPROVED OVER MIN NETWORK GAP SO FAR  (%.10f)", gap, minNetworkGapAsThreshold));
    }
    if(createNewPass) {
      pasManager.reset();
    }else{
      pasManager.getActivePass().values().stream().flatMap(Collection::stream).forEach(
          p -> passToConsider.put(p.pasId, p));
      return passToConsider;
    }


    // **********************************************************************************************
    // FIND PASs for eligible bushes
    for (var conjBush : eligibleBushes) {
      // make sure we use the latest flow optimal connections --> can only do this now because
      // we should only add/change zero flow connections when we KNOW they will by used in a PAS and will become
      // non-zero flow. Otherwise this leads to problems later on with cycle detection
      // todo: when we compute gap earlier it is not entirely right because we use the pre-updated spanning tree
      //  however, we cannot update it there because if we do not consider the bush the new added links won't be used
      //  leading to the problem mentioned, so we accept we're trailing an iteration with that...
      Set<ConjugateEdgeSegment> newlyAddedBushLinkSegments = new TreeSet<>();
      if(updateBushStructure) {
        newlyAddedBushLinkSegments =
            improveNonZeroFlowBushConnectivity(conjBush, conjLinkSegmentCosts);
      }

      // recalculate bush min/max tree as well since any changes from above will result in different min/max
      // paths
      // NOTE: we now EXCLUDE zero flow links from max paths, to avoid generating high cost paths that are not eligible
      //  for flow shifting. We can only do that because we already changed the bush earlier as for that we do require
      // max paths for zero flow links to avoid cycles in adding new links.
      boolean excludeZeroFlowLinksFromMaxPaths = true;
      var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
          conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());

      /* find (new) matching PASs - start with new PAS close to destination exploration first */
      var bushVertexIter = conjBush.getTopologicalIterator();
      BREAK_BUSH:
      while(bushVertexIter.hasNext()) {
        ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();

        if(!conjBush.containsSendingFlow(conjBushVertex)) {
          continue;
        }

        // Regular approach for used portion of bush
        bushMinMaxTree.setMinPathState(true);
        var minNextEdge = (ConjugateEdgeSegment) bushMinMaxTree.getNextEdgeSegmentForVertex(conjBushVertex);
        bushMinMaxTree.setMinPathState(false);
        var maxNextEdge = (ConjugateEdgeSegment) bushMinMaxTree.getNextEdgeSegmentForVertex(conjBushVertex);
        double entryAcceptanceFactor = conjBushVertex.hasOriginalEdgeSegment() ?
            getLoading().getCurrentFlowAcceptanceFactors()[(int)conjBushVertex.getOriginalEdgeSegment().getId()] : 1;
        for(var outgoingSegment : conjBushVertex.getExitEdgeSegments()){

          //adding of segments done while rejigging spanning tree already

          boolean minPathInitialLinkNewToBush = newlyAddedBushLinkSegments.contains(outgoingSegment);
          boolean divergentMinMaxPaths = !(minNextEdge == maxNextEdge);
          boolean preferredOutGoingSegment = (minNextEdge == outgoingSegment);
          boolean congestedEntry = (entryAcceptanceFactor + Precision.EPSILON_6) < 1;

          // for existing segments we require a potential reduced cost (checked later) AND a split at the vertex
          // for the min and max path and the current outgoing segment is the preferred (min cost) segment
          if(!divergentMinMaxPaths || !preferredOutGoingSegment){
            continue;
          }

          double reducedCost =
              bushMinMaxTree.getMaxCostToReach(conjBushVertex) - bushMinMaxTree.getMinCostToReach(conjBushVertex);

          // find PAS using either min or max cost bush paths

          /* find a (new) PAS for the bush */
          var bushPasExtensionResult = extendConjugateBushWithPas(
              conjBush,
              conjBushVertex,
              outgoingSegment,
              reducedCost,
              bushMinMaxTree,
              conjLinkSegmentCosts);
          if (bushPasExtensionResult == null || bushPasExtensionResult.first() == null) {
            // ending up not adding the PAS, so remove just added segment again
            if(minPathInitialLinkNewToBush) {
              conjBush.remove(outgoingSegment);
            }
            continue;
          }
          var pasToAdd = bushPasExtensionResult.first();
          passToConsider.put(pasToAdd.pasId, pasToAdd);
        }
      }
    }

    LOGGER.info(String.format(
        "EXCLUDED %.2f%% = %d bushes from PAS updates because they are below current network gap",
        ((double)countGapSkippedBushes*100)/getBushes().size(), countGapSkippedBushes));

    LOGGER.info(String.format("^^^^^^^^^^^^^^^^^^ %.10f <----- BUSH SUMMED INTERNAL ITERATION GAP ",
        summedInternalBushGaps));
    return passToConsider;
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
   * {@inheritDoc}
   */
  @Override
  public StaticLtmLoadingBushConjugate getLoading() {
    return (StaticLtmLoadingBushConjugate) super.getLoading();
  }

  public MultiKeyMap<Object, ConjugateEdgeSegment> getTurn2ConjugateSegmentMapping() {
    return turn2ConjugateSegmentMapping;
  }

  /**
   * Given non conjugate costs for link segments, expand to concjugate segments (turns)
   * TODO: when everything is conjugate, avoid calling this multiple times as we do now as it is costly
   *   at that point process flow can just use conjugate costs rather than non-conjugate costs.
   *
   * @param theMode to use
   * @param nonConjugateLinkSegmentCosts original costs
   * @param considerDiscontinuities when true update turn costs in case of discontinuity for zero flow turn,
   *                                false do not
   * @return conjugate projected costs
   */
  public double[] expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
      Mode theMode, double[] nonConjugateLinkSegmentCosts, boolean considerDiscontinuities){
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
    if(considerDiscontinuities) {
      updateZeroFlowDiscontinuityCongestedTurnCosts(theMode, conjugateSegmentCosts);
    }

    return conjugateSegmentCosts;
  }

  public void removeAllButProtectedZeroFlowBushSegments(double[] conjLinkSegmentCosts){

    //todo: placeholder in case we need it
//    ConjugateEdgeSegment[] allSegments = Stream.concat(
//            conjugateTransportModelNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().stream(),
//            conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst().getLinkSegments().stream()).
//        toArray(ConjugateEdgeSegment[]::new);
//
//    for (var conjBush : getBushes()) {
//      boolean excludeZeroFlowLinksFromMaxPaths = true;
//      var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
//          conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
//
//      for(var edgeSegment :allSegments){
//        if(conjBush.getSendingFlowPcuH(edgeSegment) <= 0.0 && conjBush.getDag().containsEdgeSegment(edgeSegment)){
//          boolean zeroFlowVertex = conjBush.getSendingFlowPcuH(edgeSegment.getUpstreamVertex()) <= 0;
//          if(!zeroFlowVertex){
//            conjBush.remove(edgeSegment); // zero flow turn on non-zero flow vertex -> remove not dangling
//          }else{
//            var cheapestPathSegment = bushMinMaxTree.getNextEdgeSegmentForVertex(edgeSegment.getDownstreamVertex());
//            if(cheapestPathSegment != edgeSegment){
//              conjBush.remove(edgeSegment); // zero flow turn on zero flow vertex -> remove if not on cheapest path
//            }
//            if(!conjBush.contains(cheapestPathSegment.getId())){
//              conjBush.getDag().addEdgeSegment();
//            }
//          }
//
//          if(allowDanglingNodes){
//            localRemoved = true;
//            remove(edgeSegment);
//          }else{
//            localRemoved = removeUnlessNodeDangling(edgeSegment);
//          }
//
//          if(localRemoved && logRemoved){
//            LOGGER.info(String.format("     [No flow --> remove : (%s) from bush (%s)]",
//                edgeSegment.getIdsAsString(),
//                getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
//          }
//          anyRemoved = anyRemoved || localRemoved;
//        }
//      }
//      return anyRemoved;
//
//      conjBush.removeZeroFlowSegmentsIn(allSegments, false, true);
//    }
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
