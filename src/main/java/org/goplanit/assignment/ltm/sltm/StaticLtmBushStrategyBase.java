package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathGeneralised;
import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.assignment.common.pas.*;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmSimulationData;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.virtual.SteadyStateConnectoidTravelTimeCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.zoning.zonetozone.OdDemands;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;

import java.util.*;
import java.util.logging.Logger;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 * @param <ES> type of segment
 * @param <V> type of vertex
 * @param <B> type of bush
 */
public abstract class
StaticLtmBushStrategyBase<V extends DirectedVertex, ES extends EdgeSegment, B extends RootedBush<V, ES>>
        extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategyBase.class.getCanonicalName());

  public double minNetworkGapAsThreshold = 1;

  /**
   * tracked bushes (with non-zero demand)
   */
  private TreeSet<B> bushes;

  /**
   * FLOW SHIFTING - STEP 1: PAS original sending flows per alternative:
   * 1) create executor and
   * 2) prep flow shifting to allow for ordering based on PAS flows and then construct proposed flow shifts based
   * on these network loading consistent PAS sending flows
   * @return PAS flow shifters with network loading s1 s2 sending flows initialised
   */
  private Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>>
  createPasFlowShiftersWithLoadingS1S2SendingFlows() {
    final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors = new HashMap<>();
    this.pasManager.forEachActivePas(pas -> {

      // create flow shifter
      var pasFlowShifter = createPasFlowShiftExecutor(pas, getSettings());

      // determine PAS alternative s1 and s2 sending flows
      pasFlowShifter.stepOneDetermineNetworkLoadingConsistentS1S2SendingFlows(
              getLoading().getCurrentFlowAcceptanceFactors());

      // register for further processing
      pasExecutors.put(pas, pasFlowShifter);
    });
    return pasExecutors;
  }

  /**
   * FLOW SHIFTING - STEP2: Based on current NL flows, if we have any PASs without any S2 flow, deregister bushes,
   * remove pas from manager, and remove from flow shift executors as they are no longer relevant
   *
   * @param pasExecutors to update and check for
   * @return number of removed PASs due to no remaining flow on s2 alternative
   */
  private int deactivatePassWithoutRemainingFlow(
          Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors) {
    var passWithoutBush = new ArrayList<Pas<V,ES>>();
    this.pasManager.forEachActivePas(pas -> {

      var pasFlowShifter = pasExecutors.get(pas);

      /* PAS is redundant, no more flow remaining --> mark for removal */
      if (!(pasFlowShifter.getS2SendingFlow() > 0) || !pas.hasRegisteredBushes()) {
        pas.removeAllRegisteredBushes();
        passWithoutBush.add(pas);
      }

    });

    // remove from pas manager and pas flow shift executors
    if (!passWithoutBush.isEmpty()) {
      passWithoutBush.forEach((pas) -> {
        this.pasManager.deactivatePas(pas, getSettings().isDetailedLogging());
        pasExecutors.remove(pas);
      });

    }

    int numRemovedPASs = passWithoutBush.size();
    if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format(
              "Deactivated %d PASs that were found to have no remaining flow on their high cost segment - Before flow shifting", numRemovedPASs));
    }
    return numRemovedPASs;
  }

  public Map<Pas<V,ES>, Double> computePasGaps(Collection<Pas<V,ES>> pass,
                                               double[] nlConsistentFlowAcceptanceFactors){
    Map<Pas<V,ES>, Double> pasGaps = new HashMap<>();
    for(var pas : pass){
      double s1Flow = PasFlowShiftConjugateDestinationBasedExecutor.determinePasSubPathSendingFlow(
          pas, true, getLoading().getCurrentFlowAcceptanceFactors(),nlConsistentFlowAcceptanceFactors);
      double s2Flow = PasFlowShiftConjugateDestinationBasedExecutor.determinePasSubPathSendingFlow(
          pas, false, getLoading().getCurrentFlowAcceptanceFactors(),nlConsistentFlowAcceptanceFactors);
      var gap = pas.computeGap(s1Flow, s2Flow);
      pasGaps.put(pas, gap);
    }
    return pasGaps;
  }

  public Comparator<Pas<V,ES>> getPasGapComparator(Map<Pas<V,ES>, Double> pasGaps){
    Comparator<Pas<V,ES>> PAS_GAP_COMPARATOR = (p1, p2) -> {
      double p1Gap = pasGaps.get(p1);
      double p2Gap = pasGaps.get(p2);
      return Double.compare(p2Gap, p1Gap);
    };
    return PAS_GAP_COMPARATOR;
  }

  /**
   * FLOW SHIFTING - STEP4: Create Sorted list of PASs in desired order to perform flow shifts (high to low) based
   * on relevant criterion.
   * todo: provide option for sorting order...
   * @param pasGaps current gaps of PASs
   * @return sorted PASs in descending order of importance
   */
  protected List<Pas<V,ES>> orderPass(Map<Pas<V,ES>, Double> pasGaps) {

    pasGaps.entrySet().stream().sorted(Map.Entry.comparingByValue()).skip(
        pasGaps.entrySet().size()-Math.min(pasGaps.size(), 10)).forEach(
        e -> LOGGER.info(String.format("%.10f - %s", e.getValue(), e.getKey())));

    var chosenComparator = getPasGapComparator(pasGaps);
    /* Sort all remaining PAss based on comparator */
    return this.pasManager.getActivePassSortedByReducedCost(chosenComparator);
  }

  /**
   * Log how many flow shifts were performed and deregister bushes and PAs that have no more flow on their S2
   * alternatives
   */
  private void logFlowShiftPerformedAndDeregisterEmptyPass() {

    var passWithoutBush = new ArrayList<Pas<V,ES>>(100);
    this.pasManager.forEachActivePas(p -> {
      if(!p.hasRegisteredBushes()){
        passWithoutBush.add(p);
      }
    });
    passWithoutBush.forEach((pas) -> this.pasManager.deactivatePas(pas, getSettings().isDetailedLogging()));
    int numRemovedPASs = passWithoutBush.size();
    if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format(
              "Deactivated %d PASs that were found to have no remaining flow on their high cost segment " +
                  "- After flow shifting", numRemovedPASs));
    }
  }

  /**
   * Create PAS executors for each active PAS, deactivate PASs without flow remaining
   *
   * @return pas executors for each pas
   */
  private Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> prepareForFlowShifts(){
    
    // Create dedicated executors for flow shifting per PAS
    final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors = createPasFlowShiftersWithLoadingS1S2SendingFlows();

    // Based on current network loading flows, if we have any PASs without any S2 flow, deregister bushes, remove pas
    // from manager, and remove from flow shift executors as they are no longer relevant
    // todo: when recreating PASs from scratch this step is redundant as only PASs with positive S2 flows are created
    deactivatePassWithoutRemainingFlow(pasExecutors);

    return pasExecutors;
  }

  /**
   * Shift flows based on the registered PASs and their bushes.
   *
   * @param theMode              to use
   * @param pasExecutors         to use
   * @param originalNetworkCosts to use and update
   * @param simulationData       to use
   * @return collection with all PASs where non-zero flow was shifted on
   */
  protected abstract Collection<Pas<V,ES>> performFlowShifts(
      final Mode theMode,
      final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] originalNetworkCosts,
      final StaticLtmSimulationData simulationData);

  /**
   * track all unique PASs
   */
  protected final PasManager<V,ES> pasManager;

  /**
   * access to bushes
   *
   * @return bushes
   */
  public Set<B> getBushes(){
    return bushes;
  }

  protected boolean isDestinationTrackedForLogging(B bush) {
    return getSettings().hasTrackOdsForLogging() &&
        getSettings().isTrackDestinationForLogging((OdZone) bush.getRootZoneVertex().getParent().getParentZone());
  }

  /**
   * Based on the network loading results, update the bush's turn sending flows
   */
  public void syncBushFlowsToNetworkFlows() {
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
   * @param simulationData   to use
   * @param logAll           flag
   * @return newly created PASs and existing PAss with newly assigned bushes
   */
  protected abstract Map<Long,Pas<V,ES>> updateBushPassAndGap(
      Mode mode,
      final double[] linkSegmentCosts,
      StaticLtmSimulationData simulationData,
      boolean logAll);

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
  protected StaticLtmBushStrategyBase(
      final IdGroupingToken idGroupingToken,
      long assignmentId,
      final TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportModelNetwork,
      final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents,
      boolean registerPassByDiverge) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
    this.pasManager = new PasManager<>(registerPassByDiverge);
    this.pasManager.setDetailedLogging(settings.isDetailedLogging());
  }

  /**
   * Let derived implementations create the empty bushes as desired before populating them
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected abstract TreeSet<B> createEmptyBushes(Mode mode);

  /**
   * Initialise the sLTM bush by including the relevant DAGs based on available demand and bush layout.
   * <p>
   * Add the edge segments to the bush and update the turn sending flow accordingly.
   * </p>
   *
   * @param bush                  to use
   * @param zoning                to use
   * @param odDemands             to use
   * @param shortestTreeAlgorithm to use
   * @return true when successful, false when bush could not be initialised
   */
  protected abstract boolean initialiseBush(
      B bush, Zoning zoning, OdDemands odDemands, ShortestPathGeneralised shortestTreeAlgorithm);

  /**
   * {@inheritDoc}
   *
   * @param pas      to create flow shift executor for
   * @param settings to use
   * @return created executor
   */
  protected abstract PasFlowShiftExecutor<V,ES> createPasFlowShiftExecutor(
          final Pas<V,ES> pas, final StaticLtmSettings settings);

  /**
   * Initialise bushes. Find shortest bush for each origin and add the links, flow, and destination labelling to
   * the bush
   *
   * @param mode             to use
   * @param linkSegmentCosts costs to use
   */
  protected void initialiseBushes(Mode mode, final double[] linkSegmentCosts){
    final var shortestTreeAlgorithm = createInitialNetworkShortestSearchTreeAlgo(mode, linkSegmentCosts);

    Set<B> invalidBushesToRemove = new TreeSet<>();
    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands(mode);
    for (B bush : bushes) {
      if (bush == null) {
        continue;
      }
      boolean validBush = initialiseBush(bush, zoning, odDemands, shortestTreeAlgorithm);
      if(!validBush){
        LOGGER.warning(String.format("Bush for root zone (%s) could not be initialised, likely due to lack of " +
            "connectivity as a destination, discard", bush.getRootZone().getIdsAsString()));
        invalidBushesToRemove.add(bush);
        continue;
      }

      if (isDestinationTrackedForLogging(bush) || getSettings().isDetailedLogging()) {
        LOGGER.info(bush.toString());
      }
    }
    invalidBushesToRemove.forEach(b -> bushes.remove(b));
  }

  /**
   * Create a network wide shortest search tree algorithm based on provided costs
   *
   * @param theMode          to use
   * @param linkSegmentCosts to use
   * @return one-to-all shortest tree search algorithm
   */
  protected abstract ShortestPathGeneralised createInitialNetworkShortestSearchTreeAlgo(
          Mode theMode, final double[] linkSegmentCosts);

  /**
   * Create a network wide Dijkstra shortest path algorithm based on provided costs
   *
   * @param linkSegmentCosts to use
   * @return Dijkstra shortest path algorithm
   */
  protected abstract ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] linkSegmentCosts);

  /**
   * Update all existing PASs costs based on provided original network link segment costs
   *
   * @param theMode                         the mode to use
   * @param originalNetworkLinkSegmentCosts to use
   */
  protected abstract void updatePasCosts(Mode theMode, double[] originalNetworkLinkSegmentCosts);

  /**
   * Update all existing PASs status based on current state of network without considering
   * any information on proposed flow shifts, determine congested or uncongested (without flow shift).
   * All PASs get assigned
   * todo: optimisation could be to not do this for inactive PASs, but then we have to do this on the fly
   *  when a PAS changes from inactive to active.
   *
   * @param theMode                                 the mode to use
   * @param networkLinkSegmentFlowAcceptanceFactors to determine if a link segment is congested or not
   */
  protected abstract void updatePasStatusBeforeFlowShifts(
          Mode theMode, double[] networkLinkSegmentFlowAcceptanceFactors);

  /**
   * Update costs on original network links as well as for all PASs.
   *
   * @param theMode                                 to use
   * @param costsToUpdate                           to costs to be updated on this raw array
   * @param doLoadingAllFlowUpdatePriorToCostUpdate flag
   */
  protected void executeCostUpdateAfterLoading(
      Mode theMode, double[] costsToUpdate, boolean doLoadingAllFlowUpdatePriorToCostUpdate) {
    // revert to always updating ALL link costs. This is simple and we know that we are using the right costs for the
    // gap calculation.
    boolean updateOnlyPotentiallyBlockingNodeCosts = false;
//        boolean updateOnlyPotentiallyBlockingNodeCosts = isUpdateOnlyPotentiallyBlockingNodeCosts();
//        if(updateOnlyPotentiallyBlockingNodeCosts && simulationData.isFirstIteration() && simulationData.isInitialCostsAppliedInFirstIteration(theMode)){
//          /* initial costs will be inconsistent with loading performed in first iteration, recalculate all link segment costs for free flow conditions first
//           * and then for those that need tracking override with flow based costs */
//          CostUtils.populateModalFreeFlowPhysicalLinkSegmentCosts(
//                  theMode, getInfrastructureNetwork().getLayerByMode(theMode).getLinkSegments(), costsToUpdate);
//        }
    this.executeNetworkCostsUpdate(
        theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate, doLoadingAllFlowUpdatePriorToCostUpdate);

    // todo below is not strictly needed anymore since pas costs are always updated on the fly and
    //  congested and uncongested pass are currently not separated out
    /* PAS COST UPDATE */
    updatePasCosts(theMode, costsToUpdate);
    /* PAS STATUS UPDATE (used to optimize flow shifts calcs, no longer used but leave status calc for now) */
    updatePasStatusBeforeFlowShifts(theMode, this.getLoading().getCurrentFlowAcceptanceFactors());

    // DEBUGGING
    if(getSettings().isDetailedLogging()) {
      logCongestedSegmentInfo(costsToUpdate, theMode);
    }
  }

  /**
   * Create bush based network loading implementation
   *
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected abstract StaticLtmLoadingBushBase<B> createNetworkLoading();

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
      if (this.bushes == null || this.bushes.isEmpty()) {
        this.bushes = createEmptyBushes(mode);
      }
      initialiseBushes(mode, initialLinkSegmentCosts);

      /* update loading with information */
      getLoading().setBushes(bushes);
      getLoading().setPasManager(this.pasManager);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("Unable to create initial bushes for sLTM %d", getAssignmentId()));
    }
  }

  //@formatter:off
  /**
   * Perform an iteration by:
   *  <ol>
   * <li>Conducting a loading to obtain network costs</li>
   * <li>sync bush flows to network flows based on acceptance factors found in loading</li>
   * <li>Identify new PASs and shift flow from affected bushes</li>
   * </ol>
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
        executeCostUpdateAfterLoading(theMode, costsToUpdate, true);
      }

      /* 3 - BUSH LOADING - SYNC BUSH TURN FLOWS - USE NETWORK LOADING ALPHAS - MODE AGNOSTIC FOR NOW */
      {
        syncBushFlowsToNetworkFlows();
      }

      /* 4 - BUSH ROUTE CHOICE - UPDATE BUSH SPLITTING RATES - SHIFT BUSH TURN FLOWS - MODE AGNOSTIC FOR NOW */
      {
        // debugging
        boolean logAll = false;

        /* (NEW) PAS MATCHING FOR BUSHES  + GAP calc */
        var passToConsider = updateBushPassAndGap(theMode, costsToUpdate, simulationData, logAll);
        if(getSettings().isDetailedLogging()) {
          LOGGER.info(String.format("Newly added PASs: %d (active: %d))",
                  passToConsider.size(), pasManager.getNumberOfActivePass()));
        }

        /* DO FLOW SHIFTS  */
        {
          // code for flow shifting in dedicated executor instances. Create them here, one for each PAS.
          Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors = prepareForFlowShifts( );
          // execute actual flow shifting per PAS
          performFlowShifts(theMode, pasExecutors, costsToUpdate, simulationData);
          // Dispose of PASs that no longer have S2 flows
          logFlowShiftPerformedAndDeregisterEmptyPass();
        }

      }

    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration, print stack trace when enabling detailed logging");
      if(getSettings().isDetailedLogging()){
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
    return super.hasConverged(gapFunction, iterationIndex);
  }

  /**
   * {@inheritDoc}
   *
   * Bush-based based assignment check
   */
  @Override
  public void verifyComponentCompatibility() {
    super.verifyComponentCompatibility();


    var gapFunction = getTrafficAssignmentComponent(GapFunction.class);
    /* gap function check */
    PlanItRunTimeException.throwIf(!(gapFunction instanceof PathBasedGapFunction),
            "%s bush based Static LTM currently requires PAS compatible PathBasedRelative gap function, but found %s", gapFunction.getClass().getCanonicalName());

    var virtualCost = getVirtualCost();
    /* virtual cost check */
    PlanItRunTimeException.throwIf(!(virtualCost instanceof SteadyStateConnectoidTravelTimeCost),
        "%s bush based Static LTM currently requires SteadyStateVirtualCost so queues on connectors can provide a meaningful derivative for equilibration", virtualCost.getClass().getCanonicalName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdSkimMatrix createOdSkimMatrix(
          SkimSubOutputType odSkimOutputType, Mode mode, StaticLtmSimulationData iterationData) {
    LOGGER.warning(String.format("OD Skim matrix support not yet available in %s for type %s and mode (%s)",
            this.getClass().getCanonicalName(), odSkimOutputType, mode.getIdsAsString()));

    // for time being use empty skim matrix
    return new OdSkimMatrix(getTransportNetwork().getZoning().getOdZones(), odSkimOutputType);
  }

}
