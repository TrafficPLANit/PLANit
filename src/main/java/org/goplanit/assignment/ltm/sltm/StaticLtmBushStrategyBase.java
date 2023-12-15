package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.zoning.Zoning;

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

    double factor = 1;
    for (Pas pas : sortedPass) {

      var pasFlowShifter = createPasFlowShiftExecutor(pas, getSettings());
      pasFlowShifter.initialise(); // to be able to collect pas sending flows for gap

      if (!(pasFlowShifter.getS2SendingFlow() > 0)) {
        /* PAS is redundant, no more flow remaining (for example due to flow shifts on other PASs with initial overlapping S2 segments) */
        pas.removeAllRegisteredBushes();
        passWithoutOrigins.add(pas);
        continue;
      }

      updateGap(gapFunction, pas, pasFlowShifter.getS1SendingFlow(), pasFlowShifter.getS2SendingFlow());
      if (pas.containsAny(linkSegmentsUsed)) {
        continue;
      }

      /* untouched PAS (no flows shifted yet) in this iteration */
      boolean pasFlowShifted = pasFlowShifter.run(theMode, physicalCost, virtualCost, networkLoading, factor);
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

  /** tracked bushes (with non-zero demand) */
  protected B[] bushes;

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
  protected void updateGap(final LinkBasedRelativeDualityGapFunction gapFunction, final Pas pas, double s1SendingFlow, double s2SendingFlow) {
    gapFunction.increaseConvexityBound(pas.getAlternativeLowCost() * (s1SendingFlow + s2SendingFlow));
    gapFunction.increaseMeasuredCost(s1SendingFlow * pas.getAlternativeLowCost());
    gapFunction.increaseMeasuredCost(s2SendingFlow * pas.getAlternativeHighCost());
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
   * @param linkSegmentCosts to use
   * @return newly created PASs
   * @throws PlanItException thrown if error
   */
  protected abstract Collection<Pas> updateBushPass(final double[] linkSegmentCosts) throws PlanItException;

  /**
   * Verify if solution is flow proportional
   * 
   * @param gapEpsilon to use
   * @return true when flow proportional, false otherwise
   */
  protected boolean isSolutionFlowEntropyMaximised(double gapEpsilon) {
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
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  protected StaticLtmBushStrategyBase(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    /*
     * destination based bushes are inverted, so PASs are to be registered based on vertex farthest from root, i.e, farthest from destination, so at the upstream point of the PAS
     * at its diverge
     */
    boolean registerPassByDiverge = settings.getSltmType() == StaticLtmType.DESTINATION_BUSH_BASED;
    this.pasManager = new PasManager(registerPassByDiverge);

    this.pasManager.setDetailedLogging(settings.isDetailedLogging());
    this.equalFlowDistributedPass = new HashSet<>();
  }

  /**
   * Let derived implementations create the empty bushes as desired before populating them
   * 
   * @return created empty bushes suitable for this strategy
   */
  protected abstract B[] createEmptyBushes();

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
   * @param linkSegmentCosts costs to use
   * @throws PlanItException thrown when error
   */
  protected void initialiseBushes(final double[] linkSegmentCosts) throws PlanItException {
    final var shortestBushAlgorithm = createNetworkShortestBushAlgo(linkSegmentCosts);

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands();
    for (int index = 0; index < bushes.length; ++index) {
      B bush = bushes[index];
      if (bush == null) {
        continue;
      }
      initialiseBush(bush, zoning, odDemands, shortestBushAlgorithm);

      if (bush != null && getSettings().isDetailedLogging()) {
        LOGGER.info(bush.toString());
      }
    }
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
   *
   */
  @Override
  public void createInitialSolution(double[] initialLinkSegmentCosts, int iterationIndex) {
    try {

      /* delegate to concrete implementation */
      if (this.bushes == null || this.bushes.length == 0) {
        this.bushes = createEmptyBushes();
      }
      initialiseBushes(initialLinkSegmentCosts);

      /* update loading with information */
      getLoading().setBushes(bushes);
      getLoading().setPasManager(this.pasManager);

    } catch (PlanItException e) {
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
   * @param iterationIndex we're at
   * @return true when iteration could be successfully completed, false otherwise
   */
  @Override
  public boolean performIteration(final Mode theMode, final double[] prevCosts, double[] costsToUpdate, int iterationIndex) {
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
        syncBushFlowsToNetworkFlows();
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
