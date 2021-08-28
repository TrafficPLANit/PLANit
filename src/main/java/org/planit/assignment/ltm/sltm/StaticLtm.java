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
public class StaticLtm extends LtmAssignment {

  /** generated UID */
  private static final long serialVersionUID = 8485652038791612169L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtm.class.getCanonicalName());

  /** flag indicating whether or not to take link storage constraints into consideration, i.e. have a point queue or a physical queuing model */
  private boolean disableLinkStorageConstraints;

  /** flag indicating whether or not to activate additional detailed logging during the sLTM assignment */
  private boolean activateDetailedLogging;

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
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = new DijkstraShortestPathAlgorithm(currentSegmentCosts, numberOfNetworkSegments, numberOfNetworkVertices);
    DirectedPathFactory pathFactory = new DirectedPathFactoryImpl(getIdGroupingToken());
    OdPaths odPaths = new OdPathsHashed(getIdGroupingToken(), getTransportNetwork().getZoning().getOdZones());

    OdDemands odDemand = this.demands.get(mode, timePeriod);
    Zoning zoning = this.transportNetwork.getZoning();
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
            LOGGER.warning(String.format("Unable to create path for OD (%s,%s) with non-zero demand (%.2f)", origin.getXmlId(), destination.getXmlId(), currOdDemand));
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
  }

  /**
   * Initialize time period assigment and construct the network loading instance to use
   *
   * @param timePeriod the time period
   * @param mode       covered by this time period
   * @param odDemands  to use duriung this loading
   * @return network loading instance to use
   * @throws PlanItException thrown if there is an error
   */
  private StaticLtmNetworkLoading initialiseTimePeriod(TimePeriod timePeriod, final Mode mode, final OdDemands odDemands) throws PlanItException {

    /* cost array across all segments, virtual and physical */
    double[] currentSegmentCosts = new double[transportNetwork.getNumberOfEdgeSegmentsAllLayers()];

    /* virtual cost */
    virtualCost.populateWithCost(mode, currentSegmentCosts);

    /* physical cost */
    getPhysicalCost().populateWithCost(mode, currentSegmentCosts);

    // TODO no support for initial cost yet

    OdPaths odPaths = createOdPaths(currentSegmentCosts, mode, timePeriod);

    /** create the network loading algorithm components instance */
    return new StaticLtmNetworkLoading(getIdGroupingToken(), getTransportNetwork(), mode, odPaths, odDemands);
  }

  /**
   * Verify convergence progress and if insufficient attempt to activate one or more extensions to overcome convergence difficulties
   * 
   * @param networkLoading               to verify progress on
   * @param networkLoadingIterationIndex we are at
   */
  private void verifyConvergenceProgress(StaticLtmNetworkLoading networkLoading, int networkLoadingIterationIndex) {
    /*
     * whenever the current form of the solution method does not suffice, we move to the next extension which attempts to be more cautious and has a higher likelihood of finding a
     * solution at the cost of slower convergence, so whenever we are not yet stuck, we try to avoid activating these extensions.
     */
    if (!networkLoading.isConverging()) {
      // dependent on whether or not we are modelling physical queues or not and where we started with settings
      // so bug if/else situation, therefore cleaner this way
      boolean changedScheme = networkLoading.activateNextExtension(true);
      if (changedScheme) {
        LOGGER.info(String.format("Detected network loading is not convergencing as expected (internal loading iteration %d), activating extension to mitigate",
            networkLoadingIterationIndex));
      } else {
        LOGGER.warning(
            String.format("Detected network loading is not convergencing as expected (internal loading iteration %d) - unable to activate further extensions, consider aborting",
                networkLoadingIterationIndex));
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
    /* for now, we only support a single mode to keep it simple */
    if (modes.size() != 1) {
      LOGGER.warning(String.format("sLTM only supports a single mode for now, found %s, aborting assignment for time period %s", timePeriod.getXmlId()));
      return;
    }

    /* prep */
    Mode theMode = modes.iterator().next();
    StaticLtmNetworkLoading networkLoading = initialiseTimePeriod(timePeriod, theMode, this.demands.get(theMode, timePeriod));
    configureNetworkLoadingSettings(networkLoading);

    /* temporary check until supported */
    if (!networkLoading.getSettings().isDisableStorageConstraints()) {
      LOGGER.severe("IGNORE: sLTM with physical queues is not yet implemented, please disable storage constraints and try again");
      return;
    }

    /* STEP 0 - Initialisation */
    if (!networkLoading.stepZeroInitialisation()) {
      LOGGER.severe(String.format("Aborting sLTM assignment %s, unable to continue", this.getId()));
    }

    /* for now we do not consider path choice, we conduct a one-shot all-or-nothing network loading */
    int networkLoadingIterationIndex = 0;
    do {

      /* verify if progress is being made and if not activate extensions as deemed adequate */
      verifyConvergenceProgress(networkLoading, networkLoadingIterationIndex);

      /* STEP 1 - Splitting rates update before sending flow update */
      networkLoading.stepOneSplittingRatesUpdate();

      /* STEP 2 - Sending flow update */
      networkLoading.stepTwoInflowSendingFlowUpdate();

      /* STEP 3 - Splitting rates update before receiving flow update */
      networkLoading.stepThreeSplittingRateUpdate();

      /* STEP 4 - Receiving flow update */
      networkLoading.stepFourOutflowReceivingFlowUpdate();

      /* STEP 5 - Network loading convergence */
    } while (!networkLoading.stepFiveCheckNetworkLoadingConvergence(networkLoadingIterationIndex++));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    super.verifyComponentCompatibility();

    /* gap function check */
    PlanItException.throwIf(!(getGapFunction() instanceof NormBasedGapFunction), "static LTM only supports a norm based gap function at the moment, but found %s",
        getGapFunction().getClass().getCanonicalName());
  }

  /**
   * Initialise the network loading components before we start any assignment
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
    final Collection<TimePeriod> timePeriods = demands.timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (final TimePeriod timePeriod : timePeriods) {
      Calendar startTime = Calendar.getInstance();
      final Calendar initialStartTime = startTime;
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + LoggingUtils.createTimePeriodPrefix(timePeriod) + timePeriod.toString());
      executeTimePeriod(timePeriod, demands.getRegisteredModesForTimePeriod(timePeriod));
      LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
    }
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    // TODO Auto-generated method stub
    return null;
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

}
