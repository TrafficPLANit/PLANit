package org.goplanit.assignment.ltm.sltm.conjugate;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.algorithms.shortest.*;
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
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
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

import java.util.*;
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
  protected ShortestPathGeneralised createNetworkShortestSearchTreeAlgo(
      Mode theMode, double[] nonConjugateLinkSegmentCosts) {
    //todo: once base implementation works, replace nonConjugateLinkSegment costs with turn based costs throughout
    // implementation. For now project non conjugate link segment costs to conjugate segments by using the entry segment
    // as the point of reference
    double[] conjugateSegmentCosts =
            expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, nonConjugateLinkSegmentCosts);
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
  protected Pair<ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>, ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>>
  attemptUncongestedFlowShift(
      Mode theMode,
      Collection<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>> sortedPass,
      Map<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>, PasFlowShiftExecutor<ConjugateDirectedVertex,ConjugateEdgeSegment>> pasExecutors,
      double[] originalNetworkCosts) {

    var flowShiftedPass = new ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>(
        (int) this.pasManager.getNumberOfActivePass());
    var passWithoutBush = new ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>();

    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, originalNetworkCosts);

    for(var pas : sortedPass) {
      var executor = ((PasFlowShiftConjugateDestinationBasedExecutor) pasExecutors.get(pas));
      boolean pasFlowShifted = executor.executeUncongestedPasEquilibration(
        theMode,
        getLoading(),
        getGapFunction(),
        getPhysicalCost(),
        getVirtualCost(),
        originalNetworkCosts,
        conjSegmentCosts,
        false);

      if (pasFlowShifted) {
        flowShiftedPass.add(pas);

        if (!pas.hasRegisteredBushes()) {
          passWithoutBush.add(pas);
        }

      }
    }
    return Pair.of(flowShiftedPass, passWithoutBush);
  }

  @Override
  protected void hookBeforeCongestedPasUpdate(
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
    LongAdder countSwappedPassPrev = new LongAdder();
    pasManager.forEachActivePas( p -> countSwappedPassPrev.add(p.getCountS1Swaps().second().longValue()));

    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, originalNetworkLinkSegmentCosts);

    // execute cost update based on conjugate costs
    pasManager.updateActivePassCosts(conjSegmentCosts);
    pasManager.updateInactivePassCosts(conjSegmentCosts);

    LongAdder countSwappedPassCurr = new LongAdder();
    pasManager.forEachActivePas( p -> countSwappedPassCurr.add(p.getCountS1Swaps().second().longValue()));
    long numSwaps = countSwappedPassCurr.longValue() - countSwappedPassPrev.longValue();
    if(pasManager.getNumberOfActivePass()>0) {
      double percentageSwapped = (numSwaps * 100) / (double) pasManager.getNumberOfActivePass();
      LOGGER.info(String.format("%.2f%% (%d/%d) of Active PASs  swapped which alternative was cheapest",
              percentageSwapped, numSwaps, pasManager.getNumberOfActivePass()));
    }

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

    // TODO: currently we do not actuall check if it is a zero-flow turn, we should because due to slight
    //  inconsistencies in the iterative loading procedure this sometimes gets triggered for turns with non-zero flow
    //  which is not great.

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
        assert(originalNlOutflow >= outflowConsistentWithNonZeroTurnFlow);

        //3. overwrite existing costs for turns where discontinuity was found
        var conjugateSegment = turn2ConjugateSegmentMapping.get(entry, exit);
        assert (conjSegmentCosts[(int)conjugateSegment.getId()] <= disContinuitySegmentCost);

        // in case cost has not changed (can happen in change in drop in alpha is offset in increased inflow due to
        // slight discrepancy when ending iterative loading procedure, then ignore update, otherwise, true
        // discontinuity found and update
        if(conjSegmentCosts[(int)conjugateSegment.getId()] < disContinuitySegmentCost) {
          conjSegmentCosts[(int) conjugateSegment.getId()] = disContinuitySegmentCost;
          numDiscontinuitiesUpdated.increment();
        }
      };
    //

    //2. for each congested node rerun node in turn based form
    Predicate<DirectedVertex> hasCongestedEntrySegment = n -> IterableUtils.asStream(
        n.getEntryEdgeSegments()).anyMatch(es -> Precision.smaller(
            flowAcceptanceFactors[(int)es.getId()], 1, Precision.EPSILON_6));
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

    if(getSettings().isDetailedLogging() && numDiscontinuitiesUpdated.intValue()>0) {
      LOGGER.info(String.format("Updated costs for %d zero-flow turns with a discontinuous cost function",
          numDiscontinuitiesUpdated.intValue()));
    }
  }

  /**
   * Check if segment is worth adding based on whether it is both in the min and in the max path search.
   * Consistent with intersection of P1 and P2 sets in Nie (2009) - A class of bush-based algorithms for the traffic
   * assignment problem.
   *
   * @param linkSegment          to check
   * @param conjLinkSegmentCosts segment costs to use
   * @param conjBushMinMaxPaths  min max path cost to check
   * @return true when eligible, false otherwise
   */
  private boolean isEligibleForAdding(
      ConjugateEdgeSegment linkSegment, double[] conjLinkSegmentCosts, MinMaxPathResult conjBushMinMaxPaths) {
    var endVertex = linkSegment.getUpstreamVertex();
    var startVertex = linkSegment.getDownstreamVertex();

    double startToEndCost = conjLinkSegmentCosts[(int)linkSegment.getId()];

    double minCostEnd = conjBushMinMaxPaths.getMinCostToReach(endVertex);
    double minCostStart = conjBushMinMaxPaths.getMinCostToReach(startVertex);
    if(minCostStart + startToEndCost < minCostEnd){
      double maxCostEnd = conjBushMinMaxPaths.getMaxCostToReach(endVertex);
      double maxCostStart = conjBushMinMaxPaths.getMaxCostToReach(startVertex);
      if(maxCostStart + startToEndCost < maxCostEnd){
        return true;
      }
    }
    return false;
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
   * @param bannedS1Vertices              vertices that are not allowed to be used for any new S1 alternative
   * @param allowUncongestedOnly          when true we only consider adding PASs that are NOT congested
   * @return new created PAS if successfully created, null otherwise, the boolean indicates if it indeed is a brand
   * new PAS or for some reason we still reused an existing one
   */
  protected Pair<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>, Boolean> extendConjugateBushWithPas(
      final ConjugateDestinationBush bush,
      final ConjugateDirectedVertex reducedCostVertex,
      ConjugateEdgeSegment startSegmentForS1Alternative,
      double reducedCost,
      MinMaxPathResult bushMinMaxPathResult,
      double[] conjugateLinkSegmentCosts,
      Set<ConjugateDirectedVertex> bannedS1Vertices,
      boolean allowUncongestedOnly) {

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
    if(!bannedS1Vertices.contains(currVertex)){
      s1Alternative.add(startSegmentForS1Alternative);
      ConjugateEdgeSegment nextS1Segment = null;
      do {
        nextS1Segment = (ConjugateEdgeSegment) bushMinMaxPathResult.getNextEdgeSegmentForVertex(currVertex);
        if(nextS1Segment == null){
          LOGGER.info(String.format("Unable to create new PAS (S1) for conjugate bush rooted at vertex (%s), " +
              "despite reduced cost, should not happen"));
          s1Alternative.clear();
          break;
        }else if(bannedS1Vertices.contains(nextS1Segment.getDownstreamVertex())){
          // touching banned vertex, not allowed to create s1 alternative
          s1Alternative.clear();
          break;
        }
        s1Alternative.add(nextS1Segment);
        currVertex = nextS1Segment.getDownstreamVertex();
      }while(conjAlternativeSegmentVertexLabels[(int)currVertex.getId()] != -1);
    }

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

    //todo revisit this. IS it still needed? Or maybe bad to actually use it!
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

    //todo inefficient, if we were able to determine status outside of PAS (should be easy) like cost, we can avoid
    // first creating PAS and then deactivating it etc.
    if(allowUncongestedOnly && pas.getStatus()== PasStatus.CONGESTED){
      // not allowed, because we only consider uncongested PASs at this point, deregister bush, and reset
      pas.removeBush(bush);
      if(!pas.hasRegisteredBushes()){
        pasManager.deactivatePas(pas, false);
      }
      isNewPas = false;
      pas = null;
    }

    // IMPORTANT FOR EFFICIENT MULTI_PASS adding
    if(pas != null){

    }

    return Pair.of(pas, isNewPas);
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected Set<ConjugateDestinationBush> createEmptyBushes(Mode mode) {

    var conjugateNetworkLayer =
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    Zoning zoning = getTransportNetwork().getZoning();
    Set<ConjugateDestinationBush> conjugateBushes = new TreeSet<>();

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

    // shortest path search + spanning tree creation
    var shortestPAthAlgorithm = (ShortestPathDijkstra) shortestTreeAlgorithm;
    ShortestPathResult allToOneResult = shortestPAthAlgorithm.executeAllToOne(destinationConjugateReferenceVertex);
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
        allToOneResult.forEachNextEdgeSegment(destinationConjugateReferenceVertex, originConjugateReferenceVertex,
            es -> bush.addTurnSendingFlow((ConjugateEdgeSegment) es, currOdDemand));
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

    final int MAX_CONGESTED_PAS_ADD_PER_BUSH = 1;
    final int MAX_PAS_ADD_PER_BUSH = 5;

    double totalMinCostForGap = 0; // track during bush traversal to get min OD costs based on shortest paths
    double totalRealisedCostForGap = 0;
    if(updateGap) {
      // costs as they currently are utilising the unconstrained demand as a point of reference
      var networkLayer = getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode);
      for (var linkSegment : networkLayer.getLinkSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
      var virtualLayer = getTransportNetwork().getVirtualNetwork().getLayer();
      for (var linkSegment : virtualLayer.getConnectoidSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
    }

    //todo --> should be sets
    List<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> addedPass = new ArrayList<>();

    // method overridden for conjugate implementation resulting in conjugate compatible shortest path search using
    // conjugate link segment costs. For maintainability/readability expansion to conjugate costs occurs within method for now...
    final var conjLinkSegmentCosts =
            expandNonConjugateLinkSegmentCostToConjugateSegmentCost(mode, nonConjugateLinkSegmentCosts);
    // TODO: MIN NETWORK SEARCH ONLY USED FOR GAP CALCULATION CURRENTLY --> PHASE OUT WHEN SWAPPING TO MIN-MAX SHORTEST PATHS GAP
    final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
    for (var conjBush : getBushes()) {
      if (conjBush == null) {
        continue;
      }
      // track vertices that have been added due to PAS s1 alternative
      // any new PAS that touches these vertices will not be added because they overlap
      // overlapping PASs are less effective than non-overlapping ones. Wait until next iteration
      // for when they have flow / top. ordering and bush/min/max is updated. Then try again.
      Set<ConjugateDirectedVertex> addedBushPasS1TouchedVertices = new TreeSet<>();

      /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root
       * is destination) */
      var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(
              conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
      if (bushMinMaxTree == null) {
        LOGGER.severe(String.format(
                "Unable to obtain conjugate min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }
      bushMinMaxTree.setMinPathState(false);

      /* network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush
       * where destination is root) */
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

      /* find (new) matching PASs - start with new PAS close to destination exploration first */
      int countPassAddedForBush = 0;
      int countCongestedPassAddedForBush = 0;
      var bushVertexIter = conjBush.getTopologicalIterator();
      BREAK_BUSH:
      while(bushVertexIter.hasNext()) {
        ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();
        for(var outgoingSegment : conjBushVertex.getExitEdgeSegments()){
          if(conjBush.contains(outgoingSegment) ||
              !isEligibleForAdding(outgoingSegment, conjLinkSegmentCosts, bushMinMaxTree)){
            continue;
          }

          // we want to only add eligible links which have flow leading into their upstream vertex, otherwise there is
          // no flow to divert.
          if(!conjBush.containsSendingFlow(conjBushVertex)){
            continue;
          }


          // found segment to add -- necessitates creation of a new PAS because we are merging two possible routes
          conjBush.getDag().addEdgeSegment(outgoingSegment);
          double minCostToVertexWithNewLink = conjLinkSegmentCosts[(int)outgoingSegment.getId()] + bushMinMaxTree.getMinCostToReach(outgoingSegment.getDownstreamVertex());
          double minReducedCost =
              bushMinMaxTree.getMinCostToReach(conjBushVertex) - minCostToVertexWithNewLink;
          double maxReducedCost =
              bushMinMaxTree.getMaxCostToReach(conjBushVertex) - minCostToVertexWithNewLink;

          // find PAS using either min or max cost bush paths

          /* find a (new) PAS for the bush */
          boolean allowUncongestedOnly = MAX_CONGESTED_PAS_ADD_PER_BUSH == countCongestedPassAddedForBush;
          var bushPasExtensionResult = extendConjugateBushWithPas(
              conjBush,
              conjBushVertex,
              outgoingSegment,
              maxReducedCost,
              bushMinMaxTree,
              conjLinkSegmentCosts,
              addedBushPasS1TouchedVertices,
              allowUncongestedOnly);
          if (bushPasExtensionResult == null || bushPasExtensionResult.first() == null) {
            // ending up not adding the PAS, so remove just added segment again
            conjBush.remove(outgoingSegment);
            continue;
          }
          var pasToAdd = bushPasExtensionResult.first();

          // truly new PAS
          addedPass.add(pasToAdd);
          if(isDestinationTrackedForLogging(conjBush) || logAll){
            LOGGER.info(String.format("Registered new PAS (%s) on conjugate bush (%s)",
                pasToAdd, conjBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }

          ++countPassAddedForBush;
          if(pasToAdd.getStatus() == PasStatus.CONGESTED) {
            ++countCongestedPassAddedForBush;
          }
          if(countCongestedPassAddedForBush == MAX_CONGESTED_PAS_ADD_PER_BUSH ||
                  countPassAddedForBush - countCongestedPassAddedForBush >= MAX_PAS_ADD_PER_BUSH){
            break BREAK_BUSH;
          }

          // update added PAS vertices - to ban for subsequent new PASs
          var lowCostAlt = pasToAdd.getAlternative(true);
          for(int index = 0; index < lowCostAlt.length-1; ++index){
            addedBushPasS1TouchedVertices.add(lowCostAlt[index].getDownstreamVertex());
          }
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

    return Pair.of(addedPass,new ArrayList<>(0));
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
   * Given non conjugate costs for link segments, expand to concjugate segments (turns)
   * TODO: when everything is conjugate, avoid calling this multiple times as we do now as it is costly
   *   at that point process flow can just use conjugate costs rather than non-conjugate costs.
   *
   * @param theMode to use
   * @param nonConjugateLinkSegmentCosts original costs
   * @return conjugate projected costs
   */
  public double[] expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
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
