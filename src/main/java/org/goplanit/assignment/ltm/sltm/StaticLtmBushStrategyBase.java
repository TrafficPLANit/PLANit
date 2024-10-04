package org.goplanit.assignment.ltm.sltm;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.CostUtils;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public abstract class StaticLtmBushStrategyBase<B extends RootedBush<?, ?>> extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategyBase.class.getCanonicalName());

  /**
   * Knowing which edge segments no longer have flow for the given bushes, we must deregister all these bushes from
   * any other PASs on which they reside that also utilise these link segments as it is no longer possible to traverse
   * them on the bush with non-zero flow.
   *
   * @param bushRemovedLinkSegments to consider
   */
  private void deregisterBushesWithRemovedSegmentsFromMatchingPass(
          Map<EdgeSegment, Set<RootedLabelledBush>> bushRemovedLinkSegments) {

    for(var entry : bushRemovedLinkSegments.entrySet()){
      // check if any edge segment of pas is matching with the link segment removed from the bush
      Predicate<Pas> pasPredicate = p ->
              p.anyMatch(es -> es.idEquals(entry.getKey()), true) ||
                      p.anyMatch(es -> es.idEquals(entry.getKey()), false);;
      for(var bush : entry.getValue()){
        pasManager.removeBushFromPasIf(bush, pasPredicate);
      }
    }
  }

  /**
   * If a bush has added link segments due to shifted flows then we must remove this bush from all other
   * PASs that 1) have this bush registered AND 2) have been NEWLY added in this iteration ONLY IF the new
   * PAS would introduce a cycle based on the newly added link segments. This is what is checked here and if found
   * to cause a cycle then deregistration of the bush from the new PAS is performed immediately.
   *
   * <p>
   *   Existing PASs need not checking because
   *   this PAS that added the segments has been vetted against those implicitly by checking the original bush.
   *   (can only occur with overlapping pas update)
   * </p>
   *
   * @param bushAddedLinkSegments to consider
   * @param newPass that need to be checked
   */
  private void deregisterBushesWithAddedSegmentsFromNewPassCausingCycles(
      Map<EdgeSegment, Set<RootedLabelledBush>> bushAddedLinkSegments, Collection<Pas> newPass) {
    if(newPass == null || newPass.isEmpty()){
      return;
    }

    // lambda to see if an edge segment of a pas alternative has any overlapping vertices with the
    // bushAddedLinkSegments provided. If so it yields true
    final Predicate<EdgeSegment> anyVertexMatches = es ->
            bushAddedLinkSegments.keySet().stream().anyMatch(
                esAdded -> esAdded.hasAnyVertex(es.getUpstreamVertex(), es.getDownstreamVertex()));

    // only consider new PASs that have overlapping vertices and on which any of the bushes is registered before
    // attempting to...
    final var pasAlternativeTypes = List.of(true, false); // low/high cost
    for(var newPas : newPass){
      Set<B> toBeDeregisteredBushes = null;
      for(var alternativeType : pasAlternativeTypes){

        if(!newPas.anyMatch(anyVertexMatches, alternativeType)){
          continue;
        }
        var pasBushes = newPas.getRegisteredBushes();
        for(var pasBush : pasBushes){

          if(pasBush.determineIntroduceCycle(newPas.getAlternative(alternativeType))==null){
            // no cycle, it is fine do nothing
            continue;
          }

          // CYCLE! if we were to consider this new PAS, deregister bush since another new PASs added segments make it
          //        invalid to for now on this bush
          if(isDestinationTrackedForLogging((B)pasBush)){
            LOGGER.info(String.format("Deregistering bush (%s) from new PAS (%s) due to cycle introducing aspects of " +
                            "recently processed PAS",
                    pasBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(), newPas));
          }

          if(toBeDeregisteredBushes == null){
            toBeDeregisteredBushes = new TreeSet<>();
          }
          toBeDeregisteredBushes.add((B)pasBush);
        }
      }
      if(toBeDeregisteredBushes!=null){
        toBeDeregisteredBushes.forEach(b -> newPas.removeBush((RootedLabelledBush) b));
      }
    }

  }

  /**
   * Shift flows based on the registered PASs and their origins.
   *
   * @param theMode        to use
   * @param simulationData to use
   * @param newAndUpdatedPass only used to deregister bush from new/updated Pass pre-emptively in case we flag a cycle
   *                          if it were to be used due to the current PAS adding link segments to the bush as a result
   *                          of the flow shift
   * @return all PASs where non-zero flow was shifted on
   */
  private Collection<Pas> shiftFlows(
          final Mode theMode,
          final StaticLtmSimulationData simulationData,
          final Pair<Collection<Pas>,Collection<Pas>> newAndUpdatedPass) {
    var flowShiftedPass = new ArrayList<Pas>((int) this.pasManager.getNumberOfPass());
    var passWithoutBush = new ArrayList<Pas>();

    var networkLoading = getLoading();
    var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);
    var physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    var virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);

    // track remaining new or bush-updated PASs that have not been processed (only used when overlapping PAS updates are allowed to
    // minimise the on-the-fly checking required for possible cycle introducing conflicts due to overlapping PAS updates)
    final Map<Long, Pas> unprocessedNewOrUpdatedPass = new TreeMap<>();
    newAndUpdatedPass.<Collection<Pas>>both(c -> c.forEach(p -> unprocessedNewOrUpdatedPass.put(p.pasId, p)));

    // STEP 1: PAS original sending flows per alternative
    // prep flow shifting to allow for ordering based on PAS flows and then construct proposed flow shifts based
    // on these network loading consistent PAS sending flows
    final Map<Pas, PasFlowShiftExecutor> pasExecutors = new HashMap<>();
    this.pasManager.forEachPas( pas -> {
            var pasFlowShifter = createPasFlowShiftExecutor(pas, getSettings());
            pasFlowShifter.updateS1S2EntrySendingFlows();
            pasExecutors.put(pas, pasFlowShifter);
    });

    // STEP2: determine the proposed flow shift for each PAS as if it were performing
    //  its flow shift in isolation + update remaining gap based on current PAS flows (before shifts) and costs
    final Map<Pas, Map<EdgeSegment, Double>> pasProposedFlowShifts = new HashMap<>();
    this.pasManager.forEachPas( pas -> {
      var pasFlowShifter = pasExecutors.get(pas);

      if (!(pasFlowShifter.getS2SendingFlow() > 0) || !pas.hasRegisteredBushes()) {
        /* PAS is redundant, no more flow remaining (for example due to flow shifts on other PASs with initial
         * overlapping S2 segments) */
        pas.removeAllRegisteredBushes();
        passWithoutBush.add(pas);
        return;
      }

      updateGap(gapFunction, pas, pasFlowShifter.getS1SendingFlow(), pasFlowShifter.getS2SendingFlow());

      Map<EdgeSegment, Double> flowShifts = pasFlowShifter.determineProposedFlowShiftByEntrySegment(
          theMode, physicalCost, virtualCost, networkLoading);
      pasProposedFlowShifts.put(pas, flowShifts);
    });
    if (!passWithoutBush.isEmpty()) {
      passWithoutBush.forEach((pas) -> this.pasManager.removePas(pas, getSettings().isDetailedLogging()));
    }
    passWithoutBush.clear();

    // flow based comparator
    final Comparator<Pas> PAS_REDUCED_COST_BY_FLOW_COMPARATOR = (p1, p2) -> {
      double p1Cost = p1.getNormalisedReducedCost() * pasExecutors.get(p1).getS2SendingFlow();
      double p2Cost = p2.getNormalisedReducedCost() * pasExecutors.get(p2).getS2SendingFlow();
      if (Precision.greater(p1Cost, p2Cost, Precision.EPSILON_15)) {
        return -1;
      } else if (Precision.smaller(p1Cost, p2Cost, Precision.EPSILON_15)) {
        return 1;
      } else {
        return 0;
      }
    };

    /* STEP 3: Sort all remaining PAss based on comparator */
    Collection<EdgeSegment> linkSegmentsUsed = new HashSet<>(100);
    Collection<Pas> sortedPass = this.pasManager.getPassSortedByReducedCost(PAS_REDUCED_COST_BY_FLOW_COMPARATOR);

    Pas prevProcessedPas = null;
    for (Pas pas : sortedPass) {

      // do here because some process flows get cut-off in loop, so only in next entry we can finalise previous
      if(prevProcessedPas!=null) {
        unprocessedNewOrUpdatedPass.remove(prevProcessedPas.pasId);
      }
      prevProcessedPas = pas;

      var pasFlowShifter = pasExecutors.get(pas);

      if(!getSettings().isAllowOverlappingPasUpdate())
      {
        // todo: should probably also check on entry segments to avoid overlap or cycles, this is not yet done!

        /* cannot do overlapping PASs without network loading update, so skip those for now */
        if (pas.containsAny(linkSegmentsUsed)) {
          continue;
        }
        /* cannot do PASs that have conflicting (opposite) direction links compared to earlier processed PASs in this loop,
         * this can happen if multiple PASs were identified for the first time as potentially eligible for a bush, but
         * they contain opposing link segments, if one has been applied, then the next triggers this check and we should
         * skip it */
        if (pas.containsAnyOppositeDirection(linkSegmentsUsed)) {
          // if the opposite direction is present on the bush due to earlier pas shift, then do not execute, if none of the
          // bushes overlap with the previous pas that was applied that contained the opposite direction then we can
          // still safely proceed
          // todo the above explained portion of this check is not yet implemented, but could improve convergence per loading, now we are very conservative by
          //  always skipping even if bushes between the two PASs are not overlapping at all
          continue;
        }
      }

      if (!(pasFlowShifter.getS2SendingFlow() > 0) || !pas.hasRegisteredBushes()) { // todo: this piece of code is duplication from line 166 -> consolidate
        /* PAS is redundant, no more flow remaining (for example due to flow shifts on other PASs with initial
         * overlapping S2 segments, or cycles were found as a result causing deregistering of all bushes on the PAS) */
        pas.removeAllRegisteredBushes();
        passWithoutBush.add(pas);
        continue;
      }

      /* untouched PAS (no flows shifted yet) in this iteration */
      boolean pasFlowShifted = pasFlowShifter.run(
          pasProposedFlowShifts.get(pas), theMode, physicalCost, virtualCost, networkLoading, getSmoothing());
      if (pasFlowShifted) {
        flowShiftedPass.add(pas);

        /* s1 */
        pas.forEachEdgeSegment(true /* low cost */, linkSegmentsUsed::add);
        /* s2 */
        pas.forEachEdgeSegment(false /* high cost */, linkSegmentsUsed::add);

        /* when s2 no longer used on any bush - mark PAS for overall removal */
        if (!pas.hasRegisteredBushes()) {
          passWithoutBush.add(pas);
        }

        /* If due to flow shifting some bushes have removed edges due to zero flow remaining
         * then we must remove these bushes from other pass that 1) have this bush registered, and 2) have
         * the link segment present that no longer has any flow on the bush. */
         if(pasFlowShifter.hasAnyBushRemovedLinkSegments()){
           Map<EdgeSegment, Set<RootedLabelledBush>> bushRemovedLinkSegments = pasFlowShifter.getBushRemovedLinkSegments();
           deregisterBushesWithRemovedSegmentsFromMatchingPass(bushRemovedLinkSegments);
         }
         /* conversely if a bush has added link segments due to shifted flows then we must remove this bush from all other
          * PASs that 1) have this bush registered 2) have been NEWLY added in this iteration. Existing PASs are fine because
          * this PAS that added the segments has been vetted against those implicitly by checking the original bush.
          * (can only occur with overlapping pas update) */
        if(pasFlowShifter.hasAnyBushAddedLinkSegments() && getSettings().isAllowOverlappingPasUpdate()){
          Map<EdgeSegment, Set<RootedLabelledBush>> bushAddedLinkSegments = pasFlowShifter.getBushAddedLinkSegments();
          deregisterBushesWithAddedSegmentsFromNewPassCausingCycles(bushAddedLinkSegments, unprocessedNewOrUpdatedPass.values());
        }
      }
    }

    if (!passWithoutBush.isEmpty()) {
      passWithoutBush.forEach((pas) -> this.pasManager.removePas(pas, getSettings().isDetailedLogging()));
    }

    return flowShiftedPass;
  }

  /**
   * tracked bushes (with non-zero demand)
   */
  protected B[] bushes;

  /**
   * track all unique PASs
   */
  protected final PasManager pasManager;

  protected boolean isDestinationTrackedForLogging(B bush) {
    return getSettings().hasTrackOdsForLogging() &&
        getSettings().isTrackDestinationForLogging((OdZone) bush.getRootZoneVertex().getParent().getParentZone());
  }

  /**
   * Update gap. modified path based gap function where we update the GAP based on PAS cost discrepancy.
   * This is due to the impossibility of efficiently determining the network and
   * minimum path costs in a capacity constrained bush based setting. Instead we:
   * <p>
   * minimumCost PAS : s1 cost * SUM(s1 sending flow, s2 sending flow), measuredAbsoluteCostGap PAS: s2 sending flow * (s2 cost - s1 cost)
   * <p>
   * Sum the above over all PASs. Note that PASs can (partially) overlap, so the measured cost does likely not add up to the network cost
   *
   * @param gapFunction   to use
   * @param pas           to compute for
   * @param s1SendingFlow of the PAS s1 segment
   * @param s2SendingFlow of the PAS s2 segment
   */
  protected void updateGap(final PathBasedGapFunction gapFunction, final Pas pas, double s1SendingFlow, double s2SendingFlow) {
    gapFunction.increaseMinimumPathCosts(pas.getAlternativeLowCost(), (s1SendingFlow + s2SendingFlow));
    gapFunction.increaseAbsolutePathGap(pas.getAlternativeHighCost(),  s2SendingFlow, pas.getAlternativeLowCost());
  }

  /**
   * Based on the network loading results, update the bush' turn sending flows
   */
  protected void syncBushFlowsToNetworkFlows() {
    for (var bush : bushes) {
      if (bush == null) {
        continue;
      }

      bush.syncToNetworkFlows(getLoading().getCurrentFlowAcceptanceFactors());
    }
  }

  /**
   * Update the PASs for bushes given the network costs and current bushes DAGs
   *
   * @param mode             to use
   * @param linkSegmentCosts to use
   * @return newly created PASs and exiting PAss with newly assigned bushes
   */
  protected abstract Pair<Collection<Pas>, Collection<Pas>> updateBushPass(Mode mode, final double[] linkSegmentCosts);

  /**
   * Constructor
   *
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  protected StaticLtmBushStrategyBase(
      final IdGroupingToken idGroupingToken,
      long assignmentId,
      final TransportModelNetwork transportModelNetwork,
      final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    /*
     * destination based bushes are inverted, so PASs are to be registered based on vertex farthest from root, i.e, farthest from destination, so at the upstream point of the PAS
     * at its diverge
     */
    boolean registerPassByDiverge = settings.getSltmType() == StaticLtmType.DESTINATION_BUSH_BASED;
    this.pasManager = new PasManager(registerPassByDiverge);

    this.pasManager.setDetailedLogging(settings.isDetailedLogging());
  }

  /**
   * Let derived implementations create the empty bushes as desired before populating them
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected abstract B[] createEmptyBushes(Mode mode);

  /**
   * Initialise the sLTM bush by including the relevant DAGs based on available demand and bush layout. When equal costs are found between alternative paths OD demand is to be
   * split proportionally
   * <p>
   * Add the edge segments to the bush and update the turn sending flow accordingly.
   *
   * @param bush                  to use
   * @param zoning                to use
   * @param odDemands             to use
   * @param shortestBushAlgorithm to use
   */
  protected abstract void initialiseBush(B bush, Zoning zoning, OdDemands odDemands, ShortestBushGeneralised shortestBushAlgorithm);

  /**
   * {@inheritDoc}
   *
   * @param pas      to create flow shift executor for
   * @param settings to use
   * @return created executor
   */
  protected abstract PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings);

  /**
   * Initialise bushes. Find shortest bush for each origin and add the links, flow, and destination labelling to the bush
   *
   * @param mode             to use
   * @param linkSegmentCosts costs to use
   */
  protected void initialiseBushes(Mode mode, final double[] linkSegmentCosts){
    final var shortestBushAlgorithm = createNetworkShortestBushAlgo(linkSegmentCosts);

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands(mode);
    for (B bush : bushes) {
      if (bush == null) {
        continue;
      }
      initialiseBush(bush, zoning, odDemands, shortestBushAlgorithm);

      if (isDestinationTrackedForLogging(bush)) {
        LOGGER.info(bush.toString());
      }
    }
    LOGGER.info(String.format("Initialised with %d PASs", pasManager.getNumberOfPass()));
    LOGGER.info("TODO: Consider removing initialisation with PASs as it is not complete and side effect of initialisation");
  }

  /**
   * Create a network wide shortest bush algorithm based on provided costs
   *
   * @param linkSegmentCosts to use
   * @return one-to-all shortest bush algorithm
   */
  protected ShortestBushGeneralised createNetworkShortestBushAlgo(final double[] linkSegmentCosts) {
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new ShortestBushGeneralised(linkSegmentCosts, numberOfVertices);
  }

  /**
   * Create a network wide Dijkstra shortest path algorithm based on provided costs
   *
   * @param linkSegmentCosts to use
   * @return Dijkstra shortest path algorithm
   */
  protected ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] linkSegmentCosts) {
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new ShortestPathDijkstra(linkSegmentCosts, numberOfVertices);
  }

  /**
   * Create bush based network loading implementation
   *
   * @param segmentPair2MovementMap mapping from entry/exit segment (dual key) to movement, use to covert turn flows
   *                                to splitting rate data format
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected abstract StaticLtmLoadingBushBase<B> createNetworkLoading(MultiKeyMap<Object, Movement> segmentPair2MovementMap);

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  protected StaticLtmLoadingBushBase<B> getLoading() {
    return (StaticLtmLoadingBushBase<B>) super.getLoading();
  }

  /**
   * {@inheritDoc}
   * Create initial bushes, where for each origin the bush is initialised with the shortest path only
   */
  @Override
  public void createInitialSolution(Mode mode, OdZones odZones, double[] initialLinkSegmentCosts, int iterationIndex) {
    try {

      /* delegate to concrete implementation */
      if (this.bushes == null || this.bushes.length == 0) {
        this.bushes = createEmptyBushes(mode);
      }
      initialiseBushes(mode, initialLinkSegmentCosts);

      /* update loading with information */
      getLoading().setBushes(bushes);
      getLoading().setPasManager(this.pasManager);

    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to create initial bushes for sLTM %d", getAssignmentId()));
    }
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
   * @param prevCosts the previously used costs from the previous iteration
   * @param costsToUpdate to place updated costs in (output)
   * @param simulationData tracking relevant simulation information for the strategy
   * @return true when iteration could be successfully completed, false otherwise
   */
  @Override
  public boolean performIteration(
          final Mode theMode,
          final double[] prevCosts,
          double[] costsToUpdate,
          final StaticLtmSimulationData simulationData) {
    try {
      
      /* 1 - NETWORK LOADING - UPDATE ALPHAS - USE BUSH SPLITTING RATES (i-1) -  MODE AGNOSTIC FOR NOW */
      {
        executeNetworkLoading(theMode);
      }
             
      /* 2 - NETWORK COST UPDATE + UPDATE NETWORK REALISED COST GAP */
      {
        boolean updateOnlyPotentiallyBlockingNodeCosts = isUpdateOnlyPotentiallyBlockingNodeCosts();
        if(simulationData.isFirstIteration() && updateOnlyPotentiallyBlockingNodeCosts && simulationData.isInitialCostsAppliedInFirstIteration(theMode)){
          /* initial costs will be inconsistent with loading performed in first iteration, recalculate all link segment costs for free flow conditions first
           * and then for those that need tracking override with flow based costs */
          CostUtils.populateModalFreeFlowPhysicalLinkSegmentCosts(
                  theMode, getInfrastructureNetwork().getLayerByMode(theMode).getLinkSegments(), costsToUpdate);
        }
        this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);
        
        /* PAS COST UPDATE*/
        pasManager.updateCosts(costsToUpdate);      

        if(getSettings().isDetailedLogging()){
          LOGGER.info(String.format("** ALPHA: %s", Arrays.toString(getLoading().getCurrentFlowAcceptanceFactors())));
          LOGGER.info(String.format("** COSTS: %s", Arrays.toString(costsToUpdate)));
          LOGGER.info(String.format("** INFLOW: %s", Arrays.toString(getLoading().getCurrentInflowsPcuH())));
          LOGGER.info(String.format("** OUTFLOW: %s", Arrays.toString(getLoading().getCurrentOutflowsPcuH())));
        }
      }
      
      /* 3 - BUSH LOADING - SYNC BUSH TURN FLOWS - USE NETWORK LOADING ALPHAS - MODE AGNOSTIC FOR NOW */
      {
        syncBushFlowsToNetworkFlows();
      }
      
      /* 4 - BUSH ROUTE CHOICE - UPDATE BUSH SPLITTING RATES - SHIFT BUSH TURN FLOWS - MODE AGNOSTIC FOR NOW */     
      {
        /* (NEW) PAS MATCHING FOR BUSHES */
        long numOriginalPass = pasManager.getNumberOfPass();
        var newAndUpdatedPass = updateBushPass(theMode, costsToUpdate);
        if(getSettings().isDetailedLogging()) {
          LOGGER.info(String.format("%d PASs known (including %d new and %d updated PASs)",
                  pasManager.getNumberOfPass(), newAndUpdatedPass.first().size(), newAndUpdatedPass.second().size()));
        }
              
        /* PAS/BUSH FLOW SHIFTS + GAP UPDATE */
        Collection<Pas> updatedPass = shiftFlows(theMode, simulationData, newAndUpdatedPass);

        var justNewPass = newAndUpdatedPass.first();
        var newPassWithShiftedFlows = new ArrayList<>(justNewPass);
        newPassWithShiftedFlows.retainAll(updatedPass);
        long remainingPass = pasManager.getNumberOfPass();
        LOGGER.info(String.format("Flow shifts performed: %d ---- [#PASs: before %d, after %d, newly added (with shifts): %d]",
            updatedPass.size(), numOriginalPass, remainingPass, newPassWithShiftedFlows.size()));

        /* Remove unused new PASs, in case no flow shift is applied due to overlap with PAS with higher reduced cost
         * In this case, the new PAS is not used and is to be removed identical to how existing PASs are removed during flow shifts when they no longer carry flow*/
        justNewPass.removeAll(updatedPass);
        justNewPass.forEach( pas -> pasManager.removePas(pas, getSettings().isDetailedLogging()));
      }
      
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration, print stack trace when enabling detailed logging");
      e.printStackTrace();
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
    boolean converged = super.hasConverged(gapFunction, iterationIndex);
    return converged;
  }

  /**
   * {@inheritDoc}
   *
   * Path-based based assignment requires a form of path choice if we do more than a single iteration
   */
  @Override
  public void verifyComponentCompatibility() {
    super.verifyComponentCompatibility();

    var gapFunction = getTrafficAssignmentComponent(GapFunction.class);

    /* gap function check */
    PlanItRunTimeException.throwIf(!(gapFunction instanceof PathBasedGapFunction),
            "%s bush based Static LTM currently requires PAS compatible PathBasedRelative gap function, but found %s", gapFunction.getClass().getCanonicalName());

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdSkimMatrix createOdSkimMatrix(OdSkimSubOutputType odSkimOutputType, Mode mode, StaticLtmSimulationData iterationData) {
    LOGGER.warning(String.format("OD Skim matrix support not yet available in %s for type % and mode (%s)",
            this.getClass().getCanonicalName(), odSkimOutputType, mode.getIdsAsString()));
    return null;
  }

}
