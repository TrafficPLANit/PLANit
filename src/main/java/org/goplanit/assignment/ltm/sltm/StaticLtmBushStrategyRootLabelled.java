package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushRooted;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;

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
   * Check if an existing PAS exists that terminates at the given bush vertex. If so, it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * 
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper segment might or might not overlap with, as long as it is close
   * enough to the potential reduced cost we'll take it to avoid exponential growth of PASs)
   * 
   * @param bush        to consider
   * @param mergeVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   * 
   * @return true when a match is found and bush is newly registered on a PAS, false otherwise
   */
  private boolean extendBushWithSuitableExistingPas(final RootedLabelledBush bush, final DirectedVertex mergeVertex, final double reducedCost) {

    boolean bushFlowThroughMergeVertex = false;
    for (var entrySegment : mergeVertex.getEntryEdgeSegments()) {
      for (var exitSegment : mergeVertex.getExitEdgeSegments()) {
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
      LOGGER.warning(String.format("Explored vertex %s for existing PAS match even though bush has no flow passing through it. This should not happen", mergeVertex.getXmlId()));
      return false;
    }

    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    Pas effectivePas = pasManager.findFirstSuitableExistingPas(bush, mergeVertex, alphas, reducedCost);
    if (effectivePas == null) {
      return false;
    }

    /*
     * found -> register origin, shifting of flow occurs when updating pas, extending bush with low cost segment occurs automatically when shifting flow later (flow is added to low
     * cost link segments which will be created if non-existent on bush)
     */
    boolean newlyRegistered = effectivePas.registerBush(bush);
    if (newlyRegistered && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("%s %s added to PAS %s", bush.isInverted() ? "Destination" : "Origin", bush.getRootZoneVertex().getXmlId(), effectivePas.toString()));
    }
    return true;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that it is considered sufficiently effective the origin is
   * registered on it.
   * 
   * @param bush              to identify new PAS for
   * @param reducedCostVertex to use for creating the PAS as a cheaper path to the root exists at this vertex
   * @param networkMinPaths   the current network shortest path tree
   * @return new created PAS if successfully created, null otherwise
   */
  private Pas extendBushWithNewPas(final RootedLabelledBush bush, final DirectedVertex reducedCostVertex, final ShortestPathResult networkMinPaths) {

    /* Label all vertices on shortest path root-reducedCostVertex as -1, and PAS merge Vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    int numShortestPathEdgeSegments = networkMinPaths.forEachNextEdgeSegment(bush.getRootVertex(), reducedCostVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) networkMinPaths.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    /* Use labels to identify when it coincides again with bush (closer to root) */
    Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> highCostSegment = bush.findBushAlternativeSubpath(reducedCostVertex, alternativeSegmentVertexLabels);
    if (highCostSegment == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format("Unable to create new PAS for bush rooted at vertex %s, despite shorter path found on network to vertex %s", bush.getRootVertex().getXmlId(),
          reducedCostVertex.getXmlId()));
      return null;
    }

    /* create the PAS and register origin bush on it */
    boolean truncateSpareArrayCapacity = true;
    var coincideCloserToRootVertex = highCostSegment.first();

    /* S1 */
    EdgeSegment[] s1 = PasManager.createSubpathArrayFrom(coincideCloserToRootVertex, reducedCostVertex, networkMinPaths, numShortestPathEdgeSegments, truncateSpareArrayCapacity);
    var cycleInducingSegment = bush.determineIntroduceCycle(s1);
    if (cycleInducingSegment != null) {
      /*
       * this can happen if the merge vertex can only be reached by traversing the bush in opposite direction of existing edge segment on the bush. In which case, an alternative
       * PAS further upstream should be considered, now identify this and ignore PAS as it is sub-optimal and cycle inducing
       */
      LOGGER.fine(String.format("Newly identified PAS alternative for bush rooted at vertex (%s) would introduce cycle on low cost alternative (edge segment %s), ignore",
          bush.getRootVertex().toString(), cycleInducingSegment.getXmlId()));
      return null;
    }

    /* S2 */
    EdgeSegment[] s2 = PasManager.createSubpathArrayFrom(coincideCloserToRootVertex, reducedCostVertex, bush.getShortestSearchType(), highCostSegment.second(),
        highCostSegment.second().size(), truncateSpareArrayCapacity);

    /* register on existing PAS (if available) otherwise create new PAS */
    Pas exitingPas = pasManager.findExistingPas(s1, s2);
    if (exitingPas != null) {
      exitingPas.registerBush(bush);
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
   * @param linkSegmentCosts to use to construct min-max path three rooted at each bush's origin
   * @return newly created PASs (empty if no new PASs were created)
   * @throws PlanItException thrown if error
   */
  @Override
  protected Collection<Pas> updateBushPass(final double[] linkSegmentCosts) throws PlanItException {

    List<Pas> newPass = new ArrayList<>();

    final var networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);

    for (int index = 0; index < bushes.length; ++index) {
      RootedLabelledBush bush = bushes[index];
      if (bush == null) {
        continue;
      }

      /* within-bush min/max-paths */
      var minMaxPaths = bush.computeMinMaxShortestPaths(linkSegmentCosts, this.getTransportNetwork().getNumberOfVerticesAllLayers());
      if (minMaxPaths == null) {
        LOGGER.severe(String.format("Unable to obtain min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }

      /* network min-paths */
      var networkMinPaths = networkShortestPathAlgo.execute(bush.getShortestSearchType(), bush.getRootVertex());
      if (networkMinPaths == null) {
        LOGGER.severe(String.format("Unable to obtain network min paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }

      /* find (new) matching PASs */
      for (var bushVertexIter = bush.getDirectedVertexIterator(); bushVertexIter.hasNext();) {
        DirectedVertex bushVertex = bushVertexIter.next();

        EdgeSegment reducedCostSegment = networkMinPaths.getNextEdgeSegmentForVertex(bushVertex);
        if (reducedCostSegment == null) {
          continue;
        }
        double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);

        /* when bush does not contain the reduced cost edge segment (or the opposite direction which would cause a cycle) consider it */
        if (reducedCost > 0 && !bush.containsAnyEdgeSegmentOf(reducedCostSegment.getParent())) {

          boolean matchFound = extendBushWithSuitableExistingPas(bush, bushVertex, reducedCost);
          if (matchFound) {
            continue;
          }

          /* no suitable match, attempt creating an entirely new PAS */
          Pas newPas = extendBushWithNewPas(bush, bushVertex, networkMinPaths);
          if (newPas == null) {
            continue;
          }

          newPass.add(newPas);
          newPas.updateCost(linkSegmentCosts);

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
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushRooted createNetworkLoading() {
    return new StaticLtmLoadingBushRooted(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushRooted getLoading() {
    return (StaticLtmLoadingBushRooted) super.getLoading();
  }

}
