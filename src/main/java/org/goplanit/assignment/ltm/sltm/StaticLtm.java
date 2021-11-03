package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.LtmAssignment;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.interactor.LinkInflowOutflowAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.sdinteraction.smoothing.MSASmoothing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

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

  /** settings used */
  StaticLtmSettings settings;

  /** the chosen equilibration approach is implemented by the concrete implementation of this class */
  private StaticLtmAssignmentStrategy assignmentStrategy;

  /** the tracked simulation data containing link costs and iteration */
  private StaticLtmSimulationData simulationData;

  /**
   * Factory method to create the desired assignment strategy to use
   * 
   * @return created strategy
   */
  private StaticLtmAssignmentStrategy createAssignmentStrategy() {
    /* create the assignment solution to apply */
    if (settings.isBushBased()) {
      return new StaticLtmBushStrategy(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
    } else {
      return new StaticLtmPathStrategy(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
    }
  }

  /**
   * Record some basic iteration information such as duration and gap
   *
   * @param startTime  the original start time of the iteration
   * @param dualityGap the duality gap at the end of the iteration
   * @return the time (in ms) at the end of the iteration for profiling purposes only
   */
  private Calendar logBasicIterationInformation(final Calendar startTime, final LinkBasedRelativeDualityGapFunction gapFunction) {
    final Calendar currentTime = Calendar.getInstance();
    LOGGER.info(String.format("%sGap: %.10f (%d ms)", createLoggingPrefix(getIterationIndex()), gapFunction.getGap(), currentTime.getTimeInMillis() - startTime.getTimeInMillis()));
    return currentTime;
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

    // TODO no support for exogenous initial cost yet

    assignmentStrategy.updateTimePeriod(timePeriod, mode, odDemands);

    /* compute costs on all link segments to start with */
    boolean updateOnlyPotentiallyBlockingNodeCosts = false;
    double[] initialLinkSegmentCosts = new double[getTotalNumberOfNetworkSegments()];
    assignmentStrategy.executeNetworkCostsUpdate(mode, updateOnlyPotentiallyBlockingNodeCosts, initialLinkSegmentCosts);
    StaticLtmSimulationData simulationData = new StaticLtmSimulationData(timePeriod, List.of(mode), getTotalNumberOfNetworkSegments());
    simulationData.setLinkSegmentTravelTimePcuH(mode, initialLinkSegmentCosts);

    /* create initial solution as starting point for equilibration */
    assignmentStrategy.createInitialSolution(initialLinkSegmentCosts);

    return simulationData;
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

    boolean converged = false;
    Calendar iterationStartTime = Calendar.getInstance();

    /* ASSIGNMENT LOOP */
    do {
      getGapFunction().reset();
      assignmentStrategy.getLoading().resetIteration();

      simulationData.incrementIterationIndex();
      getSmoothing().updateStep(simulationData.getIterationIndex());

      /* LOADING UPDATE + PATH/BUSH UPDATE */
      double[] prevCosts = getIterationData().getLinkSegmentTravelTimePcuH(theMode);
      double[] costsToUpdate = Arrays.copyOf(prevCosts, prevCosts.length);
      boolean success = assignmentStrategy.performIteration(theMode, costsToUpdate, simulationData.getIterationIndex());
      if (!success) {
        LOGGER.severe("Unable to continue PLANit sLTM run, aborting");
        break;
      }
      // COST UPDATE
      getIterationData().setLinkSegmentTravelTimePcuH(theMode, costsToUpdate);

      // CONVERGENCE CHECK
      getGapFunction().computeGap();
      converged = getGapFunction().hasConverged(simulationData.getIterationIndex());

      // PERSIST
      persistIterationResults(timePeriod, theMode, converged);

      iterationStartTime = logBasicIterationInformation(iterationStartTime, (LinkBasedRelativeDualityGapFunction) getGapFunction());
    } while (!converged);

  }

  /**
   * Persist the results for this iteration. In case the results require additional actions because the loading has been optimised this is adjusted here before persisting
   * 
   * @param timePeriod to use
   * @param theMode    to use
   * @param converged  true when converged, false otherwise
   * @throws PlanItException thrown when error
   */
  private void persistIterationResults(TimePeriod timePeriod, Mode theMode, boolean converged) throws PlanItException {
    Set<Mode> modes = Set.of(theMode);
    if (getOutputManager().isAnyOutputPersisted(timePeriod, modes, converged)) {
      assignmentStrategy.getLoading().populateForPersistence();
      getOutputManager().persistOutputData(timePeriod, modes, converged);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility() throws PlanItException {
    super.verifyComponentCompatibility();

    /* gap function check */
    PlanItException.throwIf(!(getGapFunction() instanceof LinkBasedRelativeDualityGapFunction),
        "%sStatic LTM only supports a link based relative gap function (for equilibration) at the moment, but found %s", LoggingUtils.createRunIdPrefix(getId()),
        getGapFunction().getClass().getCanonicalName());

    /* smoothing check */
    PlanItException.throwIf(!(getSmoothing() instanceof MSASmoothing), "%sStatic LTM only supports MSA smoothing at the moment, but found %s",
        LoggingUtils.createRunIdPrefix(getId()), getSmoothing().getClass().getCanonicalName());
  }

  /**
   * Initialise the components before we start any assignment + create the assignment strategy (bush or path based)
   */
  @Override
  protected void initialiseBeforeExecution() throws PlanItException {
    super.initialiseBeforeExecution();
    this.assignmentStrategy = createAssignmentStrategy();
    LOGGER.info(String.format("%sstrategy: %s", LoggingUtils.createRunIdPrefix(getId()), assignmentStrategy.getDescription()));
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
   * Return the assignment solution strategy used
   * 
   * @return
   */
  protected StaticLtmAssignmentStrategy getStrategy() {
    return assignmentStrategy;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public StaticLtm(IdGroupingToken groupId) {
    super(groupId);
    this.settings = new StaticLtmSettings();
  }

  /**
   * Copy Constructor
   * 
   * @param sltm to copy
   */
  public StaticLtm(StaticLtm sltm) {
    super(sltm);
    this.settings = sltm.settings.clone();
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
    return simulationData.getIterationIndex();
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
    return settings.isDisableStorageConstraints();
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public void setDisableLinkStorageConstraints(boolean flag) {
    settings.setDisableStorageConstraints(flag);
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public void setActivateDetailedLogging(boolean flag) {
    settings.setDetailedLogging(flag);
  }

  /**
   * Verify if bush based assignment is applied or not
   * 
   * @return true when activated, false otherwise
   */
  public boolean isActivateBushBased() {
    return settings.isBushBased();
  }

  /**
   * Set the flag indicating if bush based assignment is to be applied or not
   * 
   * @param flag when true activate, when false disable
   */
  public void setActivateBushBased(boolean flag) {
    settings.setBushBased(flag);
  }

  /**
   * Set the flag indicating link storage constraints are active or not
   * 
   * @param flag when true activate, when false disable
   */
  public boolean isActivateDetailedLogging() {
    return settings.isDetailedLogging();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentInflowsPcuHour() {
    return this.assignmentStrategy.getLoading().getCurrentInflowsPcuH();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getLinkSegmentOutflowsPcuHour() {
    return this.assignmentStrategy.getLoading().getCurrentOutflowsPcuH();
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
