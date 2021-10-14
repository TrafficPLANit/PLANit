package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.loading.StaticLtmNetworkLoading;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
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

  /**
   * settings to use
   */
  private final StaticLtmSettings settings;

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
   * Create the initial solution to start the equilibration process with
   * 
   * @param timePeriod              to apply
   * @param odDemands               to use
   * @param initialLinkSegmentCosts to use
   */
  protected abstract void createInitialSolution(TimePeriod timePeriod, OdDemands odDemands, double[] initialLinkSegmentCosts);

  /**
   * Constructor
   * 
   * @param assignmentId of the parent assignment
   */
  public StaticLtmAssignmentStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
      final StaticLtmSettings settings) {
    this.transportModelNetwork = transportModelNetwork;
    this.assignmentId = assignmentId;
    this.idGroupingToken = idGroupingToken;
    this.settings = settings;
  }

  /**
   * Invoked before start of equilibrating a new time period
   * 
   * @param timePeriod              to initialise for
   * @param odDemands               to use
   * @param initialLinkSegmentCosts to use in unit hour
   */
  public void initialiseTimePeriod(final TimePeriod timePeriod, final Mode mode, final OdDemands odDemands, final double[] initialLinkSegmentCosts) {
    this.networkLoading = createNetworkLoading();
    this.networkLoading.initialiseInputs(mode, odDemands, getTransportNetwork());
    createInitialSolution(timePeriod, odDemands, initialLinkSegmentCosts);
  }

  /**
   * Perform a single iteration based on the current available network costs
   * 
   * @param linkSegmentCosts to use
   */
  public abstract void performIteration(final double[] linkSegmentCosts);

  /**
   * Perform an update of the network wide costs.
   * 
   * @param theMode      to perform the update for
   * @param virtualCost  component
   * @param physicalCost component
   * @return network wide costs based on currently prevailing flows
   * @throws PlanItException thrown if error
   */
  public double[] executeNetworkCostsUpdate(Mode theMode, final VirtualCost virtualCost, final AbstractPhysicalCost physicalCost) throws PlanItException {
    /* cost array across all segments, virtual and physical */
    double[] segmentCostsToPopulate = new double[getTransportNetwork().getNumberOfEdgeSegmentsAllLayers()];

    /* virtual cost */
    virtualCost.populateWithCost(getTransportNetwork().getVirtualNetwork(), theMode, segmentCostsToPopulate);

    /* physical cost */
    physicalCost.populateWithCost(getInfrastructureNetwork().getLayerByMode(theMode), theMode, segmentCostsToPopulate);

    return segmentCostsToPopulate;
  }

}
