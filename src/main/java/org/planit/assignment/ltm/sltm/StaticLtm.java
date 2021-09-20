package org.planit.assignment.ltm.sltm;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.ltm.LtmAssignment;
import org.planit.gap.NormBasedGapFunction;
import org.planit.interactor.LinkInflowOutflowAccessee;
import org.planit.network.MacroscopicNetwork;
import org.planit.od.demand.OdDemands;
import org.planit.od.path.OdPaths;
import org.planit.od.path.OdPathsHashed;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.path.DirectedPathFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.OdZone;
import org.planit.zoning.Zoning;

/**
 * Static Link Transmission Model implementation (sLTM) for network loading based on solution method presented in Raadsen and Bliemer (2021) General solution scheme for the Static
 * Link Transmission Model .
 * <p>
 * Defaults initiated via configurator:
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * </ul>
 *
 * @author markr
 *
 */
public class StaticLtm extends LtmAssignment implements LinkInflowOutflowAccessee {

  /** generated UID */
  private static final long serialVersionUID = 8485652038791612169L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtm.class.getCanonicalName());

  /** flag indicating whether or not to take link storage constraints into consideration, i.e. have a point queue or a physical queuing model */
  private boolean disableLinkStorageConstraints;

  /** flag indicating whether or not to activate additional detailed logging during the sLTM assignment */
  private boolean activateDetailedLogging;

  /** the tracked simulation data during assignment */
  private StaticLtmSimulationData simulationData;

  /**
   * Record some basic iteration information such as duration and gap
   *
   * @param startTime  the original start time of the iteration
   * @param dualityGap the duality gap at the end of the iteration
   * @return the time (in ms) at the end of the iteration for profiling purposes only
   */
  private Calendar logBasicIterationInformation(final Calendar startTime, final double dualityGap) {
    final Calendar currentTime = Calendar.getInstance();
    LOGGER.info(createLoggingPrefix(simulationData.getIterationIndex()) + String.format("Network cost: N/A (yet)"));
    LOGGER.info(
        createLoggingPrefix(simulationData.getIterationIndex()) + String.format("Gap: %.10f (%d ms)", dualityGap, currentTime.getTimeInMillis() - startTime.getTimeInMillis()));
    return currentTime;
  }

  /**
   * Create the od paths based on provided costs. Only create paths for od pairs with non-zero flow.
   * 
   * @param currentSegmentCosts costs to use for the shortest path algorithm
   * @param mode                to use
   * @param timePeriod
   * @return create odPaths
   * @throws PlanItException thrown if error
   */
  private OdPaths createOdPaths(final double[] currentSegmentCosts, final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(currentSegmentCosts, getTotalNumberOfNetworkSegments(),
        getTotalNumberOfNetworkVertices());
    DirectedPathFactory pathFactory = new DirectedPathFactoryImpl(getIdGroupingToken());
    OdPaths odPaths = new OdPathsHashed(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());

    OdDemands odDemand = getDemands().get(mode, timePeriod);
    Zoning zoning = getTransportNetwork().getZoning();
    for (OdZone origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
      for (OdZone destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        /* for positive demand on OD generate the shortest path under given costs */
        Double currOdDemand = odDemand.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          DirectedPath path = oneToAllResult.createPath(pathFactory, origin.getCentroid(), destination.getCentroid());
          if (path == null) {
            LOGGER.warning(String.format("%sUnable to create path for OD (%s,%s) with non-zero demand (%.2f)", LoggingUtils.createRunIdPrefix(getId()), origin.getXmlId(),
                destination.getXmlId(), currOdDemand));
            continue;
          }
          odPaths.setValue(origin, destination, path);
        }
      }
    }
    return odPaths;
  }

  /**
   * Apply the relevant network loading settings on the passed in network loading based on the configuration of this assignment
   * 
   * @param networkLoading to configure before running it
   */
  private void configureNetworkLoadingSettings(final StaticLtmNetworkLoading networkLoading) {
    /* point queue or physical queue */
    networkLoading.getSettings().setDisableStorageConstraints(this.disableLinkStorageConstraints);
    /* detailed logging */
    networkLoading.getSettings().setDetailedLogging(isActivateDetailedLogging());

    /* temporary check until supported */
    if (!networkLoading.getSettings().isDisableStorageConstraints()) {
      LOGGER.severe(
          String.format("%sIGNORE: sLTM with physical queues is not yet implemented, please disable storage constraints and try again", LoggingUtils.createRunIdPrefix(getId())));
      return;
    }
  }

  /**
   * Initialize time period assigment and construct the network loading instance to use
   *
   * @param timePeriod the time period
   * @param mode       covered by this time period
   * @param odDemands  to use during this loading
   * @return simulationData initialised for time period
   * @throws PlanItException thrown if there is an error
   */
  private StaticLtmSimulationData initialiseTimePeriod(TimePeriod timePeriod, final Mode mode, final OdDemands odDemands) throws PlanItException {

    /* register new time period on costs */
    getPhysicalCost().updateTimePeriod(timePeriod);
    getVirtualCost().updateTimePeriod(timePeriod);

    /* compute costs to start with */
    double[] currentSegmentCosts = executeCostsUpdate(mode);

    // TODO no support for initial cost yet

    /* initial paths based on costs */
    OdPaths odPaths = createOdPaths(currentSegmentCosts, mode, timePeriod);
    // for path based -> odmultipath option
    // for bush based -> originbased bushes option
    // both to be supported by network loading...

    /* create the network loading algorithm components instance */
    StaticLtmNetworkLoading networkLoading = new StaticLtmNetworkLoading(getIdGroupingToken(), getId(), getTransportNetwork(), mode, odPaths, odDemands);

    return new StaticLtmSimulationData(networkLoading, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
  }

  /**
   * Verify convergence progress and if insufficient attempt to activate one or more extensions to overcome convergence difficulties
   * 
   * @param networkLoading               to verify progress on
   * @param networkLoadingIterationIndex we are at
   */
  private void verifyNetworkLoadingConvergenceProgress(StaticLtmNetworkLoading networkLoading, int networkLoadingIterationIndex) {
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
                LoggingUtils.createRunIdPrefix(getId()), networkLoadingIterationIndex));
      }
    }
  }

  /**
   * Execute for a specific time period
   * 
   * @param timePeriod to execute traffic assignment for
   * @param modes      used for time period
   * @throws PlanItException thrown if error
   */
  private void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes) throws PlanItException {

    if (modes.size() != 1) {
      LOGGER.warning(String.format("%ssLTM only supports a single mode for now, found %s, aborting assignment for time period %s", LoggingUtils.createRunIdPrefix(getId()),
          timePeriod.getXmlId()));
      return;
    }

    /* prep */
    Mode theMode = modes.iterator().next();
    this.simulationData = initialiseTimePeriod(timePeriod, theMode, getDemands().get(theMode, timePeriod));
    configureNetworkLoadingSettings(simulationData.getNetworkLoading());

    boolean converged = false;
    Calendar iterationStartTime = Calendar.getInstance();

    /* ASSIGNMENT LOOP */
    do {
      getGapFunction().reset();
      getSmoothing().updateStep(simulationData.getIterationIndex());

      // NETWORK LOADING - MODE AGNOSTIC FOR NOW
      {
        executeNetworkLoading();
      }

      // COST UPDATE
      getIterationData().setLinkSegmentTravelTimeHour(theMode, executeCostsUpdate(theMode));

      // PERSIST
      getOutputManager().persistOutputData(timePeriod, modes, converged);

      // CONVERGENCE CHECK
      getGapFunction().computeGap();
      converged = getGapFunction().hasConverged(simulationData.getIterationIndex());

      // SMOOTHING
      // TODO smoothing.execute()

      simulationData.incrementIterationIndex(); // different location from traditional static (more logical location) -- careful changing this
      iterationStartTime = logBasicIterationInformation(iterationStartTime, getGapFunction().getGap());
    } while (!converged);

  }

  /**
   * Update the costs based on the network loading solution found.
   * 
   * @param mode to collect costs for
   * @return computed costs for all edge segments in the network
   * @throws PlanItException thrown if error
   */
  private double[] executeCostsUpdate(Mode mode) throws PlanItException {
    /* cost array across all segments, virtual and physical */
    double[] currentSegmentCosts = new double[getTransportNetwork().getNumberOfEdgeSegmentsAllLayers()];

    /* virtual cost */
    getVirtualCost().populateWithCost(getTransportNetwork().getVirtualNetwork(), mode, currentSegmentCosts);

    /* physical cost */
    getPhysicalCost().populateWithCost(getInfrastructureNetwork().getLayerByMode(mode), mode, currentSegmentCosts);

    return currentSegmentCosts;
  }

  /**
   * Perform a network loading based on the current assignment state
   * 
   */
  private void executeNetworkLoading() {
    StaticLtmNetworkLoading sLtmLoading = simulationData.getNetworkLoading();

    /* STEP 0 - Initialisation */
    if (!sLtmLoading.stepZeroInitialisation()) {
      LOGGER.severe(String.format("%sAborting sLTM assignment %s, unable to continue", LoggingUtils.createRunIdPrefix(getId())));
    }

    /* for now we do not consider path choice, we conduct a one-shot all-or-nothing network loading */
    int networkLoadingIterationIndex = 0;
    do {

      /* verify if progress is being made and if not activate extensions as deemed adequate */
      verifyNetworkLoadingConvergenceProgress(sLtmLoading, networkLoadingIterationIndex);

      /* STEP 1 - Splitting rates update before sending flow update */
      sLtmLoading.stepOneSplittingRatesUpdate();

      /* STEP 2 - Sending flow update */
      sLtmLoading.stepTwoInflowSendingFlowUpdate();

      /* STEP 3 - Splitting rates update before receiving flow update */
      sLtmLoading.stepThreeSplittingRateUpdate();

      /* STEP 4 - Receiving flow update */
      sLtmLoading.stepFourOutflowReceivingFlowUpdate();

      /* STEP 5 - Network loading convergence */
    } while (!sLtmLoading.stepFiveCheckNetworkLoadingConvergence(networkLoadingIterationIndex++));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    super.verifyComponentCompatibility();

    /* gap function check */
    PlanItException.throwIf(!(getGapFunction() instanceof NormBasedGapFunction), "%sStatic LTM only supports a norm based gap function at the moment, but found %s",
        LoggingUtils.createRunIdPrefix(getId()), getGapFunction().getClass().getCanonicalName());
  }

  /**
   * Initialise the components before we start any assignment
   */
  @Override
  protected void initialiseBeforeExecution() throws PlanItException {
    super.initialiseBeforeExecution();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeEquilibration() throws PlanItException {
    // perform assignment per period
    final Collection<TimePeriod> timePeriods = getDemands().timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (final TimePeriod timePeriod : timePeriods) {
      Calendar startTime = Calendar.getInstance();
      final Calendar initialStartTime = startTime;
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + LoggingUtils.createTimePeriodPrefix(timePeriod) + timePeriod.toString());
      executeTimePeriod(timePeriod, getDemands().getRegisteredModesForTimePeriod(timePeriod));
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
    }
  }

  /**
   * Return the simulation data for the current iteration
   *
   * @return simulation data
   */
  protected StaticLtmSimulationData getIterationData() {
    return simulationData;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public StaticLtm(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param sltm to copy
   */
  public StaticLtm(StaticLtm sltm) {
    super(sltm);
    this.activateDetailedLogging = sltm.activateDetailedLogging;
    this.disableLinkStorageConstraints = sltm.disableLinkStorageConstraints;
    this.simulationData = sltm.simulationData.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork getInfrastructureNetwork() {
    return (MacroscopicNetwork) super.getInfrastructureNetwork();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    OutputTypeAdapter outputTypeAdapter = null;
    switch (outputType) {
    case LINK:
      outputTypeAdapter = new StaticLtmLinkOutputTypeAdapter(outputType, this);
      break;
    case OD:
      // NOT YET SUPPORTED
      break;
    case PATH:
      // NOT YET SUPPORTED
      break;
    default:
      LOGGER.warning(String.format("%s%s is not supported yet", LoggingUtils.createRunIdPrefix(getId()), outputType.value()));
    }
    return outputTypeAdapter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getIterationIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtm clone() {
    return new StaticLtm(this);
  }

  // GETTERS/SETTERS

  /**
   * Verify to enable link storage constraints or not
   * 
   * @return true when enabled, false otherwise
   */
  public boolean isDisableLinkStorageConstraints() {
    return disableLinkStorageConstraints;
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public void setDisableLinkStorageConstraints(boolean flag) {
    this.disableLinkStorageConstraints = flag;
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public void setActivateDetailedLogging(boolean flag) {
    this.activateDetailedLogging = flag;
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public boolean isActivateDetailedLogging() {
    return this.activateDetailedLogging;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentInflowsPcuHour() {
    return simulationData.getNetworkLoading().getCurrentInflowsPcuH();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentOutflowsPcuHour() {
    return simulationData.getNetworkLoading().getCurrentInflowsPcuH();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    this.simulationData.reset();
  }

}
