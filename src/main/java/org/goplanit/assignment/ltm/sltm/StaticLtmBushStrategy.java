package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.DijkstraShortestPathAlgorithm;
import org.goplanit.algorithms.shortest.OneToAllShortestBushAlgorithm;
import org.goplanit.algorithms.shortest.OneToAllShortestBushAlgorithmImpl;
import org.goplanit.algorithms.shortest.OneToAllShortestPathAlgorithm;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public abstract class StaticLtmBushStrategy extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategy.class.getCanonicalName());

  /** track bushes per origin (with non-zero demand) */
  protected final OriginBush[] bushes;

  /** track all unique PASs */
  protected final PasManager pasManager;

  /** track all PASs where we are attempting to distribute flow equally to obtain unique solution under unequal flow but equal cost/cost-derivative */
  protected Set<Pas> equalFlowDistributedPass;

  /**
   * Update gap. Unconventional gap function where we update the GAP based on PAS cost discrepancy. This is due to the impossibility of efficiently determining the network and
   * minimum path costs in a capacity constrained bush based setting. Instead we:
   * <p>
   * minimumCost PAS : s1 cost * SUM(s1 sending flow, s2 sending flow) measuredCost PAS: s1 sending flow * s1 cost + s2 sending flow * s2 cost
   * <p>
   * Sum the above over all PASs. Note that PASs can (partially) overlap, so the measured cost does likely not add up to the network cost
   * 
   * @param gapFunction   to use
   * @param pas           to compute for
   * @param s1SendingFlow of the PAS s1 segment
   * @param s2SendingFlow of the PAS s2 segment
   */
  private void updateGap(final LinkBasedRelativeDualityGapFunction gapFunction, final Pas pas, double s1SendingFlow, double s2SendingFlow) {
    gapFunction.increaseConvexityBound(pas.getAlternativeLowCost() * (s1SendingFlow + s2SendingFlow));
    gapFunction.increaseMeasuredCost(s1SendingFlow * pas.getAlternativeLowCost());
    gapFunction.increaseMeasuredCost(s2SendingFlow * pas.getAlternativeHighCost());
  }

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
  private boolean extendBushWithSuitableExistingPas(final OriginBush originBush, final DirectedVertex mergeVertex, final double reducedCost) {

    boolean bushFlowThroughMergeVertex = false;
    for (var entrySegment : mergeVertex.getEntryEdgeSegments()) {
      for (var exitSegment : mergeVertex.getExitEdgeSegments()) {
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
      LOGGER.warning(String.format("Explored vertex %s for existing PAS match even though bush has no flow passing through it. This should not happen", mergeVertex.getXmlId()));
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
    effectivePas.registerBush(originBush);
    if (getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("Origin %s added to PAS %s", originBush.getOrigin().getXmlId(), effectivePas.toString()));
    }
    return true;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that it is considered sufficiently effective the origin is
   * registered on it.
   * 
   * @param originBush      to identify new PAS for
   * @param mergeVertex     at which the PAS is supposed to terminate
   * @param networkMinPaths the current network shortest path tree
   * @return new created PAS if successfully created, null otherwise
   */
  private Pas extendBushWithNewPas(final OriginBush originBush, final DirectedVertex mergeVertex, final ShortestPathResult networkMinPaths) {

    /* Label all vertices on shortest path origin-bushVertex as -1, and PAS merge Vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) mergeVertex.getId()] = 1;
    int numShortestPathEdgeSegments = networkMinPaths.forEachBackwardEdgeSegment(originBush.getOrigin().getCentroid(), mergeVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) edgeSegment.getUpstreamVertex().getId()] = -1);

    /* Use labels to identify when it merges again with bush (at upstream diverge point) */
    Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> highCostSegment = originBush.findBushAlternativeSubpath(mergeVertex, alternativeSegmentVertexLabels);
    if (highCostSegment == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format("Unable to create new PAS for origin zone %s, despite shorter path found on network to vertex %s", originBush.getOrigin().getXmlId(),
          mergeVertex.getXmlId()));
      return null;
    }

    /* create the PAS and register origin bush on it */
    boolean truncateSpareArrayCapacity = true;
    var divergeVertex = highCostSegment.first();

    /* S1 */
    EdgeSegment[] s1 = PasManager.createSubpathArrayFrom(divergeVertex, mergeVertex, networkMinPaths, numShortestPathEdgeSegments, truncateSpareArrayCapacity);
    var cycleInducingSegment = originBush.determineIntroduceCycle(s1);
    if (cycleInducingSegment != null) {
      /*
       * this can happen if the merge vertex can only be reached by traversing the bush in opposite direction of existing edge segment on the bush. In which case, an alternative
       * PAS further upstream should be considered, now identify this and ignore PAS as it is sub-optimal and cycle inducing
       */
      LOGGER.fine(String.format("Newly identified PAS alternative for origin (%s) would introduce cycle on low cost alternative (edge segment %s), ignore",
          originBush.getOrigin().toString(), cycleInducingSegment.getXmlId()));
      return null;
    }

    /* S2 */
    EdgeSegment[] s2 = PasManager.createSubpathArrayFrom(divergeVertex, mergeVertex, highCostSegment.second(), highCostSegment.second().size(), truncateSpareArrayCapacity);

    /* register on existing PAS (if available) otherwise create new PAS */
    Pas pas = pasManager.findExistingPas(s1, s2);
    if (pas != null) {
      pas.registerBush(originBush);
    } else {
      pas = pasManager.findExistingPas(s2, s1);
      if (pas != null) {
        LOGGER.severe(String.format("existing pas has inverted high/low segment costs compared to current situation, shouldn't happen"));
        pas.registerBush(originBush);
      } else {
        pas = pasManager.createAndRegisterNewPas(originBush, s1, s2);
        /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance factor information */
        getLoading().activateNodeTrackingFor(pas);
      }
    }

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
  private Collection<Pas> updateBushPass(final double[] linkSegmentCosts) throws PlanItException {

    List<Pas> newPass = new ArrayList<>();

    final var networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);

    for (int index = 0; index < bushes.length; ++index) {
      Bush bush = bushes[index];
      if (bush != null && bush instanceof OriginBush) {
        OriginBush originBush = (OriginBush) bush;

        /* within-bush min/max-paths */
        var minMaxPaths = originBush.computeMinMaxShortestPaths(linkSegmentCosts, this.getTransportNetwork().getNumberOfVerticesAllLayers());
        if (minMaxPaths == null) {
          LOGGER.severe(String.format("Unable to obtain min-max paths on bush (origin %s), this shouldn't happen, skip updateBushPass", originBush.getOrigin().getXmlId()));
          continue;
        }

        /* network min-paths */
        var networkMinPaths = networkShortestPathAlgo.executeOneToAll(originBush.getOrigin().getCentroid());
        if (networkMinPaths == null) {
          LOGGER.severe(String.format("Unable to obtain network min paths on bush (origin %s), this shouldn't happen, skip updateBushPass", originBush.getOrigin().getXmlId()));
          continue;
        }

        /* find (new) matching PASs */
        for (var bushVertexIter = originBush.getDirectedVertexIterator(); bushVertexIter.hasNext();) {
          DirectedVertex bushVertex = bushVertexIter.next();

          EdgeSegment reducedCostSegment = networkMinPaths.getIncomingEdgeSegmentForVertex(bushVertex);
          if (reducedCostSegment == null) {
            continue;
          }
          double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);

          /* when bush does not contain the reduced cost edge segment (or the opposite direction which would cause a cycle) consider it */
          if (reducedCost > 0 && !originBush.containsAnyEdgeSegmentOf(reducedCostSegment.getParentEdge())) {

            boolean matchFound = extendBushWithSuitableExistingPas(originBush, bushVertex, reducedCost);
            if (matchFound) {
              continue;
            }

            /* no suitable match, attempt creating an entirely new PAS */
            Pas newPas = extendBushWithNewPas(originBush, bushVertex, networkMinPaths);
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
    }
    return newPass;
  }

  /**
   * Shift flows based on the registered PASs and their origins.
   * 
   * @param theMode to use
   * @return all PASs where non-zero flow was shifted on
   */
  private Collection<Pas> shiftFlows(final Mode theMode) {
    equalFlowDistributedPass.clear();
    var flowShiftedPass = new ArrayList<Pas>((int) pasManager.getNumberOfPass());
    var passWithoutOrigins = new ArrayList<Pas>();

    var networkLoading = getLoading();
    var gapFunction = (LinkBasedRelativeDualityGapFunction) getTrafficAssignmentComponent(GapFunction.class);
    var physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    var virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);

    /**
     * Sort all PAss by their reduced cost ensuring we shift flows from the most attractive shift towards the least where we exclude all link segments of a processed PAS such that
     * no other PASs are allowed to shift flows if they overlap to avoid using inconsistent costs after a flow shift to or from a link segment
     */
    BitSet linkSegmentsUsed = new BitSet(networkLoading.getCurrentInflowsPcuH().length);
    Collection<Pas> sortedPass = pasManager.getPassSortedByReducedCost();
    for (Pas pas : sortedPass) {

      var pasFlowShifter = createPasFlowShiftExecutor(pas, getSettings());
      pasFlowShifter.initialise(); // to be able to collect pas sending flows for gap
      updateGap(gapFunction, pas, pasFlowShifter.getS1SendingFlow(), pasFlowShifter.getS2SendingFlow());

      if (pas.containsAny(linkSegmentsUsed)) {
        continue;
      }

      /* untouched PAS (no flows shifted yet) in this iteration */
      boolean pasFlowShifted = pasFlowShifter.run(theMode, physicalCost, virtualCost, networkLoading);
      if (pasFlowShifted) {
        flowShiftedPass.add(pas);

        /*
         * When flow is shifted we disallow overlapping other PASs to shift flow in this iteration as cost is likely to change. However, when flow is shifted to maximise entropy,
         * it means cost is already equal, and is expected to not be affected by shift. Hence, in that case we do not disallow other PASs to shift flow and do not mark the PASs
         * link segments as "used".
         */
        if (pasFlowShifter.isTowardsEqualAlternativeFlowDistribution()) {
          equalFlowDistributedPass.add(pas);
          continue;
        }

        /* s1 */
        pas.forEachEdgeSegment(true /* low cost */, (es) -> linkSegmentsUsed.set((int) es.getId()));
        /* s2 */
        pas.forEachEdgeSegment(false /* high cost */, (es) -> linkSegmentsUsed.set((int) es.getId()));

        pasFlowShifter.getUsedCongestedEntrySegments().forEach(es -> linkSegmentsUsed.set((int) es.getId()));

        /* when s2 no longer used on any bush - mark PAS for overall removal */
        if (!pas.hasRegisteredBushes()) {
          passWithoutOrigins.add(pas);
        }
      }
    }

    if (!passWithoutOrigins.isEmpty()) {
      passWithoutOrigins.forEach((pas) -> pasManager.removePas(pas, getSettings().isDetailedLogging()));
    }
    return flowShiftedPass;
  }

  private void syncBushTurnFlows() {
    for (var originBush : bushes) {
      if (originBush == null) {
        continue;
      }

      originBush.updateTurnFlows(getLoading().getCurrentFlowAcceptanceFactors());
    }
  }

  /**
   * Verify if solution is flow proportional
   * 
   * @param gapEpsilon to use
   * @return true when flow proportional, false otherwise
   */
  private boolean isSolutionFlowEntropyMaximised(double gapEpsilon) {
    StringBuilder remainingPassToMaximiseEntropy = new StringBuilder("PASs not at max entropy: \n");
    boolean entryFound = false;
    for (var pas : this.equalFlowDistributedPass) {
      entryFound = true;
      remainingPassToMaximiseEntropy.append("PAS - ");
      remainingPassToMaximiseEntropy.append(pas.toString());
      remainingPassToMaximiseEntropy.append("\n");
    }
    if (entryFound && getSettings().isDetailedLogging()) {
      LOGGER.info(remainingPassToMaximiseEntropy.toString());
      return false;
    }

    return true;
  }

  /**
   * Initialise the origin sLTM bush by including the DAGs for each origin-destination. While adding make sure the labelling is consistent with the adopted labelling strategy.
   * single path but comprises multiple (implicit) paths, we split the OD demand proportionally
   * <p>
   * Add the edge segments to the bush and update the turn sending flow accordingly.
   * 
   * @param originBush                  to use
   * @param destination                 to use
   * @param originDestinationDemandPcuH to use
   * @param destinationDAG              to use
   */
  protected abstract void initialiseBushForDestination(final OriginBush originBush, final OdZone destination, final Double originDestinationDemandPcuH,
      final ACyclicSubGraph destinationDAG);

  /**
   * Initialise bushes. Find shortest bush for each origin and add the links, flow, and destination labelling to the bush
   * 
   * @param linkSegmentCosts costs to use
   * @throws PlanItException thrown when error
   */
  protected void initialiseBushes(final double[] linkSegmentCosts) throws PlanItException {
    final var shortestBushAlgorithm = createNetworkShortestBushAlgo(linkSegmentCosts);

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands();
    for (var origin : zoning.getOdZones()) {
      ShortestBushResult oneToAllResult = null;
      OriginBush originBush = null;
      for (var destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          if (originBush == null) {
            /* register new bush */
            originBush = new OriginBush(getIdGroupingToken(), origin, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
            bushes[(int) origin.getOdZoneId()] = originBush;
          }

          /* find one-to-all shortest paths */
          if (oneToAllResult == null) {
            oneToAllResult = shortestBushAlgorithm.executeOneToAll(origin.getCentroid());
          }

          /* initialise bush with this destination shortest path */
          var destinationDag = oneToAllResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), destination.getCentroid());
          initialiseBushForDestination(originBush, destination, currOdDemand, destinationDag);
        }

      }

      if (originBush != null && getSettings().isDetailedLogging()) {
        LOGGER.info(originBush.toString());
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @param pas      to create flow shift executor for
   * @param settings to use
   * @return created executor
   */
  protected abstract PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings);

  /**
   * Create a network wide shortest bush algorithm based on provided costs
   * 
   * @param linkSegmentCosts to use
   * @return one-to-all shortest bush algorithm
   */
  protected OneToAllShortestBushAlgorithm createNetworkShortestBushAlgo(final double[] linkSegmentCosts) {
    final int numberOfEdgeSegments = getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new OneToAllShortestBushAlgorithmImpl(linkSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Create a network wide shortest path algorithm based on provided costs
   * 
   * @param linkSegmentCosts to use
   * @return one-to-all shortest path algorithm
   */
  protected OneToAllShortestPathAlgorithm createNetworkShortestPathAlgo(final double[] linkSegmentCosts) {
    final int numberOfEdgeSegments = getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new DijkstraShortestPathAlgorithm(linkSegmentCosts, numberOfEdgeSegments, numberOfVertices);
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
   * 
   * @param initialLinkSegmentCosts costs to use
   */
  @Override
  public void createInitialSolution(double[] initialLinkSegmentCosts) {
    try {
      /* delegate to concrete implementation */
      initialiseBushes(initialLinkSegmentCosts);

      getLoading().setBushes(bushes);
      getLoading().setPasManager(this.pasManager);

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
   * @param taComponents          to use for access to user configured assignment components
   */
  public StaticLtmBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
    this.bushes = new OriginBush[transportModelNetwork.getZoning().getOdZones().size()];
    this.pasManager = new PasManager();
    this.pasManager.setDetailedLogging(settings.isDetailedLogging());
    this.equalFlowDistributedPass = new HashSet<>();
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
   * @param theMode to use
   * @param costsToUpdate to place updated costs in (output)
   * @param iterationIndex we're at
   * @return true when iteration could be successfully completed, false otherwise
   */
  @Override
  public boolean performIteration(final Mode theMode, double[] costsToUpdate, int iterationIndex) {
    try {
      
      /* 1 - NETWORK LOADING - UPDATE ALPHAS - USE BUSH SPLITTING RATES (i-1) -  MODE AGNOSTIC FOR NOW */
      {
        executeNetworkLoading();
      }
             
      /* 2 - NETWORK COST UPDATE + UPDATE NETWORK REALISED COST GAP */
      {
        boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
        this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);
        
        /* PAS COST UPDATE*/
        pasManager.updateCosts(costsToUpdate);      
                          
        LOGGER.info(String.format("** ALPHA: %s", Arrays.toString(getLoading().getCurrentFlowAcceptanceFactors())));
        LOGGER.info(String.format("** COSTS: %s", Arrays.toString(costsToUpdate)));
        LOGGER.info(String.format("** INFLOW: %s", Arrays.toString(getLoading().getCurrentInflowsPcuH())));
        LOGGER.info(String.format("** OUTFLOW: %s", Arrays.toString(getLoading().getCurrentOutflowsPcuH())));
      }
      
      /* 3 - BUSH LOADING - SYNC BUSH TURN FLOWS - USE NETWORK LOADING ALPHAS - MODE AGNOSTIC FOR NOW */
      {
        syncBushTurnFlows(); 
      }
      
      /* 4 - BUSH ROUTE CHOICE - UPDATE BUSH SPLITTING RATES - SHIFT BUSH TURN FLOWS - MODE AGNOSTIC FOR NOW */     
      {
        /* (NEW) PAS MATCHING FOR BUSHES */
        Collection<Pas> newPass = updateBushPass(costsToUpdate);            
              
        /* PAS/BUSH FLOW SHIFTS + GAP UPDATE */
        Collection<Pas> updatedPass = shiftFlows(theMode);      
        
        if(getSettings().isDetailedLogging()) {
          var newUsedPass = new ArrayList<Pas>(newPass);
          newUsedPass.retainAll(updatedPass);
          newUsedPass.forEach( p -> LOGGER.info(String.format("Created new PAS and applied flow shift on it: %s", p.toString()))); 
        }
        /* Remove unused new PASs, in case no flow shift is applied due to overlap with PAS with higher reduced cost 
         * In this case, the new PAS is not used and is to be removed identical to how existing PASs are removed during flow shifts when they no longer carry flow*/
        newPass.removeAll(updatedPass);
        newPass.forEach( pas -> pasManager.removePas(pas, false));
      }
      
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration");
      if(getSettings().isDetailedLogging()) {
        e.printStackTrace();
      }
      return false;
    }
    return true;
  }

  /**
   * Unlike the default convergence check, we also see if the solution is proportional if relevant; in a bush setting with a triangular fundamental diagram we do not obtain a
   * unique solution if a PAS has equal cost with an equal derivative but unequal flow distribution along its two segments, e.g. in free flow conditions we expect equal flow along
   * both alternatives if equal cost. When the settings indicate so, we verify if the solution is proportional or not and only if so we indicate convergence has been reached.
   * 
   * @param gapFunction    to use for regular convergence check on cost
   * @param iterationIndex at hand
   * @return true when converged, false otherwise
   * 
   */
  @Override
  public boolean hasConverged(GapFunction gapFunction, int iterationIndex) {
    // TODO Auto-generated method stub
    boolean converged = super.hasConverged(gapFunction, iterationIndex);
    if (converged && getSettings().isEnforceMaxEntropyFlowSolution()) {
      converged = isSolutionFlowEntropyMaximised(gapFunction.getStopCriterion().getEpsilon());
      if(!converged) {
        LOGGER.info("cost convergence: yes - yet one or more PASs flow distribution is not entropy maximised - overall convergence: no");
      }
    }
    return converged;
  } 

}
