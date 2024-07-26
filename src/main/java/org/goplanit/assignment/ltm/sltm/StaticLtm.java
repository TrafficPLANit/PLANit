package org.goplanit.assignment.ltm.sltm;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.LtmAssignment;
import org.goplanit.assignment.ltm.sltm.conjugate.StaticLtmStrategyConjugateBush;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.cost.CostUtils;
import org.goplanit.gap.GapFunction;
import org.goplanit.interactor.LinkInflowOutflowAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.sdinteraction.smoothing.IterationBasedSmoothing;
import org.goplanit.sdinteraction.smoothing.MSRASmoothing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
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
    StaticLtmAssignmentStrategy strategy;
    switch (settings.getSltmType()) {
      case ORIGIN_BUSH_BASED:
        strategy =  new StaticLtmOriginBushDestLabelledStrategy(
                getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
        break;
      case DESTINATION_BUSH_BASED:
        strategy =  new StaticLtmDestinationBushStrategy(
                getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
        break;
      case CONJUGATE_DESTINATION_BUSH_BASED:
        strategy =  new StaticLtmStrategyConjugateBush(
                getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
        break;
      case PATH_BASED:
        strategy = new StaticLtmPathStrategy(
                getIdGroupingToken(), getId(), getTransportNetwork(), settings, this);
        break;
      default:
        LOGGER.warning(String.format("Unsupported static LTM type chosen %s, aborting",settings.getSltmType()));
        strategy = null;
    }

    return strategy;
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
   * @param modes    used in this time period
   * @return simulationData initialised for time period
   */
  private StaticLtmSimulationData initialiseTimePeriodSpecificData(TimePeriod timePeriod, final Set<Mode> modes) {

    /* register new time period on costs */
    getPhysicalCost().updateTimePeriod(timePeriod);
    getVirtualCost().updateTimePeriod(timePeriod);

    var simulationData = new StaticLtmSimulationData(timePeriod, modes, getTotalNumberOfNetworkSegments());
    assignmentStrategy.updateTimePeriod(timePeriod, modes, getDemands());


    /* for now only a single mode is supported (although written for more), todo: https://github.com/TrafficPLANit/PLANit/issues/112 */
    for(var mode : modes){
      /* construct costs on all link segments to start with */

      /* empty entries for all link segments by mode */
      final double[] initialLinkSegmentCosts = CostUtils.createEmptyLinkSegmentCostArray(getInfrastructureNetwork(), getZoning());
      /* virtual component */
      CostUtils.populateModalVirtualLinkSegmentCosts(mode, getVirtualCost(), getZoning().getVirtualNetwork(), initialLinkSegmentCosts);
      /* physical component (including initial costs if present)*/
      if(populateWithPhysicalInitialCostIfAvailable(mode, timePeriod, getUsedNetworkLayer().getLinkSegments(), initialLinkSegmentCosts)) {
        LOGGER.info(String.format("%sPrepared sLTM initial costs for traffic assignment time period (%s)",LoggingUtils.runIdPrefix(getId()), timePeriod.getIdsAsString()));
        simulationData.setInitialCostsAppliedInFirstIteration(mode, true);
      }else{
        LOGGER.info(String.format("%sNo initial costs for traffic assignment time period (%s), utilising free flow costs",LoggingUtils.runIdPrefix(getId()), timePeriod.getIdsAsString()));
        CostUtils.populateModalPhysicalLinkSegmentCosts(mode, getPhysicalCost(), getInfrastructureNetwork(), initialLinkSegmentCosts);
      }
      simulationData.setLinkSegmentTravelTimePcuH(mode, initialLinkSegmentCosts);

      /* create initial solution as starting point for equilibration */
      assignmentStrategy.createInitialSolution(
          mode, getZoning().getOdZones(), initialLinkSegmentCosts, simulationData.getIterationIndex());
      LOGGER.info("Created initial solution, proceeding with iterative procedure");
    }

    return simulationData;
  }

  /**
   * Execute for a specific time period
   * 
   * @param timePeriod to execute traffic assignment for
   * @param modes      used for time period
   */
  private void executeTimePeriod(TimePeriod timePeriod, Set<Mode> modes){

    /* prep */
    setTimePeriod(timePeriod);
    this.simulationData = initialiseTimePeriodSpecificData(timePeriod, modes);
    if (simulationData.getSupportedModes().size() != 1) {
      LOGGER.warning(
          String.format("%ssLTM only supports a single mode for now, found %d, aborting assignment for time period %s",
                  LoggingUtils.runIdPrefix(getId()), simulationData.getSupportedModes().size(), timePeriod.getXmlId()));
      return;
    }

    boolean convergedOrStop = false;
    Calendar iterationStartTime = Calendar.getInstance();

    /* ASSIGNMENT LOOP */
    do {
      simulationData.incrementIterationIndex();
      var smoothing = getSmoothing();
      if (smoothing instanceof IterationBasedSmoothing) {
        ((IterationBasedSmoothing) smoothing).updateIteration(simulationData.getIterationIndex());
        if(smoothing instanceof MSRASmoothing) {
          ((MSRASmoothing)smoothing).updateIsBadIteration(getGapFunction().getPreviousGap(), getGapFunction().getGap());
          if(settings.isDetailedLogging() && ((MSRASmoothing)smoothing).isBadIteration()){
            ((MSRASmoothing)smoothing).logStepSize();
          }
        }
        smoothing.updateStepSize();
      }
      getGapFunction().resetIteration();
      assignmentStrategy.getLoading().resetIteration();

      /* LOADING UPDATE + PATH/BUSH UPDATE */
      for(var mode : simulationData.getSupportedModes()) {
        double[] prevCosts = getIterationData().getLinkSegmentTravelTimePcuH(mode);
        double[] costsToUpdate = Arrays.copyOf(prevCosts, prevCosts.length);
        boolean success = assignmentStrategy.performIteration(mode, prevCosts, costsToUpdate, simulationData);
        if (!success) {
          LOGGER.severe("Unable to continue PLANit sLTM run, aborting");
          break;
        }
        // COST UPDATE
        getIterationData().setLinkSegmentTravelTimePcuH(mode, costsToUpdate);
      }

      // CONVERGENCE CHECK
      convergedOrStop = assignmentStrategy.hasConverged(getGapFunction(), simulationData.getIterationIndex());

      // PERSIST
      persistIterationResults(convergedOrStop);

      iterationStartTime = logBasicIterationInformation(iterationStartTime, getGapFunction());
    } while (!convergedOrStop);

  }

  /**
   * Persist the results for this iteration. In case the results require additional actions because the loading has been
   * optimised this is adjusted here before persisting
   * 
   * @param converged  true when converged, false otherwise
   */
  private void persistIterationResults(boolean converged){
    var timePeriod = simulationData.getTimePeriod();
    var modes = simulationData.getSupportedModes();
    if (getOutputManager().isPersistAnyOutput(timePeriod, modes, converged)) {

      assignmentStrategy.getLoading().stepSixFinaliseForPersistence(modes.iterator().next());
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
   *
   * @param resetAndRecreateManagedIds when true, reset and then recreate (and rest) all internal managed ids of transport model network components (links, nodes, connectoids etc.), when false do not.
   */
  @Override
  protected void initialiseBeforeExecution(boolean resetAndRecreateManagedIds) throws PlanItException {
    super.initialiseBeforeExecution(resetAndRecreateManagedIds);

    /* make sure movements have been generated, so we can track data on movement level where it is efficient to do so */
    if(!getTransportNetwork().hasPermissibleMovements()){
      getTransportNetwork().generatePermissibleMovements();
      LOGGER.info(String.format(
              "%sGenerated %d permissible movements",
              LoggingUtils.runIdPrefix(getId()), getTransportNetwork().getMovements().size()));
    }

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
      LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.timePeriodPrefix(timePeriod) + timePeriod);
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
   * Access to the assigment strategy. Only to be used by output adapter to access
   * or create results for persistence
   *
   * @return assignment strategy
   */
  public StaticLtmAssignmentStrategy getAssignmentStrategy(){
    return assignmentStrategy;
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
      outputTypeAdapter = new StaticLtmOdOutputTypeAdapter(outputType, this);
      break;
    case PATH:
      if(settings.getSltmType() != StaticLtmType.PATH_BASED){
        LOGGER.warning("Path output type not available when static LTM assignment is not path based");
      }
      outputTypeAdapter = new StaticLtmPathOutputTypeAdapter(outputType, this);
      break;
    case SIMULATION:
      outputTypeAdapter = new StaticLtmSimulationOutputTypeAdapter(outputType, this);
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

  // CONFIGURATOR GETTERS/SETTERS

  // pass on to settings
  public boolean isDisableLinkStorageConstraints() {
    return settings.isDisableStorageConstraints();
  }

  // pass on to settings
  public void setDisableLinkStorageConstraints(boolean flag) {
    settings.setDisableStorageConstraints(flag);
  }

  // pass on to settings
  public void setActivateDetailedLogging(boolean flag) {
    settings.setDetailedLogging(flag);
  }

  // pass on to settings
  public void setType(StaticLtmType type) {
    settings.setSltmType(type);
  }

  // pass on to settings
  public boolean isActivateDetailedLogging() {
    return settings.isDetailedLogging();
  }

  // pass on to settings
  public boolean isEnforceMaxEntropyFlowSolution() {
    return settings.isEnforceMaxEntropyFlowSolution();
  }

  // pass on to settings
  public void addTrackOdForLoggingById(Integer originId, Integer destinationId){
    settings.addTrackOdForLoggingById(originId, destinationId);
  }

  // pass on to settings
  public void addTrackOdForLoggingByXmlId(String originId, String destinationId){
    settings.addTrackOdForLoggingByXmlId(originId, destinationId);
  }

  // pass on to settings
  public void addTrackOdForLoggingByExternalId(String originId, String destinationId){
    settings.addTrackOdForLoggingByExternalId(originId, destinationId);
  }

  // pass on to settings
  public void setEnforceMaxEntropyFlowSolution(boolean enforceMaxEntropyFlowSolution) {
    settings.setEnforceMaxEntropyFlowSolution(enforceMaxEntropyFlowSolution);
  }

  // pass on to settings
  public void setNetworkLoadingFlowAcceptanceGapEpsilon(Double networkLoadingFlowAcceptanceGapEpsilon) {
    this.settings.setNetworkLoadingFlowAcceptanceGapEpsilon(networkLoadingFlowAcceptanceGapEpsilon);
  }

  // pass on to settings
  public void setNetworkLoadingSendingFlowGapEpsilon(Double networkLoadingSendingFlowGapEpsilon) {
    this.settings.setNetworkLoadingSendingFlowGapEpsilon(networkLoadingSendingFlowGapEpsilon);
  }

  // pass on to settings
  public void setNetworkLoadingReceivingFlowGapEpsilon(Double networkLoadingReceivingFlowGapEpsilon) {
    this.settings.setNetworkLoadingReceivingFlowGapEpsilon(networkLoadingReceivingFlowGapEpsilon);
  }

  // pass on to setting
  public StaticLtmLoadingScheme getNetworkLoadingInitialScheme() {
    return this.settings.getNetworkLoadingInitialScheme();
  }

  // pass on to setting
  public void setNetworkLoadingInitialScheme(StaticLtmLoadingScheme initialSltmLoadingScheme) {
    this.settings.setNetworkLoadingInitialScheme(initialSltmLoadingScheme);
  }

  // pass on to settings
  public Integer getDisablePathGenerationAfterIteration() {
    return this.settings.getDisablePathGenerationAfterIteration();
  }

  // pass on to settings
  public void setDisablePathGenerationAfterIteration(Integer disablePathGenerationAfterIteration) {
    this.settings.setDisablePathGenerationAfterIteration(disablePathGenerationAfterIteration);
  }

  // pass on to settings
  public void setActivateRelativeScalingFactor(Boolean flag){
    this.settings.setActivateRelativeScalingFactor(flag);
  }

  // pass on to settings
  public Boolean isActivateRelativeScalingFactor(){
    return this.settings.isActivateRelativeScalingFactor();
  }

  // pass on to settings
  public void setDisableRelativeScalingFactorUpdateAfterIteration(Integer disableRelativeScalingFactorUpdateAfterIteration){
    this.settings.setDisableRelativeScalingFactorUpdateAfterIteration(disableRelativeScalingFactorUpdateAfterIteration);
  }

  // pass on to settings
  public Integer getDisableRelativeScalingFactorUpdateAfterIteration(){
    return this.settings.getDisableRelativeScalingFactorUpdateAfterIteration();
  }

  // OVERRIDES

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
