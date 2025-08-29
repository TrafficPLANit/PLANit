package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathGeneralised;
import org.goplanit.assignment.ltm.sltm.conjugate.PasFlowShiftConjugateDestinationBasedExecutor;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmConjugateBushStrategy;
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

  /**
   * tracked bushes (with non-zero demand)
   */
  private TreeSet<B> bushes;

  private void logCongestedSegmentInfo(double[] costs, Mode theMode) {
    List<String> idList = new ArrayList<>();
    List<Quadruple<Double, Double, Double,Double>> alphaCostInOutflowList = new ArrayList<>();
    var alphas = getLoading().getCurrentFlowAcceptanceFactors();
    for(var ls : getInfrastructureNetwork().getLayerByMode(theMode).getLinkSegments()){
      if(Precision.smaller(alphas[(int)ls.getId()], 1, Precision.EPSILON_9)){
        idList.add(ls.getParent().getIdsAsString());
        alphaCostInOutflowList.add(Quadruple.of(
            alphas[(int)ls.getId()],
            costs[(int)ls.getId()],
            getLoading().getCurrentInflowsPcuH()[(int)ls.getId()],
            getLoading().getCurrentOutflowsPcuH()[(int)ls.getId()]));
      }
    }
    for(var ls : getTransportNetwork().getVirtualNetwork().getLayer().getConnectoidSegments()){
      if(Precision.smaller(alphas[(int)ls.getId()], 1, Precision.EPSILON_9)){
        idList.add(ls.getIdsAsString());
        alphaCostInOutflowList.add(Quadruple.of(
            alphas[(int)ls.getParent().getId()],
            costs[(int)ls.getId()],
            getLoading().getCurrentInflowsPcuH()[(int)ls.getId()],
            getLoading().getCurrentOutflowsPcuH()[(int)ls.getId()]));
      }
    }
    for(int index =0 ; index<idList.size();++index){
      var quad = alphaCostInOutflowList.get(index);
      LOGGER.info(String.format("Congested Link (%s) - U: %.1f - V: %.1f - alpha: %.4f - cost: %.8f",
          idList.get(index), quad.third(), quad.fourth(), quad.first(), quad.second()));
    }
  }

  /**
   * Knowing which edge segments no longer have flow for the given bushes, we must deregister all these bushes from
   * any other PASs on which they reside that also utilise these link segments as it is no longer possible to traverse
   * them on the bush with non-zero flow.
   *
   * @param bushRemovedLinkSegments to consider
   */
  @Deprecated
  protected void unregisterBushesWithRemovedSegmentsFromMatchingPass(
      Map<ES, Set<RootedBush<V, ES>>> bushRemovedLinkSegments) {

    //todo: should no longer be needed, bushes should simply not find to have any flow anymore
    // so they will get skipped in flow shifting automatically

    for(var entry : bushRemovedLinkSegments.entrySet()){
      // check if any edge segment of pas is matching with the link segment removed from the bush
      Predicate<Pas<V,ES>> pasPredicate = p ->
              p.anyMatch(es -> es.idEquals(entry.getKey()), true) ||
                      p.anyMatch(es -> es.idEquals(entry.getKey()), false);;
      for(var bush : entry.getValue()){
        pasManager.removeBushFromActivePasIf(bush, pasPredicate, true);
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
   * Existing PASs need not checking because
   * this PAS that added the segments has been vetted against those implicitly by checking the original bush.
   * (can only occur with overlapping pas update)
   * </p>
   *
   * @param newPass that need to be checked
   * @return set of deregistered bushes
   */
  @SuppressWarnings("unchecked")
  private Set<B> deregisterBushesWithAddedSegmentsFromNewPassCausingCycles(Collection<Pas<V,ES>> newPass) {
    Set<B> allUnregisteredBushes = new TreeSet<>();
    if(newPass == null || newPass.isEmpty()){
      return allUnregisteredBushes;
    }

    // only consider new PASs that have overlapping vertices and on which any of the bushes is registered before
    // attempting to...
    final var pasAlternativeTypes = List.of(true, false); // low/high cost
    for(var newPas : newPass){
      Set<B> toBeUnregisteredBushes = null;
      for(var alternativeType : pasAlternativeTypes){
        Set<B> pasBushes = (Set<B>) newPas.getRegisteredBushes();
        for(B pasBush : pasBushes){
          var downCastBush = (RootedBush<DirectedVertex, EdgeSegment>) pasBush;
          if(downCastBush.determineIntroduceCycle(newPas.getAlternative(alternativeType))==null){
            // no cycle, it is fine do nothing
            continue;
          }

          // CYCLE! if we were to consider this new PAS, deregister bush since another new PASs added segments make it
          //        invalid to for now on this bush
          if(isDestinationTrackedForLogging(pasBush)){
            LOGGER.info(String.format("Unregistering bush (%s) from new PAS (%s) due to cycle introducing aspects of " +
                "recently processed PAS",
                pasBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString(), newPas));
          }

          if(toBeUnregisteredBushes == null){
            toBeUnregisteredBushes = new TreeSet<>();
          }
          toBeUnregisteredBushes.add(pasBush);
        }
      }
      if(toBeUnregisteredBushes!=null){
        toBeUnregisteredBushes.forEach(newPas::removeBush);
        if(allUnregisteredBushes.isEmpty()){
          allUnregisteredBushes = toBeUnregisteredBushes;
        }else{
          allUnregisteredBushes.addAll(toBeUnregisteredBushes);
        }
      }
    }

    return allUnregisteredBushes;
  }

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

  /**
   * FLOW SHIFTING - STEP4: Create Sorted list of PASs in desired order to perform flow shifts (high to low) based
   * on relevant criterion.
   *
   * @param pasExecutors                      to use for retrieving PAS information used in sorting
   * @param nlConsistentFlowAcceptanceFactors
   * @return sorted PASs in descending order of importance
   */
  protected Collection<Pas<V,ES>> flowShiftingStepFourOrderPass(
      Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors) {

    Map<Pas<V,ES>, Double> pasGaps = new HashMap<>();
    for(var exec : pasExecutors.entrySet()){
      var pas = exec.getKey();
      var executor = (PasFlowShiftConjugateDestinationBasedExecutor) exec.getValue(); // not sustainable
      double s1Flow = PasFlowShiftConjugateDestinationBasedExecutor.determinePasSubPathSendingFlow(
          pas, true, getLoading().getCurrentFlowAcceptanceFactors(),nlConsistentFlowAcceptanceFactors);
      double s2Flow = PasFlowShiftConjugateDestinationBasedExecutor.determinePasSubPathSendingFlow(
          pas, false, getLoading().getCurrentFlowAcceptanceFactors(),nlConsistentFlowAcceptanceFactors);
      pas.computeGap(s1Flow, s2Flow);
      pasGaps.put(pas, pas.computeGap(s1Flow, s2Flow));
    }

    pasGaps.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach( e ->
        LOGGER.info(String.format("%.10f - %s", e.getValue(), e.getKey())));

    // PAS GAP
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

    // normalised cost * flow based comparator
    Comparator<Pas<V,ES>> PAS_NORMALISED_REDUCED_COST_BY_FLOW_COMPARATOR = (p1, p2) -> {
      double p1Cost = p1.getNormalisedReducedCost() * pasExecutors.get(p1).getS2SendingFlow();
      double p2Cost = p2.getNormalisedReducedCost() * pasExecutors.get(p2).getS2SendingFlow();
      if (p1Cost > p2Cost) {
        return -1;
      } else if (p1Cost < p2Cost) {
        return 1;
      } else {
        return 0;
      }
    };
    PAS_NORMALISED_REDUCED_COST_BY_FLOW_COMPARATOR = PAS_NORMALISED_REDUCED_COST_BY_FLOW_COMPARATOR.reversed();

    // regular reduced cost * flow based comparator
    final Comparator<Pas<V,ES>> PAS_REDUCED_COST_BY_FLOW_COMPARATOR = (p1, p2) -> {
      double p1Cost = p1.getReducedCost() * pasExecutors.get(p1).getS2SendingFlow();
      double p2Cost = p2.getReducedCost() * pasExecutors.get(p2).getS2SendingFlow();
      if (p1Cost > p2Cost) {
        return -1;
      } else if (p1Cost < p2Cost) {
        return 1;
      } else {
        return 0;
      }
    };

    // normalised cost * flow based comparator
    Comparator<Pas<V,ES>> PAS_REDUCED_COST = (p1,p2) ->
        Double.compare(p1.getReducedCost(), p2.getReducedCost());
    //PAS_REDUCED_COST = PAS_REDUCED_COST.reversed();

    // flow based comparator
    Comparator<Pas<V,ES>> PAS_FLOW = (p1,p2) ->
        Double.compare(pasExecutors.get(p1).getS1SendingFlow() + pasExecutors.get(p1).getS2SendingFlow(),
            pasExecutors.get(p2).getS1SendingFlow() + pasExecutors.get(p2).getS2SendingFlow());
    PAS_FLOW = PAS_FLOW.reversed();

    //var chosenComparator = PAS_NORMALISED_REDUCED_COST_BY_FLOW_COMPARATOR;
    var chosenComparator = PAS_GAP_COMPARATOR;
    /* Sort all remaining PAss based on comparator */
    var result = this.pasManager.getActivePassSortedByReducedCost(chosenComparator);
    return result;
  }

  /**
   * STEP5: Any new PASs that were identified may be in conflict with each other regarding introducing cycles. Using
   * the established ordering, we verify if a conflict exists between such newly identified PASs (adding both would
   * introduce a cycle). If so, remove one of the two PASs from the affected bush where the higher priority new PAS
   * is kept and the lower priority one discarded (for such a bush)
   * <p>
   * Note that this check is needed, because during the creation of new PASs we can only verify if it would not
   * introduce a cycle in relation to the existing bush(es), if the s1 alternative would add segments to the bush this
   * is only established after shifts are performed. Also we do not know the ordering at that point so we can't
   * determine which new PAS is more favourable on a network level.
   * </p>
   */
  @Deprecated
  private void flowShiftingStepFiveRemoveConflictingNewPass(
          Collection<Pas<V,ES>> sortedPass,
          Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
          Pair<Collection<Pas<V,ES>>, Collection<Pas<V,ES>>> newAndUpdatedPass) {

//    // track remaining new or bush-updated PASs that have not been processed (only used when overlapping PAS updates
//    // are allowed to minimise the on-the-fly checking required for possible cycle introducing conflicts due to
//    // overlapping PAS updates)
//    final Map<Long, Pas<V,ES>> unprocessedNewOrUpdatedPassS2Update = new TreeMap<>();
//    newAndUpdatedPass.<Collection<Pas<V,ES>>>both(
//            c -> c.forEach(p -> unprocessedNewOrUpdatedPassS2Update.put(p.pasId, p)));

    // prune conflicting PASs for each bush based on which one is deemed more important
    Map<ES, Set<RootedBush<V,ES>>> s1MissingLinkSegments = new TreeMap<>();
    for (var pas : sortedPass) {
      var pasFlowShifter = pasExecutors.get(pas);

      /* If a bush is going to add link segments on S1 due flows being shifted from S2 then: we must
       * remove this bush from all other PASs that 1) have this bush registered 2) have been NEWLY added in this
       * iteration IF these other PASs would introduce a cycle given the newly added link segments on S1
       * identified here. Existing PASs are fine because this PAS that added the segments has been vetted against
       * those implicitly by checking the original bush. (can only occur with overlapping pas update)
       * todo: We perform this pruning BEFORE the actual flow shift at the moment because cycle detection still relies
       *  on topological ordering being up to date, doing it after S2 update and before S1 update is not possible anymore
       *  because bush may be temporarily invalid due to removed link segments. We also can't wait until after the S1 update
       *  because then some cycle introducing bushes would already have removed flow from S2. Therefore we do it here.
       *  Note: once we have replace cycle detection with a cost based approach we can move it to after S2 update again.
       *
       * */
      var pasS1MissingLinkSegmentsByBush = pasFlowShifter.findS1MissingLinkSegmentsByBush();
      if (getSettings().isAllowOverlappingPasUpdate() && pasS1MissingLinkSegmentsByBush != null &&
              !pasS1MissingLinkSegmentsByBush.isEmpty()) {

        var deregisteredBushes = deregisterBushesWithAddedSegmentsFromNewPassCausingCycles(List.of(pas));
        // remove identified missing S1 links from deregistered bushes to avoid these links being added (locally)...
        pasS1MissingLinkSegmentsByBush.forEach( (es, bushes) -> bushes.removeAll(deregisteredBushes));
        // ... supplement bushes with the missing links of this PAS (for now), to scope out any potential
        // cycles in upcoming PASs...
        pasS1MissingLinkSegmentsByBush.forEach(( es, bushes) -> bushes.forEach( b -> b.getDag().addEdgeSegment(es)));
        // ... track the added missing segments so they can be removed again after all ordered new PASs have been
        // checked since they have not actually been processed and these links are not yet really on those bushes
        pasS1MissingLinkSegmentsByBush.forEach((es,bushes) -> s1MissingLinkSegments.computeIfAbsent(
                es, e -> new TreeSet<>()).addAll(bushes));
      }
    }
    // remove all temporarily added link segments that were used for cycle detection as they do not carry any flow (yet)
    s1MissingLinkSegments.forEach( (es, bushes) -> bushes.forEach( b -> b.getDag().removeEdgeSegment(es)));
  }

  /**
   * FLOW_SHIFTING : Do this for all but the UNCONGESTED PASs as they have already been processed in full
   *
   * @param theMode              to use
   * @param sortedPass           list of sorted PASs in processing order
   * @param pasExecutors         flow shift executors for each PAS
   * @param originalNetworkCosts to use
   * @param simulationData       for debugging
   * @return list of PASs with shifted flows, and PASs with no flow remaining (the latter may also be listed as flow
   * shifted since, after the shift it may be that it has no more flow left, then it appears in both lists)
   */
  private Pair<Set<Pas<V,ES>>, Set<Pas<V,ES>>> doCongestedFlowShiftingV1(
          Mode theMode,
          Collection<Pas<V,ES>> sortedPass,
          Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
          double[] nlConsistentFlowAcceptanceFactors,
          double[] originalNetworkCosts,
          StaticLtmSimulationData simulationData) {

    // experiment where we (if no interactions and full convergence internally is assumed we visit each PAS once
    // however, if due to interactions some PASs bounce back, we revisit them prior to other less contributing PASs
    // as this approach is married to always process ONLY the currently worst PAS.
    boolean worstPasFirstUpdateOrderingPerPas = true;

    Collection<ES> linkSegmentsUsed = new HashSet<>(100);
    boolean smoothOverIterations = worstPasFirstUpdateOrderingPerPas? false : true;

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

    int MAX_ITERATIONS_ALLOWED = worstPasFirstUpdateOrderingPerPas ? sortedPass.size() : 1;
    int iteration = 1;
    boolean doNotStop = true;
    do {

      if(!passWithoutBush.isEmpty()){
        sortedPass.removeAll(passWithoutBush);
        passWithoutBush.forEach(pasExecutors::remove);
        passWithoutBush.forEach(p -> pasManager.deactivatePas(p, true));
      }

      // sync costs as order matters
      sortedPass.stream().filter(p -> p.getStatus()!=PasStatus.UNCONGESTED_WITH_SHIFT).forEach(
          p ->p.updateCost(conjSegmentCosts));
      var updatedOrder = flowShiftingStepFourOrderPass(pasExecutors, nlConsistentFlowAcceptanceFactors);
      sortedPass = updatedOrder;

//      sortedPass.stream().sorted(Comparator.comparingDouble(Pas::getReducedCost)).filter(
//          p -> p.getStatus()!=PasStatus.UNCONGESTED_WITH_SHIFT).map(p ->
//          String.format("%.10f - %s",p.getReducedCost(),p)).forEach(LOGGER::info);

      // debugging
      boolean logAll = simulationData.getIterationIndex()>=50;

      LOGGER.info(String.format("--- NEXT CONGESTED PASs INTERNAL ITERATION %d ----", iteration));

//      int MAX_PAS_UPDATES = Math.max(5,sortedPass.size()/10); // top 10% with minimum of 5 PASs
      int congestedPasCounter = 0;

      long numCongestedPass = sortedPass.stream().filter(
          p -> p.getStatus()!=PasStatus.UNCONGESTED_WITH_SHIFT).count();
      double perPasPercentageOfTotal = 1.0/numCongestedPass;

      for (var pas : sortedPass) {
        //double importanceSmoothingFactor = 1 - (pasCounter * perPasImportanceReduction);

        // ignore uncongested PASs or PASs not on bush
        if (pas.getStatus() == PasStatus.UNCONGESTED_WITH_SHIFT || !pas.hasRegisteredBushes()) {
          if(!pas.hasRegisteredBushes()){
            passWithoutBush.add(pas);
          }
          continue;
        }

        double importanceSmoothingFactor = 1;
        if(!worstPasFirstUpdateOrderingPerPas) { // NOTE: no decay when we go per PAS and the current is always the worst
          // we start with least important because it is sequentially run, so the last should not be overridden hence
          // it also gets the largest importance, so we invert the decay to accommodate
          importanceSmoothingFactor = (1 - Math.pow(0.1, (congestedPasCounter * perPasPercentageOfTotal))) + 0.1; // run from 100% exponential decay to 1% of leat important PAS
        }
        ++congestedPasCounter;

        if (pas.pasId == 74L) {
          int bla = 4;
        }

        var pasFlowShifter = pasExecutors.get(pas);

        /* cannot do overlapping PASs without network loading update, so skip those for now */
        if (!getSettings().isAllowOverlappingPasUpdate() && pas.containsAny(linkSegmentsUsed)) {
          continue;
        }

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

        // ORIGINAL ONE SHOT
        /* untouched PAS (no flows shifted yet) in this iteration */
        //      boolean pasFlowShifted = pasFlowShifter.performOneShotCongestedS2FlowShift(
        //          pasProposedFlowShifts.get(pas), theMode, getLoading(), getSmoothing(), logAll);


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
                smoothOverIterations? (1.0/(iteration+1)) * importanceSmoothingFactor : importanceSmoothingFactor);

        double pasFlowShifted = Math.abs(pasFlowShiftByRefTurn.second());
        if (pasFlowShifted > 0) {
          totalCongestedFlowShifted += pasFlowShifted;
          //++pasCounter;

          if(iteration==1) {
            flowShiftedPass.add(pas);
          }

          /* s1 */
          pas.forEachEdgeSegment(true /* low cost */, linkSegmentsUsed::add);
          /* s2 */
          pas.forEachEdgeSegment(false /* high cost */, linkSegmentsUsed::add);

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

//        if(pasCounter > MAX_PAS_UPDATES){
//          break;
//        }

        if(worstPasFirstUpdateOrderingPerPas){
          // immediately break so we can reorder
          break;
        }
      }

    }while(iteration++ < MAX_ITERATIONS_ALLOWED);

    LOGGER.info(String.format("TOTAL CONGESTED FLOW SHIFTED: %.10f", totalCongestedFlowShifted));
    LOGGER.info(String.format("MAX PAS COST DELTA: %.10f", maxPasReducedCost));
    LOGGER.info(String.format("PAS of MAX COST DELTA: %s", maxReducedCostPas));
    LOGGER.info(String.format("AVERAGE PAS COST DELTA: %.10f", totalPasReducedCost/numConsideredPas));

    return Pair.of(flowShiftedPass, passWithoutBush);
  }

  private Map<Pas<V, ES>,Pair<EdgeSegment,Double>> determineConvergenceBasedCongestedFlowShiftsV2(
      Mode theMode,
      Collection<Pas<V,ES>> sortedPass,
      Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      StaticLtmSimulationData simulationData) {

    var pasDesiredFlowShifts = new TreeMap<Pas<V,ES>,Pair<EdgeSegment,Double>>();

    // todo very ugly, refactor
    boolean isConjugateApproach = pasExecutors.values().stream().findAny().isPresent() &&
        pasExecutors.values().stream().findAny().get() instanceof PasFlowShiftConjugateDestinationBasedExecutor;
    double[] conjSegmentCosts = null;
    if(isConjugateApproach){
      conjSegmentCosts =
          ((StaticLtmConjugateBushStrategy)this).expandNonConjugateLinkSegmentCostToConjugateSegmentCost(
              theMode, originalNetworkCosts, true);
    }

    var updatedOrder = flowShiftingStepFourOrderPass(pasExecutors, nlConsistentFlowAcceptanceFactors);
    sortedPass = updatedOrder;

    // debugging
    boolean logAll = getSettings().isDetailedLogging() && simulationData.getIterationIndex()>=50;
    LOGGER.info("--- NEXT V2 CONGESTED PASs FIND SHIFT ----");

    for (var pas : sortedPass) {
      // ignore uncongested PASs or PASs not on bush
      if (pas.getStatus() == PasStatus.UNCONGESTED_WITH_SHIFT || !pas.hasRegisteredBushes()) {
        continue;
      }

      if (pas.pasId == 532L) {
        int bla = 4;
      }

      var pasFlowShifter = pasExecutors.get(pas);

      if(!(pasFlowShifter.getS2SendingFlow() > 0) || !pas.hasRegisteredBushes()) { // todo: this piece of code is duplication from line 166 -> consolidate
        pas.removeAllRegisteredBushes();
        continue;
      }

      var pasFlowShiftByRefTurn =
          pasFlowShifter.performEquilibratedCongestedFlowShifts(
              theMode,
              this,
              originalNetworkCosts,
              conjSegmentCosts,
              nlConsistentFlowAcceptanceFactors,
              getBushes(),
              logAll,
              PasFlowShiftExecutor.FlowShiftSmoothingApproach.RESET, // only obtaining shift to get to convergence, then reset
              1 /* not relevant when resetting */);

      boolean snappedToZero = pasFlowShiftByRefTurn.third();
      double pasFlowShifted = Math.abs(pasFlowShiftByRefTurn.second());
      if (pasFlowShifted > 0 && !snappedToZero) {
        // we only apply smoothing on non-snapped PASs as the snapped PASs are fully effectuated and are to be ignored
        pasDesiredFlowShifts.put(pas, Pair.of(pasFlowShiftByRefTurn.first(), pasFlowShiftByRefTurn.second()));
      }
    }

    return pasDesiredFlowShifts;
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
   * @param theMode
   * @param simulationData
   * @return
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

    // Capture original alphas, so we can use minimum of those and updated alphas to determine
    // available sub path sending flow with the most restrictive ensuring we are not shifting too much flow
    // as it is possible node model updates increased alphas making it seem more flow can be shifted when in reality
    // doing so can cause zero flow links in the middle of a PAS (which we want to avoid as it causes issues with
    // splitting rates at the end)
    var nlConsistentFlowAcceptanceFactors =
        Arrays.copyOf(
            getLoading().getCurrentFlowAcceptanceFactors(),getLoading().getCurrentFlowAcceptanceFactors().length);

    // STEP4: Create Sorted list of PASs in desired order to perform flow shifts (high to low) based on relevant
    // criterion. todo: in future we can use this to not equilibrate all of them, currently we do all
    Collection<Pas<V,ES>> sortedPass =
        flowShiftingStepFourOrderPass(pasExecutors, nlConsistentFlowAcceptanceFactors);

    //todo: <-- should not be necessary anymore now that we use P1/P2 intersection of constructing bush (see Nie 2009)
    // STEP5: Any new PASs that were identified may be in conflict with each other regarding introducing cycles. Using
    // the established ordering, we verify if a conflict exists, and if it does, remove cycle introducing PASs from
    // the affected bush where the higher priority new PAS is kept and the lower priority one discarded (for such a bush)
    //flowShiftingStepFiveRemoveConflictingNewPass(sortedPass, pasExecutors, newAndUpdatedPass);

    hookBeforePasUpdate(pasExecutors.values());

    Set<Pas<V,ES>> flowShiftedPass = new TreeSet<>();
    Set<Pas<V,ES>> passWithoutBush = new TreeSet<>();

    // v1 = approach with internal PAS equilibration in full with per pas alpha/splittingrates/cost updates and
    //      do that x times sequentially across PASs. Results on Leuven can get you to 6*10^-5
    // V2 = approach with internal PAS equiliobration in full only to egt desired shift, reset after. Then collect all
    //      shifts and perform ACROSS PASs mini-loading for updating alphas/splittingrates/cost jointly across PAs and
    //      do that x times. The idea here is that by updating the network alphas and splitting rates across PASs should
    //      add to stability
    boolean v2Activated = false;
    if(v2Activated && getSettings().getSltmType()==StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED){

      int crossPassUpdateIndex = 1;
      int MAX_UPDATE_ITERATIONS = 4;
      do {
        LOGGER.info("++++++++++++++++++++++++++++++++++ V2 OUTER-INNER ITERATION "+crossPassUpdateIndex+" ++++++++++++++++++++++++++++++++++");
        // DETERMINE PER PAS THE DESIRED STEP TO MAKE (to reach convergence individually)
        Map<Pas<V, ES>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts = new TreeMap<>();
        {
          //getSettings().enableTrackedOds(false); // switch off temporarily

          var uncongestedPasDesiredFlowShifts = determineConvergenceBasedUncongestedFlowShiftsV2(
              theMode, sortedPass, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);
          pasDesiredFlowShifts.putAll(uncongestedPasDesiredFlowShifts);

          var congestedPasDesiredFlowShifts = determineConvergenceBasedCongestedFlowShiftsV2(
              theMode, sortedPass, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);
          // add values together if both existing in uncongested and congested, otherwise take value as is
          congestedPasDesiredFlowShifts.forEach((key, value) ->
              pasDesiredFlowShifts.merge(
                  key, value,
                  (uncongVal, congVal) -> Pair.of(uncongVal.first(), uncongVal.second() + congVal.second())));
          getSettings().enableTrackedOds(true); // switch back on
        }

        // determine overlap based smoothing per PAS
        // check shift on each link, determine total proposed value and then per PAS per link determine portion
        // the PAS takes up, then take minimum of all portions as the smoothing factor
        var smoothedPasDesiredFlowShifts =
            applyOverlapSmoothingToProposedPasShifts(theMode, pasDesiredFlowShifts);

        var resultContainers = constructPasTouchedNetworkEntities(smoothedPasDesiredFlowShifts.keySet());
        var onPasTouchedNodes = resultContainers.first();
        var onPasTouchedSegments = resultContainers.second();
        // needed to sync outflows to updated inflows on link-level. This to avoid inconsistent u/v causing invalid alphas
        var entrySegmentOutflowUpdateNodes = resultContainers.third();

        //todo: if we are going to apply smoothing --> decay function formerly applied within outer-inner loop of v1
        //  should be applied here before we are going to apply the shifts.
        var flowShiftedAndObsoletePass = performLocalisedPasNetworkLoading(
            theMode, smoothedPasDesiredFlowShifts, pasExecutors, originalNetworkCosts, getBushes(), false,
            onPasTouchedNodes, onPasTouchedSegments, entrySegmentOutflowUpdateNodes);
        flowShiftedPass.addAll(flowShiftedAndObsoletePass.first());
        passWithoutBush.addAll(flowShiftedAndObsoletePass.second());

        // update NL loading consistent alphas for next iteration
        nlConsistentFlowAcceptanceFactors =
            Arrays.copyOf(
                getLoading().getCurrentFlowAcceptanceFactors(),getLoading().getCurrentFlowAcceptanceFactors().length);

        // update costs
        {
          // localised, so we DO NOT want to do a full loading sync as it defies the purpose of the localised loading
          boolean syncFullNetworkFlowsBeforeCostUpdate = false;
          executeCostUpdateAfterLoading(theMode, originalNetworkCosts, syncFullNetworkFlowsBeforeCostUpdate);
        }

        // sync bush flows to loading (but only for localised parts of the bush)
        syncBushFlowsToNetworkFlows(onPasTouchedNodes);

      }while(crossPassUpdateIndex++ < MAX_UPDATE_ITERATIONS);

    }else{
      // V1 approach - if V2 works deprecate and remove, or apply in first x iterations before switching to v2
      // UNCONGESTED ONLY
      var flowShiftedAndObsoletePass = doUncongestedFlowShiftingV1(
          theMode, sortedPass, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);
      flowShiftedPass = flowShiftedAndObsoletePass.first();
      passWithoutBush = flowShiftedAndObsoletePass.second();

      //updatedPass.forEach( p -> LOGGER.info("Updated PAS: " + p.toString()));
      LOGGER.info(String.format("%.2f%% Uncongested Flow shifts performed: %d ---- [#Uncongested PASs without remaining flows %d)]",
          ((double)flowShiftedPass.size()*100.0)/sortedPass.size(), flowShiftedPass.size(), passWithoutBush.size()));

      // Perform flow shifts for CONGESTED AND BECOMING CONGESTED WITH SHIFT PASs
      flowShiftedAndObsoletePass = doCongestedFlowShiftingV1(
          theMode, sortedPass, pasExecutors, nlConsistentFlowAcceptanceFactors, originalNetworkCosts, simulationData);

      LOGGER.info(String.format("%.2f%% Congested Flow shifts performed: %d ---- [#Uncongested PASs without remaining flows %d)]",
          ((double)flowShiftedAndObsoletePass.first().size()*100.0)/sortedPass.size(), flowShiftedAndObsoletePass.first().size(), flowShiftedAndObsoletePass.second().size()));

      flowShiftedPass.addAll(flowShiftedAndObsoletePass.first());
      passWithoutBush.addAll(flowShiftedAndObsoletePass.second());

    }

    // STEP8 : Finalise
    // dispose of PASs that no longer have S2 flows
    flowShiftingStepEightFinalise(passWithoutBush);

    return flowShiftedPass;
  }

  /**
   * When conducting a localised loading update, we need to know what (original) nodes and edge segments are touched
   * and are eligible for updates (while the rest is not). This method based on provided PASs determines this.
   *
   * @param passToConsider pass to consider
   * @return triple of (i) touched on PAS nodes, (ii) touched entry segments of PAS nodes, (iii) same as (ii) but
   * with merge exits added as those require syncing since their sending flows are updated (so outflows also need updating)
   */
  protected abstract Triple<TreeSet<DirectedVertex>,TreeSet<EdgeSegment>,TreeSet<DirectedVertex> >
  constructPasTouchedNetworkEntities(Set<Pas<V,ES>> passToConsider);

  // given desired shifts, smooth based on identified overlap, returned map is the final smoother shift we propose
  protected abstract Map<Pas<V,ES>, Pair<EdgeSegment, Double>> applyOverlapSmoothingToProposedPasShifts(
      Mode theMode, Map<Pas<V,ES>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts);

  // given the desired shifts, perform network loading not based on full bushes, but only on the individual PAS level.
  // objective is to update costs afterwards to inform next inner iteration of route choice considering PAS interactions
  protected abstract Pair<Set<Pas<V,ES>>, Set<Pas<V,ES>>> performLocalisedPasNetworkLoading(
      Mode theMode,
      Map<Pas<V,ES>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts,
      Map<Pas<V, ES>, PasFlowShiftExecutor<V, ES>> pasExecutors,
      double[] originalNetworkCosts,
      Set<B> bushes,
      boolean logAll,
      TreeSet<DirectedVertex> onPasTouchedNodes,
      TreeSet<EdgeSegment> onPasTouchedSegments,
      TreeSet<DirectedVertex> pasMergeExitDownstreamNodesForOutFlowUpdate);

  /**
   * Allow implementations to do prep before we enter loop of per congested PAS update, e.g.
   * initialise some across PAS tracking information for example
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

  protected  Map<Pas<V, ES>,Pair<EdgeSegment,Double>> determineConvergenceBasedUncongestedFlowShiftsV2(
      Mode theMode, Collection<Pas<V,ES>> sortedPass,
      Map<Pas<V,ES>, PasFlowShiftExecutor<V,ES>> pasExecutors,
      double[] nlConsistentFlowAcceptanceFactors,
      double[] originalNetworkCosts,
      StaticLtmSimulationData simulationData) {
    // stub implementation (not implemented for regular destination based yet
    return Map.of(null, null);
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
  protected Set<B> getBushes(){
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

  public void syncBushFlowsToNetworkFlowsForSelectedBushes(Set<? extends RootedBush<?,?>> bushesToSync) {
    for (var bush : bushesToSync) {
      if (bush == null) {
        continue;
      }

      bush.syncToNetworkFlows(getLoading().getCurrentFlowAcceptanceFactors(), null);
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

      // UGLY
      if(this instanceof StaticLtmConjugateBushStrategy){
//      LOGGER.severe("#############################################################");
//      var the140dBush = getBushes().stream().filter(b -> b.getRootZone().getXmlId().equals("140")).findFirst().get();
//      LOGGER.severe(the140dBush.toString());
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

//        passToConsider.values().stream().sorted(Comparator.comparingDouble(Pas::getReducedCost)).map(p ->
//            String.format("%.10f - %s",p.getReducedCost(),p)).forEach(LOGGER::info);


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
