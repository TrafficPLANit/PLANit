package org.planit.assignment.ltm.sltm;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.MinMaxPathResult;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.ltm.sltm.consumer.InitialiseBushEdgeSegmentDemandConsumer;
import org.planit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.Pair;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.OdZone;
import org.planit.zoning.Zoning;

/**
 * Implementation to support a bush absed solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmBushStrategy extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategy.class.getCanonicalName());

  /** track bushes per origin (with non-zero demand) */
  private final Bush[] originBushes;

  /** track all unique PASs */
  private final PasManager pasManager;

  /**
   * Check if an existing PAS exists that terminates at the given (bush) merge vertex. If so, it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * 
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper segment might or might not overlap with, as long as it is close
   * enough to the potential reduced cost we'll take it to avoid exponential growth of PASs)
   * 
   * @param originBush  to consider
   * @param mergeVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   * 
   * @return true when a match is found and bush is newly registered on a PAS, false otherwise
   */
  private boolean extendBushWithSuitableExistingPas(final Bush originBush, final DirectedVertex mergeVertex, final double reducedCost) {

    boolean bushFlowThroughMergeVertex = false;
    for (EdgeSegment entrySegment : mergeVertex.getEntryEdgeSegments()) {
      for (EdgeSegment exitSegment : mergeVertex.getExitEdgeSegments()) {
        if (originBush.containsTurnSendingFlow(entrySegment, exitSegment)) {
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
      LOGGER.warning(String.format("Explored vertex %s for existing PAS match even though bush has not flow passing through it. This should not happen", mergeVertex.getXmlId()));
      return false;
    }

    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    Pas effectivePas = pasManager.findFirstSuitableExistingPas(originBush, mergeVertex, alphas, reducedCost);
    if (effectivePas == null) {
      return false;
    }

    /*
     * found -> register origin, shifting of flow occurs when updating pas, extending bush with low cost segment occurs automatically when shifting flow later (flow is added to low
     * cost link segments which will be created if non-existent on bush)
     */
    effectivePas.registerOrigin(originBush);
    return true;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that it is considered sufficiently effective the origin is
   * registered on it.
   * 
   * @param originBush      to identify new PAS for
   * @param mergeVertex     at which the PAS is supposed to terminate
   * @param networkMinPaths the current network shortest path tree
   * @return true when a new PAS was successfully created, false otherwise
   */
  private boolean extendBushWithNewPas(Bush originBush, DirectedVertex mergeVertex, ShortestPathResult networkMinPaths) {
    // TODO: below is not entirely correct yet, but the way to identify a new PAS

    /* Label all vertices on shortest path origin-bushVertex as -1, and PAS merge Vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) mergeVertex.getId()] = 1;
    int shortestPathLength = networkMinPaths.forEachBackwardEdgeSegment(originBush.getOrigin().getCentroid(), mergeVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) edgeSegment.getUpstreamVertex().getId()] = -1);

    /* Use labels to identify when it merges again with bush (at upstream diverge point) */
    Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> highCostSegment = originBush.findBushAlternativeSubpath(mergeVertex, alternativeSegmentVertexLabels);
    if (highCostSegment == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format("Unable to create new PAS for origin zone %s, despite shorter path found on network to vertex %s", originBush.getOrigin().getXmlId(),
          mergeVertex.getXmlId()));
      return false;
    }

    /* create the PAS and register origin bush on it */
    EdgeSegment[] s1 = PasManager.createSubpathArrayFrom(highCostSegment.first(), mergeVertex, networkMinPaths, shortestPathLength);
    EdgeSegment[] s2 = PasManager.createSubpathArrayFrom(highCostSegment.first(), mergeVertex, highCostSegment.second(), shortestPathLength);
    pasManager.createNewPas(originBush, s1, s2);

    /* TODO: Not needed (yet) if we postpone the shifting of flow to the PAS update. Instead simply register the origin after creating the PAS */
    double highCostSegmentFlow = originBush.computeSubPathSendingFlow(highCostSegment.first(), mergeVertex, highCostSegment.second());

    /* IF -1 -> potential NEW PAS, when 1 cycle */
    // TODO:CONTINUE

    return true;
  }

  /**
   * Create a network wide shortest path algorithm based on provided costs
   * 
   * @param linkSegmentCosts to use
   * @return one-to-all shortest path algorithm
   */
  private OneToAllShortestPathAlgorithm createNetworkShortestPathAlgo(final double[] linkSegmentCosts) {
    final int numberOfEdgeSegments = getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new DijkstraShortestPathAlgorithm(linkSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Initialise bushes. Find shortest path for each origin and add the links to the bush
   * 
   * @param odDemands        demands used
   * @param linkSegmentCosts costs to use
   * @throws PlanItException thrown when error
   */
  private void initialiseBushes(final OdDemands odDemands, final double[] linkSegmentCosts) throws PlanItException {
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = createNetworkShortestPathAlgo(linkSegmentCosts);

    Zoning zoning = getTransportNetwork().getZoning();
    for (OdZone origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = null;
      InitialiseBushEdgeSegmentDemandConsumer initialiseBushConsumer = null;
      Bush originBush = null;
      for (OdZone destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          if (originBush == null) {
            /* register new bush */
            originBush = new Bush(getIdGroupingToken(), origin, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
            originBushes[(int) origin.getOdZoneId()] = originBush;
            initialiseBushConsumer = new InitialiseBushEdgeSegmentDemandConsumer(originBush);
          }
          /* ensure bush initialisation is applied to the right destination/demand */
          initialiseBushConsumer.setDestination(destination.getCentroid(), currOdDemand);

          /* find one-to-all shortest paths */
          if (oneToAllResult == null) {
            oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
          }

          /* initialise bush with this destination shortest path */
          oneToAllResult.forEachBackwardEdgeSegment(origin.getCentroid(), destination.getCentroid(), initialiseBushConsumer);
        }

      }
    }
  }

  /**
   * Match (new) PASs to improve existing bushes (origin) at hand.
   * 
   * @param linkSegmentCosts to use to construct min-max path three rooted at each bush's origin
   * @throws PlanItException thrown if error
   */
  private void extendBushes(final double[] linkSegmentCosts) throws PlanItException {

    OneToAllShortestPathAlgorithm networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);
    for (int index = 0; index < originBushes.length; ++index) {
      Bush originBush = originBushes[index];
      if (originBush != null) {

        /* within-bush min/max-paths */
        MinMaxPathResult minMaxPaths = originBush.computeMinMaxShortestPaths(linkSegmentCosts);

        /* network min-paths */
        ShortestPathResult networkMinPaths = networkShortestPathAlgo.executeOneToAll(originBush.getOrigin().getCentroid());

        /* find (new) matching PASs */
        for (Iterator<DirectedVertex> bushVertexIter = originBush.getDirectedVertexIterator(); bushVertexIter.hasNext();) {
          DirectedVertex bushVertex = bushVertexIter.next();

          /* when bush does not contain the reduced cost edge segment (or the opposite direction which would cause a cycle) consider it */
          EdgeSegment reducedCostSegment = networkMinPaths.getIncomingEdgeSegmentForVertex(bushVertex);
          if (!originBush.containsAnyEdgeSegmentOf(reducedCostSegment.getParentEdge())) {

            double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);

            boolean matchFound = extendBushWithSuitableExistingPas(originBush, bushVertex, reducedCost);
            if (matchFound) {
              continue;
            }

            /* no suitable match, attempt creating an entirely new PAS */
            boolean newPasCreated = extendBushWithNewPas(originBush, bushVertex, networkMinPaths);
            if (newPasCreated) {
              continue;
            }

            // BRANCH SHIFT
            {
              // NOTE: since we will perform an update on all PASs it seems illogical to also explicitly register the required branch shifts
              // since they will be carried out regardless. Hence we do not log a warning nor implement the branch shift until it appears necessary

              /* no suitable new or existing PAS could be found given the conditions applied, do a branch shift instead */
              // LOGGER.info("No existing/new PAS found that satisifes flow/cost effective conditions for origin bush %s, consider branch shift - not yet implemented");
              // TODO: currently not implemented yet -> requires shifting flow on existing bush with the given vertex as the end point
            }

          }
        }
      }
    }
  }

  /**
   * Update existing PASs. Whenever flow shift is required, do it for affected bushes.
   * 
   * @param linkSegmentCosts to use
   * @return true when at least one update has been performed, false otherwise
   */
  private boolean updatePairedAlternativeSegments(double[] linkSegmentCosts) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Create bush based network loading implementation
   * 
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBush createNetworkLoading() {
    return new StaticLtmLoadingBush(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBush getLoading() {
    return (StaticLtmLoadingBush) super.getLoading();
  }

  /**
   * Create initial bushes, where for each origin the bush is initialised with the shortest path only
   */
  @Override
  protected void createInitialSolution(TimePeriod timePeriod, OdDemands odDemands, double[] initialLinkSegmentCosts) {
    try {
      initialiseBushes(odDemands, initialLinkSegmentCosts);
      getLoading().setBushes(originBushes);
    } catch (PlanItException e) {
      LOGGER.severe(String.format("Unable to create initial bushes for sLTM %d", getAssignmentId()));
    }
  }

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param smoothing             to use
   */
  public StaticLtmBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final Smoothing smoothing) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, smoothing);
    this.originBushes = new Bush[transportModelNetwork.getZoning().getOdZones().size()];
    this.pasManager = new PasManager();
  }

  //@formatter:off
  /**
   * Perform an iteration by:
   *  
   * 1. Identify new PASs and shift flow from affected bushes
   * 2. Conduct another loading update based on adjusted PASs and bushes
   * 3. Update Bushes by shifting flow between existing PASs 
   * 4. Conducting a loading to obtain network costs 
   * 
   * @param linkSegmentCosts to use
   */
  @Override
  public void performIteration(final double[] linkSegmentCosts) {

    try {
            
      /* update PAS costs */
      pasManager.updateCosts(linkSegmentCosts);
    
      /* (NEW) PAS matching for bushes */
      extendBushes(linkSegmentCosts);
      
      /* shift flows*/
      boolean flowShifted = pasManager.shiftFlows(getLoading(), getSmoothing());
      
      if(flowShifted) {
        // NETWORK LOADING - MODE AGNOSTIC FOR NOW
        executeNetworkLoading();
      }
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration");
    }
    

  }   
  

}
