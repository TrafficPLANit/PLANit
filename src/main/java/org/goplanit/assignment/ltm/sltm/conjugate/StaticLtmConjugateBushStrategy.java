package org.goplanit.assignment.ltm.sltm.conjugate;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelUtils;
import org.goplanit.algorithms.shortest.*;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.consumer.DiscontinuityTurnCostReplacementConsumer;
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
  protected Pair<ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>, ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>>
  doUncongestedFlowShiftingV1(
      Mode theMode,
      Collection<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>> sortedPass,
      Map<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>, PasFlowShiftExecutor<ConjugateDirectedVertex,ConjugateEdgeSegment>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      StaticLtmSimulationData simulationData) {

    boolean smoothOverIterations = true;

    var flowShiftedPass = new ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>(
        (int) this.pasManager.getNumberOfActivePass());
    var passWithoutBush = new ArrayList<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>>();

    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, originalNetworkCosts, true);

    // for uncongested, do each PAS (one or more times), then repeat x times so PAS interaction is
    // covered better by having an internal loop here
    int MAX_ITERATIONS_ALLOWED = 2;
    int iteration = 1;
    boolean doNotStop = true;
    do {

      var updatedOrder = flowShiftingStepFourOrderPassInDescendingOrder(pasExecutors);
      sortedPass = updatedOrder;

//      int MAX_PAS_UPDATES = Math.max(5,sortedPass.size()/10); // top 10% with minimum of 5 PASs
      int uncongestedPasCounter = 0;

      long numUncongestedPass = sortedPass.stream().filter(
          p -> p.getStatus()==PasStatus.UNCONGESTED_WITHOUT_SHIFT ||
              p.getStatus()==PasStatus.UNCONGESTED_WITH_SHIFT).count();
      double perPasPercentageOfTotal = 1.0/numUncongestedPass;

      boolean logAll = false; //simulationData.getIterationIndex()>=50 && getSettings().isDetailedLogging();
      LOGGER.info(String.format("--- NEXT UNCONGESTED PASs INTERNAL ITERATION %d ----", iteration));
      for (var pas : sortedPass) {
        var executor = ((PasFlowShiftConjugateDestinationBasedExecutor) pasExecutors.get(pas));
        double importanceSmoothingFactor = Math.pow(0.01, (uncongestedPasCounter*perPasPercentageOfTotal)); // run from 100% exponential decay to 1% of leat important PAS

        if (pas.pasId == 3941L) {
          int bla = 4; // uncongested
        }

        var pasFlowShiftedByRefTurn = executor.performEquilibratedUncongestedFlowShifts(
            theMode,
            this,
            nlConsistentFlowAcceptanceFactors,
            originalNetworkCosts,
            conjSegmentCosts,
            getBushes(),
            logAll,
            PasFlowShiftConjugateDestinationBasedExecutor.FlowShiftSmoothingApproach.NORMAL,
            smoothOverIterations ? (1.0/MAX_ITERATIONS_ALLOWED) * importanceSmoothingFactor : importanceSmoothingFactor);

        double pasFlowShifted = Math.abs(pasFlowShiftedByRefTurn.second());
        if (!pas.hasRegisteredBushes()) {
          passWithoutBush.add(pas);
          if(pasFlowShifted > 0){
            ++uncongestedPasCounter;
            flowShiftedPass.add(pas);
            logAll = false;
          }
          continue;
        }

        if(pasFlowShifted <= 0){
          continue;
        }else {
          ++uncongestedPasCounter;
          if (iteration==1) {
            flowShiftedPass.add(pas);
            logAll = false;
          }
        }
      }

    }while(iteration++ < MAX_ITERATIONS_ALLOWED);
    return Pair.of(flowShiftedPass, passWithoutBush);
  }

  @Override
  protected Map<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>,Pair<EdgeSegment,Double>> determineConvergenceBasedUncongestedFlowShiftsV2(
      Mode theMode,
      Collection<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>> sortedPass,
      Map<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>, PasFlowShiftExecutor<ConjugateDirectedVertex,ConjugateEdgeSegment>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      StaticLtmSimulationData simulationData) {

    var pasDesiredFlowShifts = new TreeMap<Pas<ConjugateDirectedVertex,ConjugateEdgeSegment>,Pair<EdgeSegment,Double>>();

    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts =
        expandNonConjugateLinkSegmentCostToConjugateSegmentCost(theMode, originalNetworkCosts, true);

    var updatedOrder = flowShiftingStepFourOrderPassInDescendingOrder(pasExecutors);
    sortedPass = updatedOrder;

    boolean logAll = false; //simulationData.getIterationIndex()>=50 && getSettings().isDetailedLogging();
    LOGGER.info("--- V2 UNCONGESTED PASs INTERNAL ITERATION ----");
    for (var pas : sortedPass) {
      var executor = ((PasFlowShiftConjugateDestinationBasedExecutor) pasExecutors.get(pas));

      if (pas.pasId == 3941L) {
        int bla = 4; // uncongested
      }

      var pasFlowShiftedByRefTurn = executor.performEquilibratedUncongestedFlowShifts(
          theMode,
          this,
          nlConsistentFlowAcceptanceFactors,
          originalNetworkCosts,
          conjSegmentCosts,
          getBushes(),
          logAll,
          PasFlowShiftConjugateDestinationBasedExecutor.FlowShiftSmoothingApproach.RESET,
          1 /*not relevant*/);
      pasDesiredFlowShifts.put(pas, pasFlowShiftedByRefTurn);
    }
    return pasDesiredFlowShifts;
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
    LongAdder countSwappedPassPrev = new LongAdder();
    pasManager.forEachActivePas( p -> countSwappedPassPrev.add(p.getCountS1Swaps().second().longValue()));

    // PASs on conjugate level, so expand link segment to conjugate segment costs as if first
    var conjSegmentCosts = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
        theMode, originalNetworkLinkSegmentCosts, true);

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

  // given the desired shifts, perform network loading not based on full bushes, but only on the individual PAS level.
  // objective is to update costs afterwards to inform next inner iteration of route choice considering PAS interactions
  @Override
  protected void performLocalisedPasNetworkLoading(
      Mode theMode,
      Map<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts,
      Map<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>, PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment>> pasExecutors,
      double[] originalNetworkCosts,
      Set<ConjugateDestinationBush> bushes,
      boolean logAll) {

    // Step 1: splitting rate update --> create custom PAS based consumer equivalent (opposed to regular bush based version)
    //getLoading().stepOneSplittingRatesUpdate(mode); <-- PAS equivalent of this
    for (var pasEntry : pasDesiredFlowShifts.entrySet()) {
      double flowShift = pasEntry.getValue().second();
      if(flowShift != 0) {
        var executor = (PasFlowShiftConjugateDestinationBasedExecutor)pasExecutors.get(pasEntry.getKey());
        executor.performOneShotFlowShift(
            theMode,
            this,
            (ConjugateEdgeSegment) pasEntry.getValue().first(),
            flowShift,
            bushes,
            logAll);
      }
    }
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
          LOGGER.info(String.format("Unable to create new PAS (S1) for conjugate bush (%s), " +
              "despite reduced cost, should not happen", bush.getRootZone().getIdsAsString()));
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

  private void optimiseZeroFlowSpanningTreeConnections(
      ConjugateDestinationBush conjBush, double[] conjLinkSegmentCosts) {
    /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root
     * is destination) */
    boolean excludeZeroFlowLinksFromMaxPaths = false;
    var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
        conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
    if (bushMinMaxTree == null) {
      LOGGER.severe(String.format(
          "Unable to obtain conjugate min-max paths for bush, this shouldn't happen, skip updateBushPass"));
      return;
    }

    // fix: rejig entire bush regarding adding cheaper links currently not in the bush.
    //      we consider both flow and non-flow carrying links in this situation and separate this out
    //      from PAS creation.
    // todo: move into its own method separate from bushPAS creation
    Map<ConjugateEdgeSegment, ConjugateEdgeSegment> replaceZeroFlowSpanningTreeSegments = new TreeMap<>();
    var bushVertexIter = conjBush.getTopologicalIterator();
    while (bushVertexIter.hasNext()) {
      ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();
      boolean zeroFlowVertex = !conjBush.containsSendingFlow(conjBushVertex);

      bushMinMaxTree.setMinPathState(true);
      var existingOutgoingSegment = (ConjugateEdgeSegment) bushMinMaxTree.getNextEdgeSegmentForVertex(conjBushVertex);
      ConjugateEdgeSegment cheapestAltOutgoingSegment = null;
      double cheapestAltOutgoingSegmentCost = Double.MAX_VALUE;
      int countExisting = 0;
      for (var outgoingSegment : conjBushVertex.getExitEdgeSegments()) {
        if (!conjBush.contains(outgoingSegment)) {
          var result = ConjugateBushUtils.isEligibleForAdding(outgoingSegment, conjLinkSegmentCosts, bushMinMaxTree);
          if (!result.first()) {
            continue;
          }

          // find cheapest of the new alternatives that is deemed eligible
          if (result.second() < cheapestAltOutgoingSegmentCost) {
            cheapestAltOutgoingSegment = outgoingSegment;
            cheapestAltOutgoingSegmentCost = result.second();
          }
        } else {
          ++countExisting;
        }
      }
      if (cheapestAltOutgoingSegment != null) {
        // mark for replacing original zero flow cheapest bush segment with newly found
        // this requires removing something from bush, so cache until we no longer traverse the bush
        if (zeroFlowVertex) {
          replaceZeroFlowSpanningTreeSegments.put(existingOutgoingSegment, cheapestAltOutgoingSegment);
        } else {
          // simply add the newly found cheapest alternative link segment directly as it will be picked up for a
          // PAS later on (make sure we only add if we are sure we are generating PASs for this bush otherwise
          // we are dding links that do not get used which is BAD since it would trigger incorrect cycle detection
          // as in it would think a cycle exists when it in fact does not (for used flow)
          conjBush.getDag().addEdgeSegment(cheapestAltOutgoingSegment);
        }
      }
    }
    if (!replaceZeroFlowSpanningTreeSegments.isEmpty()) {
      for (var entry : replaceZeroFlowSpanningTreeSegments.entrySet()) {
        conjBush.remove(entry.getKey()); // remove old
        var newSegment = entry.getValue();
        conjBush.getDag().addEdgeSegment(newSegment); // add new
        // overwrite in min max tree to ensure we do not offer a segment that is not on the bush
        // (costs are not correct but that will self-correct next iteration)
        var upstreamVertex = newSegment.getUpstreamVertex();
        bushMinMaxTree.overwriteNextSegmentForVertex(upstreamVertex, newSegment);
        bushMinMaxTree.setMinPathState(false);
        bushMinMaxTree.overwriteNextSegmentForVertex(upstreamVertex, newSegment);
      }
    }
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
  protected Map<Long,Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>
  updateBushPass(Mode mode, double[] nonConjugateLinkSegmentCosts, boolean updateGap, boolean logAll){
    // rationale, any gap multiplies cost with flow. Flow upper bound generally does not exceed 10k in PCU/h per link,
    // so any route will be less than that. Hence, any gap in terms of normalised cost to this flow should be considered
    // and will aid in bringing the network gap down.
    final double MIN_REDUCED_COST = getGapFunction().getStopCriterion().getEpsilon()/10000.0;

    final int MAX_CONGESTED_PAS_ADD_PER_BUSH = Integer.MAX_VALUE;

    final int MAX_PAS_ADD_PER_BUSH = Integer.MAX_VALUE;
    pasManager.reset();

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

    // BUSH SELECTION - FIRST ACTIVE BUSH UNLESS CONVERGED --> NEXT
//    ConjugateDestinationBush theActiveBush = getBushes().stream().dropWhile(b -> !b.currentActiveBush).findFirst().orElse(
//        getBushes().stream().findFirst().get());
//    theActiveBush.currentActiveBush = true;
//
//    final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
//    var networkMinPaths = conjNetworkShortestPathAlgo.execute(
//        theActiveBush.getShortestSearchType(), theActiveBush.getRootVertex());
//
//    double scaledMinCostBush = theActiveBush.updateBushRealisedGapInformation(
//        conjugateTransportModelNetwork, conjNetworkShortestPathAlgo, getOdDemands(mode), conjLinkSegmentCosts);
//
//    double bushUpperBoundGap =
//        (theActiveBush.getRealisedCostForGap() - theActiveBush.getMinCostForGap())/theActiveBush.getMinCostForGap();
//    double bushLowerBoundGap =
//        (scaledMinCostBush - theActiveBush.getMinCostForGap())/theActiveBush.getMinCostForGap();
//
//    // bush smoothing
//    // when lower bound gap is zero we only consider upper bound, wehn non-zero and inching towards upper the bush itself is con
//    theActiveBush.bushSmoothing.updateIsBadIteration(
//        theActiveBush.prevIterationInitialGap, bushUpperBoundGap-bushLowerBoundGap);
//    theActiveBush.bushSmoothing.updateIteration(theActiveBush.bushSmoothing.getIteration() + 1);
//    theActiveBush.prevIterationInitialGap = bushUpperBoundGap-bushLowerBoundGap;
//
//    // here we update step --> once a PAS has converged, we then scale back by performing one final flow shift between the
//    // original setup and the final one to get the correct shift (and network state).
//    if(theActiveBush.bushSmoothing.isBadIteration()){
//      LOGGER.info("BUSH GAP NOT IMPROVING --> BAD ITERATION FOUND --> CONSTRAINING STEP");
//      theActiveBush.bushSmoothing.updateStepSize();
//    }
//
//    boolean bushReachedNetworkGapConvergence = bushUpperBoundGap <= getGapFunction().getStopCriterion().getEpsilon();
//    boolean bushReachMaxConvergenceUnderCycleLimitation = bushLowerBoundGap>0 &&
//        (bushUpperBoundGap-bushLowerBoundGap)/bushLowerBoundGap < Precision.EPSILON_3;
//    if(bushReachedNetworkGapConvergence || bushReachMaxConvergenceUnderCycleLimitation) {
//      if(bushReachedNetworkGapConvergence) {
//        LOGGER.info(String.format("******************* BUSH %s CONVERGED SWITCHING ******************", theActiveBush.getRootZone().getIdsAsString()));
//      }else{
//        // cannot added certain shortest paths due to cycle detection, unable to fully converge, move to other bush instead
//        LOGGER.info(String.format("!!!!!!!!!!!!!!!!!!! BUSH %s CYCLE LIMITED, SWITCHING !!!!!!!!!!!!!!!!!!", theActiveBush.getRootZone().getIdsAsString()));
//      }
//      // change current active bush to next one in rotation, otherwise keep going as we haven't converged
//      boolean activateNext = false;
//      for (var conjBush : getBushes()) {
//        if (conjBush == null) {
//          continue;
//        }
//        if(activateNext) {
//          theActiveBush = conjBush;
//          theActiveBush.currentActiveBush = true;
//          activateNext = false;
//          break;
//        }
//        if (conjBush.currentActiveBush) {
//          activateNext = true;
//          conjBush.currentActiveBush = false; //reset
//          conjBush.prevIterationInitialGap = Double.MAX_VALUE; //reset
//          theActiveBush.bushSmoothing.reset();
//          //requires explicit reset because normally we wouldn't reset to zero, but for bush smoothing we currently
//          // reset it once the bush has converged as this is its only purpose, not across outer loop iterations
//          theActiveBush.bushSmoothing.updateIteration(0);
//        }
//      }
//      if(activateNext) {
//        //wrap around, start with first again
//        theActiveBush = getBushes().stream().filter(Objects::nonNull).findFirst().get();
//        theActiveBush.currentActiveBush = true;
//      }
//
//      // rejig new active bush, otherwise we won't get correct PASs as things have changed on network level
//      // since last bush update
//      optimiseZeroFlowSpanningTreeConnections(theActiveBush, conjLinkSegmentCosts);
//      LOGGER.info(String.format("******************* BUSH %s TO UPDATE (gap %.10f )******************",theActiveBush));
//    }
//    Set<ConjugateDestinationBush> eligibleBushes = Set.of(theActiveBush);


    // ALTERNATIVE BUSH PRUNING BASED ON BUSH GAP CALC
    int countGapSkippedBushes = 0;
    Set<ConjugateDestinationBush> eligibleBushes = new TreeSet<>();
    int totalConvergedBushes = 0;
    int totalCycleLimitedBushes = 0;
    int totalRegularUnconvergedBushes = 0;
    int totalNonImprovingBushes = 0;
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

      //double bushInternalGap = (conjBush.getRealisedCostForGap() - scaledMinCostBush)/scaledMinCostBush;

      boolean bushReachedNetworkGapConvergence = bushUpperBoundGap <= getGapFunction().getStopCriterion().getEpsilon();
      boolean bushReachMaxConvergenceUnderCycleLimitation = bushLowerBoundGap>0 &&
          (bushUpperBoundGap-bushLowerBoundGap)/bushLowerBoundGap < Precision.EPSILON_3;

      if(bushReachedNetworkGapConvergence || bushReachMaxConvergenceUnderCycleLimitation) {
        if (bushReachedNetworkGapConvergence) {
          //LOGGER.info(String.format("******************* BUSH %s CONVERGED (no update) ******************", conjBush.getRootZone().getIdsAsString()));
          // make sure we keep tracking gap even if we're not updating and skipping the rest of the gap/step update
          conjBush.prevIterationInitialGap = bushUpperBoundGap;
          ++totalConvergedBushes;
          continue;
        } else {
          // cannot add certain shortest paths due to cycle detection, unable to fully converge (for now)
          //LOGGER.info(String.format("!!!!!!!!!!!!!!!!!!! BUSH %s CYCLE LIMITED (UPDATE) !!!!!!!!!!!!!!!!!!", conjBush.getRootZone().getIdsAsString()));
          eligibleBushes.add(conjBush);
          ++totalCycleLimitedBushes;
        }
      }else{
//        LOGGER.info(String.format("++++++++++++++++++++ BUSH %s ELIGIBLE FOR UPDATE (gap %.8f) (step %.4f) ++++++++++++++++++",
//            conjBush.getRootZone().getIdsAsString(), bushUpperBoundGap, conjBush.bushSmoothing.executeRefZero(1)));
        eligibleBushes.add(conjBush);
        ++totalRegularUnconvergedBushes;
      }

      // bush smoothing
      // when lower bound gap is zero we only consider upper bound, when non-zero and inching towards upper the bush itself is con
      conjBush.bushSmoothing.updateIsBadIteration(conjBush.prevIterationInitialGap, bushUpperBoundGap);
      conjBush.bushSmoothing.updateIteration(conjBush.bushSmoothing.getIteration() + 1);
      // here we update step --> once a PAS has converged, we then scale back by performing one final flow shift between the
      // original setup and the final one to get the correct shift (and network state).
      if(conjBush.bushSmoothing.isBadIteration() && !bushReachMaxConvergenceUnderCycleLimitation){
//        LOGGER.info(String.format(
//            "BUSH (%s) GAP NOT IMPROVING (%.6f -> %.6f) --> BAD ITERATION FOUND --> CONSTRAINING STEP",
//            conjBush.getRootZone().getIdsAsString(), conjBush.prevIterationInitialGap, bushUpperBoundGap));
        conjBush.bushSmoothing.updateStepSize();
        ++totalNonImprovingBushes;
      }
      conjBush.prevIterationInitialGap = bushUpperBoundGap;

    }
    LOGGER.info(String.format("++++ Updating %d bushes (%.2f%%): %d cycleLimited - %d not improving - %d converged",
      eligibleBushes.size(), ((double)eligibleBushes.size()*100)/getBushes().size(),
        totalCycleLimitedBushes, totalNonImprovingBushes, totalConvergedBushes));
    prevNetworkRealisedCost = totalRealisedCostForGap;

    // **********************************************************************************************
    // FIND PASs for eligible bushes
    Map<Long, Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> passToConsider = new TreeMap<>();
    for (var conjBush : eligibleBushes) {
      // make sure we use the latest zero flow optimal connections --> can only do this now because
      // we should only add/change zero flow connections when we KNOW they will by used in a PAS and will become
      // non-zero flow. Otherwise this leads to problems later on with cycle detection
      // todo: when we compute gap earlier it is not entirely right because we use the pre-updated spanning tree
      //  however, we cannot update it there because if we do not consider the bush the new added links won't be used
      //  leading to the problem mentioned, so we accept we're trailing an iteration with that...
      optimiseZeroFlowSpanningTreeConnections(conjBush, conjLinkSegmentCosts);

      // track vertices that have been added due to PAS s1 alternative
      // any new PAS that touches these vertices will not be added because they overlap
      // overlapping PASs are less effective than non-overlapping ones. Wait until next iteration
      // for when they have flow / top. ordering and bush/min/max is updated. Then try again.
      Set<ConjugateDirectedVertex> addedBushPasS1TouchedVertices = new TreeSet<>();

      // recalculate bush min/max tree as well since any changes from above will result in different min/max
      // paths
      // NOTE: we now EXCLUDE zero flow links from max paths, to avoid generating high cost paths that are not eligible
      //  for flow shifting. We can only do that because we already changed the bush earlier as for that we do require
      // max paths for zero flow links to avoid cycles in adding new links.
      boolean excludeZeroFlowLinksFromMaxPaths = true;
      var bushMinMaxTree = conjBush.computeMinMaxShortestPaths(excludeZeroFlowLinksFromMaxPaths,
          conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());

      /* find (new) matching PASs - start with new PAS close to destination exploration first */
      int countPassAddedForBush = 0;
      int countCongestedPassAddedForBush = 0;
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

          // TODO: temp try using ALL "NOW" PASs of all vertices if eligible and wipe after outer iteration
          //  so we allow any eligible reduced cost vertex for now to be considered

          boolean minPathInitialLinkNewToBush = false;
          boolean divergentMinMaxPaths = !(minNextEdge == maxNextEdge);
          boolean preferredOutGoingSegment = (minNextEdge == outgoingSegment);
          boolean congestedEntry = (entryAcceptanceFactor + Precision.EPSILON_6) < 1;
          if(!conjBush.contains(outgoingSegment)){
            //disallow because by disregarding zero flow links, we could be adding cycles as the eligibility
            // check for adding would nto consider the full max cost spanning tree anymore
            continue;
//            if(!isEligibleForAdding(outgoingSegment, conjLinkSegmentCosts, bushMinMaxTree).first()){
//              continue;
//            }
//            minPathInitialLinkNewToBush = true;
          }else if(conjBush.contains(outgoingSegment)){
            // for existing segments we require a potential reduced cost (checked later) AND a split at the vertex
            // for the min and max path and the current outgoing segment is the preferred (min cost) segment
            if(!divergentMinMaxPaths || !preferredOutGoingSegment){
              continue;
            }
          }

          double reducedCost = -1;
          if(minPathInitialLinkNewToBush) {
            // found segment to add -- necessitates creation of a new PAS because we are merging two possible routes
            conjBush.getDag().addEdgeSegment(outgoingSegment);
            double minCostToVertexWithNewLink = conjLinkSegmentCosts[(int) outgoingSegment.getId()] +
                bushMinMaxTree.getMinCostToReach(outgoingSegment.getDownstreamVertex());
            reducedCost =
                bushMinMaxTree.getMaxCostToReach(conjBushVertex) - minCostToVertexWithNewLink;
          }else /*if(divergentMinMaxPaths && preferredOutGoingSegment) <-- given*/ {
            reducedCost =
                bushMinMaxTree.getMaxCostToReach(conjBushVertex) - bushMinMaxTree.getMinCostToReach(conjBushVertex);
          }

          // find PAS using either min or max cost bush paths

          /* find a (new) PAS for the bush */
          boolean allowUncongestedOnly = MAX_CONGESTED_PAS_ADD_PER_BUSH == countCongestedPassAddedForBush;
          var bushPasExtensionResult = extendConjugateBushWithPas(
              conjBush,
              conjBushVertex,
              outgoingSegment,
              reducedCost,
              bushMinMaxTree,
              conjLinkSegmentCosts,
              addedBushPasS1TouchedVertices,
              allowUncongestedOnly);
          if (bushPasExtensionResult == null || bushPasExtensionResult.first() == null) {

            // ending up not adding the PAS, so remove just added segment again
            if(minPathInitialLinkNewToBush) {
              conjBush.remove(outgoingSegment);
            }
            continue;
          }
          var pasToAdd = bushPasExtensionResult.first();

          // truly new PAS
          passToConsider.put(pasToAdd.pasId, pasToAdd);
//          if(isDestinationTrackedForLogging(conjBush) || logAll){
//            LOGGER.info(String.format("Registered new PAS (%s) on conjugate bush (%s)",
//                pasToAdd, conjBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
//          }

          ++countPassAddedForBush;
          if(pasToAdd.getStatus() == PasStatus.CONGESTED) {
            ++countCongestedPassAddedForBush;
          }
          if(countCongestedPassAddedForBush == MAX_CONGESTED_PAS_ADD_PER_BUSH ||
              countPassAddedForBush - countCongestedPassAddedForBush >= MAX_PAS_ADD_PER_BUSH){
            break BREAK_BUSH;
          }

          // todo: switched off for "non-pas" simulated approach
          // update added PAS vertices - to ban for subsequent new PASs
//          var lowCostAlt = pasToAdd.getAlternative(true);
//          for(int index = 0; index < lowCostAlt.length-1; ++index){
//            addedBushPasS1TouchedVertices.add(lowCostAlt[index].getDownstreamVertex());
//          }
        }
      }
    }

    LOGGER.info(String.format(
        "EXCLUDED %.2f%% = %d bushes from PAS updates because they are below current network gap",
        ((double)countGapSkippedBushes*100)/getBushes().size(), countGapSkippedBushes));
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
