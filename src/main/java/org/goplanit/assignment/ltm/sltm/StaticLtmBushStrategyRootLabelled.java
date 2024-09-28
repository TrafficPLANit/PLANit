package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushRooted;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.zoning.OdZone;

/**
 * Base implementation to support a rooted bush based solution for sLTM
 * 
 * @author markr
 *
 */
public abstract class StaticLtmBushStrategyRootLabelled extends StaticLtmBushStrategyBase<RootedLabelledBush> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategyRootLabelled.class.getCanonicalName());

  /**
   * Check if an existing PAS exists that terminates/starts (Depending on bush config) at the given bush vertex. If so,
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
   */
  private Pas extendBushWithSuitableExistingPas(
      final RootedLabelledBush bush, final DirectedVertex reducedCostVertex, final double reducedCost) {

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

    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    Pas effectivePas = pasManager.findFirstSuitableExistingPas(bush, reducedCostVertex, alphas, reducedCost);
    if (effectivePas == null) {
      return null;
    }

    /*
     * found -> register origin, shifting of flow occurs when updating pas, extending bush with low cost segment occurs automatically when shifting flow later (flow is added to low
     * cost link segments which will be created if non-existent on bush)
     */
    boolean newlyRegistered = effectivePas.registerBush(bush);
    if (newlyRegistered && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%s %s added to PAS %s", bush.isInverted() ? "Destination" : "Origin", bush.getRootZoneVertex().getXmlId(), effectivePas.toString()));
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
   * @return new created PAS if successfully created, null otherwise
   */
  private Pas extendBushWithNewPas(
          final RootedLabelledBush bush, final DirectedVertex reducedCostVertex, final ShortestPathResult networkMinPaths) {

    /* Label all vertices on shortest path root-reducedCostVertex as -1, and PAS reference vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    int numShortestPathEdgeSegments = networkMinPaths.forEachNextEdgeSegment(bush.getRootVertex(), reducedCostVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) networkMinPaths.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    /* Identify when it coincides again with bush (closer to root) using back link tree BF search */
    var highCostSubPathResultPair =
        bush.findBushAlternativeSubpathByBackLinkTree(
                reducedCostVertex, networkMinPaths.getNextEdgeSegmentForVertex(reducedCostVertex), alternativeSegmentVertexLabels);
    if (highCostSubPathResultPair == null || highCostSubPathResultPair.first() == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format("Unable to create new PAS for bush rooted at vertex %s, despite shorter path found on network to vertex %s", bush.getRootVertex().getXmlId(),
          reducedCostVertex.getXmlId()));
      return null;
    }

    /* create the PAS and register bush on it */
    boolean truncateSpareArrayCapacity = true;
    var coincideCloserToRootVertex = highCostSubPathResultPair.first();
    Map<DirectedVertex, EdgeSegment> backLinkTreeAsMap = highCostSubPathResultPair.second();

    /* S1 */
    EdgeSegment[] s1 = PasManager.createSubpathArrayFrom(
            coincideCloserToRootVertex, reducedCostVertex, networkMinPaths, numShortestPathEdgeSegments, truncateSpareArrayCapacity);
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
    EdgeSegment[] s2 = PasManager.createSubpathArrayFrom(
        coincideCloserToRootVertex,
        reducedCostVertex,
        bush.getShortestSearchType(),
        backLinkTreeAsMap,
        highCostSubPathResultPair.second().size(),
        truncateSpareArrayCapacity);

    /* register on existing PAS (if available) otherwise create new PAS */
    Pas existingPas = pasManager.findExistingPas(s1, s2);
    if (existingPas != null) {
      //todo: it could be that this pass was discarded earlier as suitable, perhaps we should
      //      do this check here again, and if it is not sufficiently attractive discard it?
      if(getSettings().isDetailedLogging() || isDestinationTrackedForLogging(bush)) {
        LOGGER.warning(String.format("Using existing PAS (%s) for bush (%s) while asking for new pas to be created, " +
            "possibly existing PAS was discarded as not suitable before...", existingPas, bush));
      }
      existingPas.registerBush(bush);
      return null;
    }

    /* New pas */
    Pas pas = pasManager.createAndRegisterNewPas(bush, s1, s2);
    /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance factor information */
    getLoading().activateNodeTrackingFor(pas);
    return pas;
  }

  /**
   * Match (new) PASs to improve existing bushes (origin) at hand.
   * <p>
   * Note that in order to extend the bushes we run a shortest path rooted at each bush's origin, since this is costly, we utilise the result also to update the min-cost gap for
   * each OD which requires the min-cost from each origin to each destination which is what the shortest path trees provide. The updating of the network's actual costs occurs
   * elsewhere
   *
   * @param mode to use
   * @param linkSegmentCosts to use to construct min-max path three rooted at each bush's origin
   * @return newly created PASs (empty if no new PASs were created)
   * @throws PlanItException thrown if error
   */
  @Override
  protected Collection<Pas> updateBushPass(Mode mode, final double[] linkSegmentCosts){

    List<Pas> newPass = new ArrayList<>();

    final var networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);
    for (RootedLabelledBush bush : bushes) {
      if (bush == null) {
        continue;
      }

      /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root is destination) */
      //todo: we do not yet account for if the path is used --> we should because we will likely get unused max cost paths now!
      var minMaxPaths = bush.computeMinMaxShortestPaths(
              linkSegmentCosts, this.getTransportNetwork().getNumberOfVerticesAllLayers());
      if (minMaxPaths == null) {
        LOGGER.severe(String.format("Unable to obtain min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }
      minMaxPaths.setMinPathState(false);

      /* network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush where destination is root) */
      var networkMinPaths = networkShortestPathAlgo.execute(bush.getShortestSearchType(), bush.getRootVertex());
      if (networkMinPaths == null) {
        LOGGER.severe(String.format("Unable to obtain network min paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }

      /* find (new) matching PASs - start with new PAS close to origin exploration first
       *  todo: this is a choice, could choose differently but we check all so likely not very influential */
      var bushVertexIter = bush.isInverted() ? bush.getInvertedTopologicalIterator() : bush.getTopologicalIterator();
      for (; bushVertexIter.hasNext(); ) {
        DirectedVertex bushVertex = bushVertexIter.next();
        EdgeSegment reducedCostSegment = networkMinPaths.getNextEdgeSegmentForVertex(bushVertex);
        if (reducedCostSegment == null) {
          continue;
        }

        double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);
        if(reducedCost <= 0){
          continue;
        }

        if(minMaxPaths.getNextEdgeSegmentForVertex(bushVertex).equals(networkMinPaths.getNextEdgeSegmentForVertex(bushVertex))){
          // not the location that they split paths, so should not be creating the start point of PAS here
          continue;
        }

        /* when bush does not contain the opposite direction which would cause a cycle it is worth checking */
        boolean viableSearch =
                reducedCostSegment.getOppositeDirectionSegment()==null || !bush.containsEdgeSegment(reducedCostSegment.getOppositeDirectionSegment());
        if (!viableSearch) {
          // preferred alternative cannot be added due to bush triggering a cycle if we would
          // todo: check what happens when terminate because if this gets still triggered then we have not technically
          //  converged and we have a problem...
          continue;
        }

        Pas existingRegisteredPas = extendBushWithSuitableExistingPas(bush, bushVertex, reducedCost);
        if (existingRegisteredPas != null) {
          if(isDestinationTrackedForLogging(bush)){
            LOGGER.info(String.format("Registered suitable existing PAS (%s) on bush (%s)", existingRegisteredPas, bush));
          }
          continue;
        }

        /* no suitable match, attempt creating an entirely new PAS */
        Pas newPas = extendBushWithNewPas(bush, bushVertex, networkMinPaths);
        if (newPas == null) {
          continue;
        }

        newPass.add(newPas);
        newPas.updateCost(linkSegmentCosts);
        if(isDestinationTrackedForLogging(bush)){
          LOGGER.info(String.format("Registered new PAS (%s) on bush (%s)", newPass, bush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
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
    return newPass;
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
  protected StaticLtmBushStrategyRootLabelled(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
                                              final StaticLtmSettings settings, final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
  }

  /**
   * Create bush based network loading implementation
   *
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *  to splitting rate data format
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushRooted createNetworkLoading(MultiKeyMap<Object, Movement> segmentPair2MovementMap) {
    return new StaticLtmLoadingBushRooted(getIdGroupingToken(), getAssignmentId(), segmentPair2MovementMap, getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushRooted getLoading() {
    return (StaticLtmLoadingBushRooted) super.getLoading();
  }

}
