package org.goplanit.assignment.ltm.sltm;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.LtmAssignment;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmStrategyConjugateBush;
import org.goplanit.gap.GapFunction;
import org.goplanit.interactor.LinkInflowOutflowAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.sdinteraction.smoothing.IterationBasedSmoothing;
import org.goplanit.sdinteraction.smoothing.MSRASmoothing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.reflection.ReflectionUtils;
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
   * @return created strategy, null if unsupported type is set
   */
  private StaticLtmAssignmentStrategy createAssignmentStrategy() {
    /* create the assignment solution to apply */
    switch (settings.getSltmType()) {
      case ORIGIN_BUSH_BASED:
        return new StaticLtmOriginBushDestLabelledStrategy(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
      case DESTINATION_BUSH_BASED:
        return new StaticLtmDestinationBushStrategy(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
      case CONJUGATE_DESTINATION_BUSH_BASED:
        return new StaticLtmStrategyConjugateBush(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
      case PATH_BASED:
        return new StaticLtmPathStrategy(getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);        
      default:
        LOGGER.warning(String.format("Unsupported static LTM type chosen %s, aborting",settings.getSltmType()));
        return null;
    }
  }

  /**
   * Record some basic iteration information such as duration and gap
   *
   * @param startTime  the original start time of the iteration
   * @param gapFunction the duality gap at the end of the iteration
   * @return the time (in ms) at the end of the iteration for profiling purposes only
   */
  private Calendar logBasicIterationInformation(final Calendar startTime, final GapFunction gapFunction) {
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
    var simulationData = new StaticLtmSimulationData(timePeriod, List.of(mode), getTotalNumberOfNetworkSegments());
    simulationData.setLinkSegmentTravelTimePcuH(mode, initialLinkSegmentCosts);

    /* create initial solution as starting point for equilibration */
    assignmentStrategy.createInitialSolution(initialLinkSegmentCosts, simulationData.getIterationIndex());

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
      LOGGER.warning(
          String.format("%ssLTM only supports a single mode for now, found %s, aborting assignment for time period %s", LoggingUtils.runIdPrefix(getId()), timePeriod.getXmlId()));
      return;
    }

    /* prep */
    Mode theMode = modes.iterator().next();
    this.simulationData = initialiseTimePeriod(timePeriod, theMode, getDemands().get(theMode, timePeriod));

    boolean convergedOrStop = false;
    Calendar iterationStartTime = Calendar.getInstance();

    /* ASSIGNMENT LOOP */
    do {
      simulationData.incrementIterationIndex();
      var smoothing = getSmoothing();
      if (smoothing instanceof IterationBasedSmoothing) {
        ((IterationBasedSmoothing) smoothing).updateIteration(simulationData.getIterationIndex());
        if(smoothing instanceof MSRASmoothing) {
          ((MSRASmoothing)smoothing).updateBadIteration(getGapFunction().getPreviousGap(), getGapFunction().getGap());
        }
        smoothing.updateStepSize();
      }
      getGapFunction().resetIteration();
      assignmentStrategy.getLoading().resetIteration();

      /* LOADING UPDATE + PATH/BUSH UPDATE */
      double[] prevCosts = getIterationData().getLinkSegmentTravelTimePcuH(theMode);
      double[] costsToUpdate = Arrays.copyOf(prevCosts, prevCosts.length);
      boolean success = assignmentStrategy.performIteration(theMode, prevCosts, costsToUpdate, simulationData.getIterationIndex());
      if (!success) {
        LOGGER.severe("Unable to continue PLANit sLTM run, aborting");
        break;
      }
      // COST UPDATE
      getIterationData().setLinkSegmentTravelTimePcuH(theMode, costsToUpdate);

      // CONVERGENCE CHECK
      convergedOrStop = assignmentStrategy.hasConverged(getGapFunction(), simulationData.getIterationIndex());

      // PERSIST
      persistIterationResults(timePeriod, theMode, convergedOrStop);

      iterationStartTime = logBasicIterationInformation(iterationStartTime, getGapFunction());
    } while (!convergedOrStop);

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
    var modes = Set.of(theMode);
    if (getOutputManager().isAnyOutputPersisted(timePeriod, modes, converged)) {
      assignmentStrategy.getLoading().stepSixFinaliseForPersistence();
      getOutputManager().persistOutputData(timePeriod, modes, converged);

      if(isActivateDetailedLogging()) {
        LOGGER.info(String.format("** INFLOW: %s", Arrays.toString(assignmentStrategy.getLoading().getCurrentInflowsPcuH())));
        LOGGER.info(String.format("** OUTFLOW: %s", Arrays.toString(assignmentStrategy.getLoading().getCurrentOutflowsPcuH())));
        LOGGER.info(String.format("** ALPHA: %s", Arrays.toString(assignmentStrategy.getLoading().getCurrentFlowAcceptanceFactors())));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void verifyComponentCompatibility(){
    super.verifyComponentCompatibility();

  }

  /**
   * Initialise the components before we start any assignment + create the assignment strategy (bush or path based)
   */
  @Override
  protected void initialiseBeforeExecution() throws PlanItException {
    super.initialiseBeforeExecution();
    this.assignmentStrategy = createAssignmentStrategy();
    assignmentStrategy.verifyComponentCompatibility();
    LOGGER.info(String.format("%sstrategy: %s", LoggingUtils.runIdPrefix(getId()), assignmentStrategy.getDescription()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeEquilibration() throws PlanItException {
    // perform assignment per period
    final var timePeriods = getDemands().timePeriods.asSortedSetByStartTime();
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + "total time periods: " + timePeriods.size());
    for (final TimePeriod timePeriod : timePeriods) {
      Calendar startTime = Calendar.getInstance();
      final Calendar initialStartTime = startTime;
      LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.timePeriodPrefix(timePeriod) + timePeriod.toString());
      executeTimePeriod(timePeriod, getDemands().getRegisteredModesForTimePeriod(timePeriod));
      LOGGER.info(LoggingUtils.runIdPrefix(getId()) + String.format("run time: %d milliseconds", startTime.getTimeInMillis() - initialStartTime.getTimeInMillis()));
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
   * @return used assignment strategy
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
    super(sltm, false);
    this.settings = sltm.settings.shallowClone();
    this.simulationData = sltm.simulationData.shallowClone();
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
      if(settings.getSltmType() != StaticLtmType.PATH_BASED){
        LOGGER.warning("Path output type not available when static LTM assignment is not path based");
      }
      outputTypeAdapter = new StaticLtmPathOutputTypeAdapter(outputType, this);
      break;
    default:
      LOGGER.warning(String.format("%s%s is not supported yet", LoggingUtils.runIdPrefix(getId()), outputType.value()));
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
  public StaticLtm shallowClone() {
    return new StaticLtm(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtm deepClone() {
    throw new PlanItRunTimeException("Deep clone not yet implemented");
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
   * Set the flag indicating what type of bush based assignment is to be applied
   * 
   * @param type to use
   */
  public void setType(StaticLtmType type) {
    settings.setSltmType(type);
  }

  /**
   * Collect the flag indicating link storage constraints are active or not
   * 
   * @return flag when true activated, when false disabled
   */
  public boolean isActivateDetailedLogging() {
    return settings.isDetailedLogging();
  }

  /**
   * Collect the flag indicating to enforce max entropy flow solution is active or not
   * 
   * @return flag when true activated, when false disabled
   */
  public boolean isEnforceMaxEntropyFlowSolution() {
    return settings.isEnforceMaxEntropyFlowSolution();
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingById(Integer originId, Integer destinationId){
    settings.addTrackOdForLoggingById(originId, destinationId);
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingByXmlId(String originId, String destinationId){
    settings.addTrackOdForLoggingByXmlId(originId, destinationId);
  }

  /**
   * Provide OD pair to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param originId origin id in idType form
   * @param destinationId origin id in idType form
   */
  public void addTrackOdForLoggingByExternalId(String originId, String destinationId){
    settings.addTrackOdForLoggingByExternalId(originId, destinationId);
  }

  /**
   * Set the flag indicating to enforce max entropy flow solution is active or not
   * 
   * @param enforceMaxEntropyFlowSolution set flag to enforce max entropy solution
   */
  public void setEnforceMaxEntropyFlowSolution(boolean enforceMaxEntropyFlowSolution) {
    settings.setEnforceMaxEntropyFlowSolution(enforceMaxEntropyFlowSolution);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var privateFieldNameValues = ReflectionUtils.declaredFieldsNameValueMap(settings, i -> Modifier.isPrivate(i) && !Modifier.isStatic(i));
    var keyValueMap = new HashMap<String, String>();
    privateFieldNameValues.forEach((k, v) -> keyValueMap.put(k, v.toString()));
    return keyValueMap;
  }

}
