package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.loading.SplittingRateData;
import org.planit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.virtual.AbstractVirtualCost;
import org.planit.gap.GapFunction;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.interactor.TrafficAssignmentComponentAccessee;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.time.TimePeriod;

/**
 * Base class for dealing with different assignment solution methods within sLTM. These solution methods differ regarding their approach to representing path choices, e.g. bush
 * based, or path based.
 * 
 * @author markr
 *
 */
public abstract class StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmAssignmentStrategy.class.getCanonicalName());

  // INPUTS

  /** token to use for underlying instances */
  private final IdGroupingToken idGroupingToken;

  /** parent assignment id */
  private final long assignmentId;

  /**
   * Transport model network used
   */
  private final TransportModelNetwork transportModelNetwork;

  /**
   * Network loading to use
   */
  private StaticLtmNetworkLoading networkLoading;

  /** OD demands used */
  private OdDemands odDemands;

  /** static LTM specific settings to use */
  private final StaticLtmSettings settings;

  /** the user configured traffic assignment components used */
  private final TrafficAssignmentComponentAccessee taComponents;

  /**
   * Based on the provided costs we update the network costs portion on the gap function. This is agnostic to the the applied strategy as it is simply a multiplication of the link
   * segment costs with the demand traversing the link
   * 
   * @param theMode                 these costs apply to
   * @param networkLinkSegmentCosts
   */
  private void executeGapNetworkCostsUpdate(final Mode theMode, final double[] networkLinkSegmentCosts) {
    LinkBasedRelativeDualityGapFunction gapFunction = ((LinkBasedRelativeDualityGapFunction) getTrafficAssignmentComponent(GapFunction.class));
    double[] inflowsPcuH = getLoading().getCurrentInflowsPcuH();

    /* flows and costs structured in array, simply use synchronised index, no need to relate to link segments explicitly */
    int numberOfEdgeSegments = networkLinkSegmentCosts.length;
    for (int index = 0; index < numberOfEdgeSegments; ++index) {
      gapFunction.increaseMeasuredNetworkCost(networkLinkSegmentCosts[index] * inflowsPcuH[index]);
    }

  }

  /**
   * The transport model network used
   * 
   * @return transport model network
   */
  protected TransportModelNetwork getTransportNetwork() {
    return transportModelNetwork;
  }

  /**
   * The physical network used
   */
  protected MacroscopicNetwork getInfrastructureNetwork() {
    return (MacroscopicNetwork) getTransportNetwork().getInfrastructureNetwork();
  }

  /**
   * Token to use
   * 
   * @return token
   */
  protected IdGroupingToken getIdGroupingToken() {
    return idGroupingToken;
  }

  /**
   * Collect parent assignment id
   * 
   * @return parent assignment id
   */
  protected long getAssignmentId() {
    return assignmentId;
  }

  /**
   * The network loading to apply
   * 
   * @return network loading
   */
  protected StaticLtmNetworkLoading getLoading() {
    return networkLoading;
  }

  /**
   * The settings
   * 
   * @return settings
   */
  protected StaticLtmSettings getSettings() {
    return settings;
  }

  /**
   * Get od demands used
   * 
   * @return odDemands used
   */
  protected OdDemands getOdDemands() {
    return this.odDemands;
  }

  /**
   * set od demand used
   * 
   * @param odDemands to use
   */
  protected void setOdDemands(final OdDemands odDemands) {
    this.odDemands = odDemands;
  }

  /**
   * collect a component based on its class component signature key
   * 
   * @param <T>                 key type of component
   * @param taComponentClassKey class of component
   * @return found component, if any
   */
  protected <T> T getTrafficAssignmentComponent(final Class<T> taComponentClassKey) {
    return taComponents.getTrafficAssignmentComponent(taComponentClassKey);
  }

  /**
   * Verify convergence progress and if insufficient attempt to activate one or more extensions to overcome convergence difficulties
   * 
   * @param networkLoading               to verify progress on
   * @param networkLoadingIterationIndex we are at
   */
  protected void verifyNetworkLoadingConvergenceProgress(StaticLtmNetworkLoading networkLoading, int networkLoadingIterationIndex) {
    /*
     * whenever the current form of the solution method does not suffice, we move to the next extension which attempts to be more cautious and has a higher likelihood of finding a
     * solution at the cost of slower convergence, so whenever we are not yet stuck, we try to avoid activating these extensions.
     */
    if (!networkLoading.isConverging()) {
      // dependent on whether or not we are modelling physical queues or not and where we started with settings
      boolean changedScheme = networkLoading.activateNextExtension(true);
      if (!changedScheme) {
        LOGGER.warning(
            String.format("%sDetected network loading is not converging as expected (internal loading iteration %d) - unable to activate further extensions, consider aborting",
                LoggingUtils.createRunIdPrefix(getAssignmentId()), networkLoadingIterationIndex));
      }
    }
  }

  /**
   * Perform a network loading based on the current assignment state
   * 
   */
  protected void executeNetworkLoading() {

    /* STEP 0 - Initialisation */
    if (!getLoading().stepZeroInitialisation()) {
      LOGGER.severe(String.format("%sAborting sLTM assignment %s, unable to continue", LoggingUtils.createRunIdPrefix(getAssignmentId())));
    }

    /* for now we do not consider path choice, we conduct a one-shot all-or-nothing network loading */
    int networkLoadingIterationIndex = 0;
    do {

      /* verify if progress is being made and if not activate extensions as deemed adequate */
      verifyNetworkLoadingConvergenceProgress(getLoading(), networkLoadingIterationIndex);

      /* STEP 1 - Splitting rates update before sending flow update */
      getLoading().stepOneSplittingRatesUpdate();

      /* STEP 2 - Sending flow update */
      getLoading().stepTwoInflowSendingFlowUpdate();

      /* STEP 3 - Splitting rates update before receiving flow update */
      getLoading().stepThreeSplittingRateUpdate();

      /* STEP 4 - Receiving flow update */
      getLoading().stepFourOutflowReceivingFlowUpdate();

      /* STEP 5 - Network loading convergence */
    } while (!getLoading().stepFiveCheckNetworkLoadingConvergence(networkLoadingIterationIndex++));
  }

  /**
   * Factory method to create the desired network loading
   * 
   * @return network loading for this solution approach
   */
  protected abstract StaticLtmNetworkLoading createNetworkLoading();

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for id generation
   * @param assignmentId          id of the parent assignment
   * @param transportModelNetwork the transport model network
   * @param settings              the sLTM settings to use
   * @param taComponents          to use
   */
  public StaticLtmAssignmentStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    this.transportModelNetwork = transportModelNetwork;
    this.assignmentId = assignmentId;
    this.idGroupingToken = idGroupingToken;
    this.settings = settings;
    this.taComponents = taComponents;
  }

  /**
   * Invoked before start of equilibrating a new time period
   * 
   * @param timePeriod to initialise for
   * @param odDemands  to use
   */
  public void initialiseTimePeriod(final TimePeriod timePeriod, final Mode mode, final OdDemands odDemands) {
    this.networkLoading = createNetworkLoading();
    this.networkLoading.initialiseInputs(mode, odDemands, getTransportNetwork());
    setOdDemands(odDemands);
  }

  /**
   * Create the initial solution to start the equilibration process with
   * 
   * @param initialLinkSegmentCosts to use
   */
  public abstract void createInitialSolution(double[] initialLinkSegmentCosts);

  /**
   * Perform a single iteration where we perform a loading and then an equilibration step resulting in updated costs
   *
   * @param mode          to use
   * @param costsToUpdate the link segment costs we are updating (possibly partially for all link segments that might have been affected by a loading
   * @return true when iteration could be successfully completed, false otherwise
   */
  public abstract boolean performIteration(final Mode theMode, final double[] costsToUpdate);

  /**
   * Perform an update of the network wide costs where a partial update is applied in case only potentially blocking nodes are updated during the loading
   * 
   * @param theMode                    to perform the update for
   * @param updateOnlyTrackedNodeCosts flag indicating if only the costs of the entry link segments of tracked nodes are to be updated, or all link segment costs are to be updated
   * @param costsToUpdate              the network wide costs to update (fully or partially), this is an output
   * @throws PlanItException thrown if error
   */
  public void executeNetworkCostsUpdate(Mode theMode, boolean updateOnlyTrackedNodeCosts, double[] costsToUpdate) throws PlanItException {

    final AbstractPhysicalCost physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    final AbstractVirtualCost virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);
    SplittingRateData splittingRateData = getLoading().getSplittingRateData();
    if (updateOnlyTrackedNodeCosts) {

      MacroscopicNetworkLayer networkLayer = getInfrastructureNetwork().getLayerByMode(theMode);
      VirtualNetwork virtualLayer = getTransportNetwork().getZoning().getVirtualNetwork();

      /* only update when node is both (flow) tracked as well as potentially blocking */
      for (DirectedVertex trackedFlowNode : splittingRateData.getTrackedNodes()) {
        if (!splittingRateData.isPotentiallyBlocking(trackedFlowNode)) {
          continue;
        }

        /* entry segments */
        networkLayer.getLinkSegments().forEachMatchingIdIn(trackedFlowNode.getEntryEdgeSegments(),
            (es) -> costsToUpdate[(int) es.getId()] = physicalCost.getSegmentCost(theMode, (MacroscopicLinkSegment) es));
        virtualLayer.getConnectoidSegments().forEachMatchingIdIn(trackedFlowNode.getEntryEdgeSegments(),
            (es) -> costsToUpdate[(int) es.getId()] = virtualCost.getSegmentCost(theMode, (ConnectoidSegment) es));
      }
    }
    /* OTHER -> all nodes (and attached links) are updated, update all costs */
    else {

      /* virtual cost */
      virtualCost.populateWithCost(getTransportNetwork().getVirtualNetwork(), theMode, costsToUpdate);

      /* physical cost */
      physicalCost.populateWithCost(getInfrastructureNetwork().getLayerByMode(theMode), theMode, costsToUpdate);

    }

    /* use the provided costs to update the current state of the network costs to compare against the potential min costs for the gap */
    executeGapNetworkCostsUpdate(theMode, costsToUpdate);
  }

  /**
   * Description of the chosen sLTM strategy for equilibration
   * 
   * @return String
   */
  public abstract String getDescription();

}
