package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushRooted;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Base implementation to support a rooted bush based solution for sLTM
 * 
 * @author markr
 *
 */
public abstract class StaticLtmBushStrategyRootLabelled<B extends RootedLabelledBush>
        extends StaticLtmBushStrategyBase<DirectedVertex, EdgeSegment, B> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategyRootLabelled.class.getCanonicalName());

  /**
   * Check if an existing PAS exists that terminates/starts (depending on bush config) at the given bush vertex. If so,
   * it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * </ul>
   * 
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper segment might or might not overlap with, as long as it is close
   * enough to the potential reduced cost we'll take it to avoid exponential growth of PASs)
   * 
   * @param bush        to consider
   * @param reducedCostVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   * 
   * @return PAS when a match is found and null otherwise (PAS is already registered as part of this call)
   *
   */
  private Pas<DirectedVertex,EdgeSegment> extendBushWithSuitableExistingPas(
      final RootedLabelledBush bush, final DirectedVertex reducedCostVertex, final double reducedCost) {
    //todo: effectively same as for conjugate can be consolidated in base case with some minor changes likely

    boolean bushFlowThroughMergeVertex = false;
    for (var entrySegment : reducedCostVertex.getEntryEdgeSegments()) {
      for (var exitSegment : reducedCostVertex.getExitEdgeSegments()) {
        if (bush.containsTurnSendingFlow(entrySegment, exitSegment)) {
          bushFlowThroughMergeVertex = true;
          break;
        }
      }
      if (bushFlowThroughMergeVertex) {
        break;
      }
    }

    if (!bushFlowThroughMergeVertex) {
      // TODO: when we find this condition never occurs (and it shouldn't, remove the above checks as they are costly)
      LOGGER.warning(String.format("Explored vertex %s for existing PAS match even though bush has no flow passing through it. This should not happen", reducedCostVertex.getXmlId()));
      return null;
    }

    // disable effectiveness check: at some point it was considered effective for some bush, therefore we allow
    // for it to be reactivated
    boolean checkEffectiveness = false;
    boolean inActive = false;
    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    var effectivePas = pasManager.findFirstSuitableActivePas(
            bush, reducedCostVertex, alphas, reducedCost, checkEffectiveness);
    if (effectivePas == null) {
      effectivePas = pasManager.findFirstSuitableInactivePas(
              bush, reducedCostVertex, alphas, reducedCost, checkEffectiveness);
      inActive = true;
    }
    if (effectivePas == null) {
      return null;
    }

    /*
     * found -> register bush for future flow shifting, if PAS was previously inactive, reactivate it so it is
     * considered again.
     */
    if(inActive){
      pasManager.reactivatePas(effectivePas);
    }
    boolean newlyRegistered = effectivePas.registerBush(bush);
    if (newlyRegistered && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%s %s added to PAS %s",
              bush.isInverted() ? "Destination" : "Origin", bush.getRootZoneVertex().getParent().getParentZone().getXmlId(), effectivePas));
    }
    return effectivePas;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that
   * it is considered sufficiently effective the bush is registered on it.
   *
   * @param bush              to identify new PAS for
   * @param reducedCostVertex to use for creating the PAS as a cheaper path to the root exists at this vertex
   * @param networkMinPaths   the current network shortest path tree
   * @param reducedCost       to check if new PAS is considered effective
   * @param linkSegmentCosts  to check if new PAS is considered effective
   * @return new created PAS if successfully created, null otherwise, the boolean indicates if it indeed is a brand new PAS
   * or for some reason we still reused an existing one
   *
   */
  private Pas<DirectedVertex,EdgeSegment> extendBushWithNewPas(
          final RootedLabelledBush bush,
          final DirectedVertex reducedCostVertex,
          final ShortestPathResult networkMinPaths,
          double reducedCost,
          double[] linkSegmentCosts) {
    //todo: effectively same as for conjugate can be consolidated in base clase with some minor changes likely

    /* Label all vertices on shortest path root-reducedCostVertex as -1, and PAS reference vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    int numShortestPathEdgeSegments = networkMinPaths.forEachNextEdgeSegment(bush.getRootVertex(), reducedCostVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) networkMinPaths.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    /* Identify when it coincides again with bush (closer to root) using back link tree BF search */
    var highCostSubPathResultPair =
        bush.findBushAlternativeSubpathBfs(
                reducedCostVertex,
                networkMinPaths.getNextEdgeSegmentForVertex(reducedCostVertex),
                alternativeSegmentVertexLabels);
    if (highCostSubPathResultPair == null || highCostSubPathResultPair.first() == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format(
              "Unable to create new PAS for bush rooted at vertex %s, despite shorter path found on network to vertex %s",
              bush.getRootVertex().getXmlId(), reducedCostVertex.getXmlId()));
      return null;
    }

    /* create the PAS and register bush on it */
    boolean truncateSpareArrayCapacity = true;
    var coincideCloserToRootVertex = highCostSubPathResultPair.first();
    Map<DirectedVertex, EdgeSegment> backLinkTreeAsMap = highCostSubPathResultPair.second();

    /* S1 */
    EdgeSegment[] s1 = PasManager.createSubPathArrayFrom(
            coincideCloserToRootVertex,
            reducedCostVertex,
            networkMinPaths,
            new EdgeSegment[numShortestPathEdgeSegments],
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
    EdgeSegment[] s2 = PasManager.createSubPathArrayFrom(
        coincideCloserToRootVertex,
        reducedCostVertex,
        bush.getShortestSearchType(),
        backLinkTreeAsMap,
        new EdgeSegment[highCostSubPathResultPair.second().size()],
        truncateSpareArrayCapacity);

    var existingPas = pasManager.findMatchingActivePas(s1, s2);
    if (existingPas != null) {
      // exists already but was discarded as option for this bush (otherwise we would not ask for a new PAS to be created
      // not able to create new PAS using this S1/S2 alternative
      //todo: consider alternative S1/S2 creation using DFS for example to see if an alternative partially overlapping PAS
      // could help. For now we just accept no new PAS is to be created for this bush at this vertex
      return null;
    }
    existingPas = pasManager.findMatchingInactivePas(s1, s2);
    if (existingPas != null) {
      // same as above
      return null;
    }

    double highCostAlternativeCost = PasManager.computeCost(s2, linkSegmentCosts);
    double lowCostAlternativeCost = PasManager.computeCost(s1, linkSegmentCosts);
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
    pas.updateCost(linkSegmentCosts);
    /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance factor information */
    getLoading().activateNodeTrackingFor(pas);
    if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format("Created new PAS: %s", pas));
    }

    return pas;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ShortestBushGeneralised createInitialNetworkShortestSearchTreeAlgo(Mode theMode, final double[] linkSegmentCosts) {
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new ShortestBushGeneralised(linkSegmentCosts, numberOfVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] linkSegmentCosts) {
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new ShortestPathDijkstra(linkSegmentCosts, numberOfVertices);
  }

  /**
   * Match (new) PASs to improve existing bushes (origin) at hand.
   * <p>
   * Note that in order to extend the bushes we run a shortest path rooted at each bush's origin, since this is costly, we utilise the result also to update the min-cost gap for
   * each OD which requires the min-cost from each origin to each destination which is what the shortest path trees provide. The updating of the network's actual costs occurs
   * elsewhere
   *
   * @param mode             to use
   * @param linkSegmentCosts to use to construct min-max path three rooted at each bush's origin
   * @param updateGap        flag
   * @param simulationData
   * @param logAll           flag
   * @return newly created PASs and existing pass with newly registered bushes on them  (empty if no new PASs were created or newly assigned))
   */
  @Override
  protected Map<Long,Pas<DirectedVertex,EdgeSegment>> updateBushPass(
      Mode mode, final double[] linkSegmentCosts, boolean updateGap, StaticLtmSimulationData simulationData, boolean logAll){

    double totalMinCost = 0; // track during bush traversal to get min OD costs based on shortest paths
    double totalRealisedCost = 0;
    if(updateGap) {
      // costs as they currently are utilising the unconstrained demand as a point of reference
      for (var linkSegment : getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode).getLinkSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = linkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCost += linkCost * linkDemand;
      }
      for (var linkSegment : getTransportNetwork().getVirtualNetwork().getLayer().getConnectoidSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = linkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCost += linkCost * linkDemand;
      }
    }

    var passToConsider = new TreeMap<Long,Pas<DirectedVertex,EdgeSegment>>();

    final var networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);
    for (var bush : getBushes()) {
      if (bush == null) {
        continue;
      }

      /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root is destination) */
      //todo: we do not yet account for if the path is used --> we should because we will likely get unused max cost paths now!
      var minMaxPaths = bush.computeMinMaxShortestPaths(false,
              linkSegmentCosts, this.getTransportNetwork().getNumberOfVerticesAllLayers());
      if (minMaxPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }
      minMaxPaths.setMinPathState(false);

      /* network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush where destination is root) */
      var networkMinPaths = networkShortestPathAlgo.execute(bush.getShortestSearchType(), bush.getRootVertex());
      if (networkMinPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain network min paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }

      if(updateGap) {
        // update/track total min cost across bushes(Ods) for gap calculation
        var odDemands = getOdDemands(mode);
        var destination = ((DestinationBush) bush).getDestination().getParent().getParentZone();
        for (var originVertex : bush.getOriginVertices()) {
          var origin = originVertex.getParent().getParentZone();
          double odDemand = odDemands.getValue(origin, destination);
          double minOdCost = networkMinPaths.getCostToReach(originVertex);
          totalMinCost += minOdCost * odDemand;
        }
      }

      /* find (new) matching PASs - start with new PAS close to origin exploration first
       *  todo: this is a choice, could choose differently and going with close to destination seems safer */
      var bushVertexIter = bush.isInverted() ? bush.getInvertedTopologicalIterator() : bush.getTopologicalIterator();
      while(bushVertexIter.hasNext()) {
        DirectedVertex bushVertex = bushVertexIter.next();
        EdgeSegment reducedCostSegment = networkMinPaths.getNextEdgeSegmentForVertex(bushVertex);
        if (reducedCostSegment == null) {
          continue;
        }

        double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);
        if(reducedCost <= 0){
          continue;
        }

        if(minMaxPaths.getNextEdgeSegmentForVertex(bushVertex).equals(
                networkMinPaths.getNextEdgeSegmentForVertex(bushVertex))){
          // not the location that they split paths, so should not be creating the start point of PAS here
          continue;
        }

        /* when bush does not contain the opposite direction which would cause a cycle it is worth checking */
        boolean viableSearch =
                reducedCostSegment.getOppositeDirectionSegment()==null ||
                        !bush.contains(reducedCostSegment.getOppositeDirectionSegment());
        if (!viableSearch) {
          // preferred alternative cannot be added due to bush triggering a cycle if we would
          continue;
        }

        var existingRegisteredPas = extendBushWithSuitableExistingPas(bush, bushVertex, reducedCost);
        if (existingRegisteredPas != null) {
          if(isDestinationTrackedForLogging(bush) || logAll){
            LOGGER.info(String.format("Registered suitable existing PAS (%s) on bush (%s)", existingRegisteredPas, bush));
          }
          passToConsider.put(existingRegisteredPas.pasId,existingRegisteredPas);
          continue;
        }

        /* no suitable match, attempt creating an entirely new PAS */
        var newPas = extendBushWithNewPas(bush, bushVertex, networkMinPaths, reducedCost, linkSegmentCosts);
        if (newPas == null) {
          continue;
        }

        // truly new PAS
        passToConsider.put(newPas.pasId, newPas);
        newPas.updateCost(linkSegmentCosts);
        if(isDestinationTrackedForLogging(bush) || logAll){
          LOGGER.info(String.format("Registered new PAS (%s) on bush (%s)",
                  newPas, bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }

        // BRANCH SHIFT
        {
          // NOTE: since we will perform an update on all PASs it seems illogical to also explicitly register the required branch shifts
          // since they will be carried out regardless. Hence we do not log a warning nor implement the branch shift until it appears necessary

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
      gapFunction.increaseMinimumPathCosts(totalMinCost,1);
      gapFunction.increaseAbsolutePathGap(totalRealisedCost, 1, totalMinCost);
    }

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
   * @param registerPassByDiverge when true index registration by diverge, merge otherwise
   */
  protected StaticLtmBushStrategyRootLabelled(
          final IdGroupingToken idGroupingToken,
          long assignmentId,
          final TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportModelNetwork,
          final StaticLtmSettings settings,
          final TrafficAssignmentComponentAccessee taComponents,
          boolean registerPassByDiverge) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents, registerPassByDiverge);
  }

  /**
   * Create bush based network loading implementation
   *
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushRooted createNetworkLoading() {
    return new StaticLtmLoadingBushRooted(
            getIdGroupingToken(), getAssignmentId(), getSegmentToMovementMapping(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushRooted getLoading() {
    return (StaticLtmLoadingBushRooted) super.getLoading();
  }

}
