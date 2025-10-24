package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathGeneralised;
import org.goplanit.assignment.SimulationData;
import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.assignment.common.pas.*;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmSettings;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmSimulationData;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.virtual.SteadyStateConnectoidTravelTimeCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Quadruple;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.goplanit.assignment.common.pas.PasUtils.createConjugatePasGroups;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
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
  flowShiftingStepOneCreatePasFlowShiftersWithLoadingS1S2SendingFlows() {
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
  private int flowShiftingStepTwoDeactivatePassWithoutRemainingFlow(
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
      if (p1Gap > p2Gap) {
        return -1;
      } else if (p1Gap < p2Gap) {
        return 1;
      } else {
        return 0;
      }
    };
    return PAS_GAP_COMPARATOR;
  }

  public Comparator<PasGroup<V,ES>> getPasGroupMaxGapComparator(Map<Pas<V,ES>, Double> pasGaps){
    Comparator<PasGroup<V,ES>> PASGROUP_MAXGAP_COMPARATOR = (g1, g2) -> {
      double pg1MaxGap = g1.getPass().stream().mapToDouble(pasGaps::get).max().getAsDouble();
      double pg2MaxGap = g2.getPass().stream().mapToDouble(pasGaps::get).max().getAsDouble();
      if (pg1MaxGap > pg2MaxGap) {
        return -1;
      } else if (pg1MaxGap < pg2MaxGap) {
        return 1;
      } else {
        return 0;
      }
    };
    return PASGROUP_MAXGAP_COMPARATOR;
  }

  protected List<PasGroup<V,ES>> flowShiftingStepFourOrderPasGroups(
      Collection<PasGroup<V,ES>> pasGroups,
      Map<Pas<V,ES>, Double> pasGaps) {
    var pascomparator = getPasGapComparator(pasGaps);
    var pasGroupComparator = getPasGroupMaxGapComparator(pasGaps);

    var sortedList = new ArrayList<>(pasGroups);
    sortedList.sort(pasGroupComparator);
    return sortedList;
  }

  /**
   * FLOW SHIFTING - STEP4: Create Sorted list of PASs in desired order to perform flow shifts (high to low) based
   * on relevant criterion.
   * todo: provide option for sorting order...
   * @param pasExecutors                      to use for retrieving PAS information used in sorting
   * @param pasGaps current gaps of PASs
   * @return sorted PASs in descending order of importance
   */
  protected List<Pas<V,ES>> flowShiftingStepFourOrderPass(
      Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      Map<Pas<V,ES>, Double> pasGaps) {

    pasGaps.entrySet().stream().sorted(Map.Entry.comparingByValue()).skip(pasGaps.entrySet().size()-Math.min(pasGaps.size(), 10)).forEach(
        e -> LOGGER.info(String.format("%.10f - %s", e.getValue(), e.getKey())));

    //var chosenComparator = PAS_NORMALISED_REDUCED_COST_BY_FLOW_COMPARATOR;
    var chosenComparator = getPasGapComparator(pasGaps);
    /* Sort all remaining PAss based on comparator */
    var result = this.pasManager.getActivePassSortedByReducedCost(chosenComparator);
    return result;
  }

  protected abstract Collection<PasGroup<V,ES>> createPasGroups(Collection<Pas<V,ES>> pass);

  /**
   * FLOW_SHIFTING : Do this for all but the UNCONGESTED PASs as they have already been processed in full
   *
   * @param theMode              to use
   * @param pasExecutors         flow shift executors for each PAS
   * @param originalNetworkCosts to use
   * @param simulationData       for debugging
   * @return list of PASs with shifted flows, and PASs with no flow remaining (the latter may also be listed as flow
   * shifted since, after the shift it may be that it has no more flow left, then it appears in both lists)
   */
  private Pair<Set<Pas<V,ES>>, Set<Pas<V,ES>>> doCongestedFlowShiftingV1(
          Mode theMode,
          Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
          double[] nlConsistentFlowAcceptanceFactors,
          double[] originalNetworkCosts,
          StaticLtmSimulationData simulationData) {

    var flowShiftedPass = new TreeSet<Pas<V,ES>>();
    var passWithoutBush = new TreeSet<Pas<V,ES>>();

    // todo very ugly, refactor
    boolean isConjugateApproach = pasExecutors.values().stream().findAny().isPresent() &&
        pasExecutors.values().stream().findAny().get() instanceof PasFlowShiftConjugateDestinationBasedExecutor;
    double[] conjSegmentCosts;
    if(isConjugateApproach){
      conjSegmentCosts =
          ((StaticLtmConjugateBushStrategy)this).expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
              theMode, originalNetworkCosts, true);
    } else {
      conjSegmentCosts = null;
    }

    double totalCongestedFlowShifted = 0;
    double maxPasReducedCost = 0;
    Pas<?,?> maxReducedCostPas = null;
    double totalPasReducedCost = 0;
    int numConsideredPas = 0;

    double startingAveragePasGap = -1;

    minNetworkGapAsThreshold = Math.min(minNetworkGapAsThreshold, getGapFunction().getGap());
    int MAX_ITERATIONS = ((int) -Math.log10(minNetworkGapAsThreshold)+1) * 4;
    int iteration = 1;
    do {

      if(!passWithoutBush.isEmpty()){
        pasExecutors.entrySet().removeAll(passWithoutBush);
        passWithoutBush.forEach(p -> pasManager.deactivatePas(p, true));
      }

      // sync costs as order matters
      pasExecutors.keySet().stream().filter(p -> p.getStatus()!=PasStatus.UNCONGESTED_WITH_SHIFT).forEach(
          p ->p.updateCost(conjSegmentCosts));

      var pasGaps = computePasGaps(pasExecutors.keySet(), nlConsistentFlowAcceptanceFactors);
      var sortedPass = flowShiftingStepFourOrderPass(pasExecutors, pasGaps);
      Collections.reverse(sortedPass);

      // debugging
      boolean logAll = false; //simulationData.getIterationIndex()>=50;

      LOGGER.info(String.format("--- NEXT CONGESTED PASs INTERNAL ITERATION %d ----", iteration));

      int congestedPasCounter = 0;
      long numCongestedPass = pasExecutors.keySet().stream().filter(
          p -> p.getStatus()!=PasStatus.UNCONGESTED_WITH_SHIFT || !p.hasRegisteredBushes()).count();
      double perPasPercentageOfTotal = 1.0/numCongestedPass;

      for (var pas : sortedPass) {
         // ignore uncongested PASs or PASs not on bush
        if (pas.getStatus() == PasStatus.UNCONGESTED_WITH_SHIFT || !pas.hasRegisteredBushes()) {
          if(!pas.hasRegisteredBushes()){
            passWithoutBush.add(pas);
          }
          continue;
        }

        ++congestedPasCounter;

        // run from 1% exponential decay to 100% of most important PAS
        double importanceSmoothingFactor =
            Math.min(1, (1 - (Math.pow(0.01, (congestedPasCounter * perPasPercentageOfTotal))) + 0.01));
        var pasFlowShifter = pasExecutors.get(pas);

        if (!(pasFlowShifter.getS2SendingFlow() > 0) || !pas.hasRegisteredBushes()) { // todo: this piece of code is duplication from line 166 -> consolidate
          /* PAS is redundant, no more flow remaining (for example due to flow shifts on other PASs with initial
           * overlapping S2 segments */
          pas.removeAllRegisteredBushes();
          passWithoutBush.add(pas);
          continue;
        }

        if (iteration==1){
          if (pas.getReducedCost() > maxPasReducedCost) {
            maxPasReducedCost = pas.getReducedCost();
            maxReducedCostPas = pas;
          }
          totalPasReducedCost += pas.getReducedCost();
          ++numConsideredPas;
        }

        // equilibrated --> needs pas cost update because change of alphas and flows may impact low/high cost
        var pasFlowShiftByRefTurn =
            pasFlowShifter.performEquilibratedCongestedFlowShifts(
                theMode,
                this,
                originalNetworkCosts,
                conjSegmentCosts,
                nlConsistentFlowAcceptanceFactors,
                getBushes(),
                logAll,
                PasFlowShiftExecutor.FlowShiftSmoothingApproach.NORMAL,
                (1.0/(Math.pow(iteration,0.66))) * importanceSmoothingFactor);

        double pasFlowShifted = Math.abs(pasFlowShiftByRefTurn.second());
        if (pasFlowShifted > 0) {
          totalCongestedFlowShifted += pasFlowShifted;
          //++pasCounter;

          if(iteration==1) {
            flowShiftedPass.add(pas);
          }

          /* when s2 no longer used on any bush - mark PAS for overall removal */
          if (!pas.hasRegisteredBushes()) {
            passWithoutBush.add(pas);
            continue;
          }

          if (logAll) {
            LOGGER.info(String.format("   pas flow shifted: %.10f", pasFlowShifted));
            if(congestedPasCounter > 5) {
              // do only first 5 PASs
              logAll = false;
            }
          }
        }
      }

      iteration++;
    }while(iteration <= MAX_ITERATIONS);

    LOGGER.info(String.format("TOTAL CONGESTED FLOW SHIFTED: %.10f", totalCongestedFlowShifted));
    LOGGER.info(String.format("MAX PAS COST DELTA: %.10f", maxPasReducedCost));
    LOGGER.info(String.format("PAS of MAX COST DELTA: %s", maxReducedCostPas));
    LOGGER.info(String.format("AVERAGE PAS COST DELTA: %.10f", totalPasReducedCost/numConsideredPas));

    return Pair.of(flowShiftedPass, passWithoutBush);
  }

  /**
   * Finalise flow shifting by deregistering bushes and PAs that have no more flow on their S2 alternatives
   * after finalising all shifts
   *
   * @param passWithoutBush to consider
   */
  private void flowShiftingStepEightFinalise(Set<Pas<V,ES>> passWithoutBush) {
    if (!passWithoutBush.isEmpty()) {
      passWithoutBush.forEach((pas) -> this.pasManager.deactivatePas(pas, getSettings().isDetailedLogging()));
    }

    int numRemovedPASs = passWithoutBush.size();
    if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format(
              "Deactivated %d PASs that were found to have no remaining flow on their high cost segment - After flow shifting", numRemovedPASs));
    }
  }

  /**
   * Create PAS executors for each active PAS, deactivate PASs without flow remaining
   *
   * @param theMode to use
   * @param simulationData to use
   * @return pas executors for each pas
   */
  private Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> prepareForFlowShifts(
      final Mode theMode, final StaticLtmSimulationData simulationData){
    
    // STEP 1: PAS original sending flows per alternative
    final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors =
        flowShiftingStepOneCreatePasFlowShiftersWithLoadingS1S2SendingFlows();

    // STEP2: Based on current NL flows, if we have any PASs without any S2 flow, deregister bushes, remove pas
    // from manager, and remove from flow shift executors as they are no longer relevant
    flowShiftingStepTwoDeactivatePassWithoutRemainingFlow(pasExecutors);

    return pasExecutors;
  }

  /**
   * Shift flows based on the registered PASs and their bushes.
   *
   * @param theMode              to use
   * @param pasExecutors         to use
   * @param originalNetworkCosts to use and update
   * @param simulationData       to use
   * @return all PASs where non-zero flow was shifted on
   */
  private Collection<Pas<V,ES>> performFlowShifts(
      final Mode theMode,
      final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] originalNetworkCosts,
      final StaticLtmSimulationData simulationData) {

    if(pasExecutors.isEmpty())
    {
      return Collections.emptyList();
    }

    // Capture original alphas, so we can use minimum of those and updated alphas to determine
    // available sub path sending flow with the most restrictive ensuring we are not shifting too much flow
    // as it is possible node model updates increased alphas making it seem more flow can be shifted when in reality
    // doing so can cause zero flow links in the middle of a PAS (which we want to avoid as it causes issues with
    // splitting rates at the end)
    var nlConsistentFlowAcceptanceFactors =
        Arrays.copyOf(
            getLoading().getCurrentFlowAcceptanceFactors(),getLoading().getCurrentFlowAcceptanceFactors().length);

    hookBeforePasUpdate(pasExecutors.values());

    Set<Pas<V,ES>> flowShiftedPass = new TreeSet<>();
    Set<Pas<V,ES>> passWithoutBush = new TreeSet<>();

//      // V1 approach - if V2 works deprecate and remove, or apply in first x iterations before switching to v2
//      // UNCONGESTED ONLY
//      var flowShiftedAndObsoletePass = doUncongestedFlowShiftingV1(
//          theMode, sortedPass, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);
//      flowShiftedPass = flowShiftedAndObsoletePass.first();
//      passWithoutBush = flowShiftedAndObsoletePass.second();
//
//      //updatedPass.forEach( p -> LOGGER.info("Updated PAS: " + p.toString()));
//      LOGGER.info(String.format("%.2f%% Uncongested Flow shifts performed: %d ---- [#Uncongested PASs without remaining flows %d)]",
//          ((double)flowShiftedPass.size()*100.0)/sortedPass.size(), flowShiftedPass.size(), passWithoutBush.size()));

    // Perform flow shifts for CONGESTED AND BECOMING CONGESTED WITH SHIFT PASs
    var flowShiftedAndObsoletePass = doCongestedFlowShiftingV1(
        theMode, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);

    LOGGER.info(String.format("%.2f%% Congested Flow shifts performed: %d ---- [#Uncongested PASs without remaining flows %d)]",
        ((double)flowShiftedAndObsoletePass.first().size()*100.0)/pasExecutors.size(), flowShiftedAndObsoletePass.first().size(), flowShiftedAndObsoletePass.second().size()));

    flowShiftedPass.addAll(flowShiftedAndObsoletePass.first());
    passWithoutBush.addAll(flowShiftedAndObsoletePass.second());

    // STEP8 : Finalise
    // dispose of PASs that no longer have S2 flows
    flowShiftingStepEightFinalise(passWithoutBush);

    return flowShiftedPass;
  }

  /**
   * Allow implementations to do prep before we enter loop of per congested PAS update, e.g.
   * initialise some across PAS tracking information for example
   *
   * @param pasExecutors used
   */
  protected abstract void hookBeforePasUpdate(Collection<PasFlowShiftExecutor<V, ES>> pasExecutors);

  protected Pair<Set<Pas<V,ES>>, Set<Pas<V,ES>>> doUncongestedFlowShiftingV1(
      Mode theMode, Collection<Pas<V,ES>> sortedPass,
      Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts, StaticLtmSimulationData simulationData) {
    // stub implementation (not implemented for regular destination based yet
    return Pair.of(null, null);
  }

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
   * Based on the network loading results, update the bush' turn sending flows
   */
  public void syncBushFlowsToNetworkFlows() {
    syncBushFlowsToNetworkFlows(null);
  }

  /**
   * Based on the network loading results, update the bush' turn sending flows
   *
   * @param nodesToSync if null all nodes are synced, otherwise selection only
   */
  public void syncBushFlowsToNetworkFlows(Set<DirectedVertex> nodesToSync) {
    for (var bush : bushes) {
      if (bush == null) {
        continue;
      }

      bush.syncToNetworkFlows(getLoading().getCurrentFlowAcceptanceFactors(), nodesToSync);
    }
  }

  /**
   * Update the PASs for bushes given the network costs and current bushes DAGs
   *
   * @param mode             to use
   * @param linkSegmentCosts to use
   * @param updateGap        flag
   * @param simulationData   to use
   * @param logAll           flag
   * @return newly created PASs and existing PAss with newly assigned bushes
   */
  protected abstract Map<Long,Pas<V,ES>> updateBushPass(
      Mode mode, final double[] linkSegmentCosts, boolean updateGap, StaticLtmSimulationData simulationData, boolean logAll);

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
        LOGGER.warning(String.format("Bush for root zone (%s) could not be initialised, likely due to lack of connectivity " +
                "as a destination, discard", bush.getRootZone().getIdsAsString()));
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
   * To avoid bushes keeping low flow links occupied and limiting options to use links or opposite links
   * more efficiently, we will remove very low flow links from each bush, implicitly shifting this flow to
   * higher usage branches.
   *
   * @param flowThreshold         any links with flow below this threshold will be implictly branch shifted
   * @param flowAcceptanceFactors to use
   */
  @SuppressWarnings("unchecked")
  protected void performLowFlowBushBranchShifts(double flowThreshold, double[] flowAcceptanceFactors) {
    int numShifts = 0;
    Map<ES, Set<B>> removedSegmentsForBushes = new TreeMap<>();
    for (B bush : bushes) {

      if(bush == null){
        continue;
      }

      var removedEdgeSegments =
              bush.performLowFlowBranchShifts(
                      flowThreshold, flowAcceptanceFactors, isDestinationTrackedForLogging(bush));
      if(removedEdgeSegments==null || removedEdgeSegments.isEmpty()){
        continue;
      }

      ++numShifts;
      // signal that this bush should be removed from any PAS that utilises the removed edge segment on S1/S2
      for(var removedSegment : removedEdgeSegments){
        removedSegmentsForBushes.computeIfAbsent(removedSegment, es -> new TreeSet<>()).add(bush);
      }
    }

    // deregister bushes from PASs that have edge segments that were removed for that bush as a result of the branch shift
    this.pasManager.forEachActivePas(pas -> {
      for(var removedSegmentsEntry : removedSegmentsForBushes.entrySet()){
        if(pas.getRegisteredBushes().stream().noneMatch(b -> removedSegmentsEntry.getValue().contains(b))){
          continue;
        }
        // potential for removing one or more bushes but only if removed segment(s) overlap with the PAS
        if(pas.containsEdgeSegment(removedSegmentsEntry.getKey())){
          // overlap found, remove bush from PAS since it is no longer valid
          pas.removeBushes((Collection<RootedBush<V, ES>>) removedSegmentsEntry.getValue());
        }
      }
    });

    if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format("Performed %d low flow branch shifts", numShifts));
    }
  }

  protected void executeCostUpdateAfterLoading(
      Mode theMode, double[] costsToUpdate, boolean doLoadingAllFlowUpdatePriorToCostUpdate) {
    // revert to always updating ALL link costs. This is simple and we know that we are using the right costs for the
    // gap calculation.
    boolean updateOnlyPotentiallyBlockingNodeCosts = false;
//        boolean updateOnlyPotentiallyBlockingNodeCosts = isUpdateOnlyPotentiallyBlockingNodeCosts();
//        if(simulationData.isFirstIteration() && updateOnlyPotentiallyBlockingNodeCosts && simulationData.isInitialCostsAppliedInFirstIteration(theMode)){
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
    /* PAS STATUS UPDATE (used to truncate search for PASs on bush) */
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
      e.printStackTrace();
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
        executeCostUpdateAfterLoading(theMode, costsToUpdate, true);
      }

      /* 3 - BUSH LOADING - SYNC BUSH TURN FLOWS - USE NETWORK LOADING ALPHAS - MODE AGNOSTIC FOR NOW */
      {
        syncBushFlowsToNetworkFlows();
      }

      // todo: UGLY
      if(this instanceof StaticLtmConjugateBushStrategy){
        StaticLtmConjugateBushStrategy conjStrat = (StaticLtmConjugateBushStrategy) this;
        if(conjStrat.PERSIST_WARM_START_TO_DISK_TURN_FLOW_ITERATION == simulationData.getIterationIndex()){
          LOGGER.info("PERSISTING BUSH DATA FOR FUTURE WARM START PURPOSES TO "+conjStrat.WARM_START_LOCATION);
          conjStrat.persistBushDataForWarmStart();
        }
      }

      /* 4 - BUSH ROUTE CHOICE - UPDATE BUSH SPLITTING RATES - SHIFT BUSH TURN FLOWS - MODE AGNOSTIC FOR NOW */
      {
        // debugging
        boolean logAll = false; //simulationData.getIterationIndex()>=200;

        /* (NEW) PAS MATCHING FOR BUSHES */
        boolean updateGap = true; // todo consider computing gap directly after determining costs?
        var passToConsider = updateBushPass(theMode, costsToUpdate, updateGap, simulationData, logAll);
        if(getSettings().isDetailedLogging()) {
          LOGGER.info(String.format("Newly added PASs: %d (active: %d))",
                  passToConsider.size(), pasManager.getNumberOfActivePass()));
        }

        /* PAS/BUSH FLOW SHIFTS + GAP UPDATE */
        {
          // STEP1 + STEP2 (flow shifters + deactivate unused PASs)
          final Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors =
              prepareForFlowShifts(theMode, simulationData);

          /* UNCONGESTED/CONGESTED FLOW SHIFTS (considering proposed shift) */
          Collection<Pas<V,ES>> updatedPass = performFlowShifts(
              theMode, pasExecutors, costsToUpdate, simulationData);

          LOGGER.info(String.format("Flow shifts performed: %d (%.2f%% of all pass)",
              updatedPass.size(),((double)updatedPass.size()*100)/pasManager.getNumberOfActivePass()));
        }

      }

      /* 5 - perform low flow branch shifts on the bush level */
//      {
//        performLowFlowBushBranchShifts(0.001, getLoading().getCurrentFlowAcceptanceFactors());
//      }

      
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
          OdSkimSubOutputType odSkimOutputType, Mode mode, StaticLtmSimulationData iterationData) {
    LOGGER.warning(String.format("OD Skim matrix support not yet available in %s for type %s and mode (%s)",
            this.getClass().getCanonicalName(), odSkimOutputType, mode.getIdsAsString()));

    // for time being use empty skim matrix
    var emptySkimMatrix = new OdSkimMatrix(getTransportNetwork().getZoning().getOdZones(), odSkimOutputType);
    return emptySkimMatrix;
  }

}
