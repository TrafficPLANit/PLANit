package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.ltm.LtmConfigurator;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.virtual.VirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.PathChoiceConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.Pair;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * <li>Smoothing: MSA</li>
 * <li>Gap function: PATH_BASED</li>
 * <li>Physical Cost: STEADY_STATE</li>
 * <li>Virtual Cost: FIXED</li>
 * </ul>
 * Further the following other settings have the defaults:
 * <ul>
 * <li>disableLinkStorageConstraints: true</li>
 * <li>activateDetailedLogging: false</li>
 * <li>activateEnforceMaxEntropyFlowDistribution: false</li>
 * <li>allowOverlappingPasUpdate: true</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class StaticLtmConfigurator extends LtmConfigurator<StaticLtm> {

  private static final Logger LOGGER = Logger.getLogger(StaticLtmConfigurator.class.getCanonicalName());

  private static final String DISABLE_LINK_STORAGE_CONSTRAINTS = "setDisableLinkStorageConstraints";

  private static final String ACTIVATE_DETAILED_LOGGING = "setActivateDetailedLogging";

  private static final String SET_NETWORKLOADING_FLOW_ACCEPTANCE_GAP_EPSILON = "setNetworkLoadingFlowAcceptanceGapEpsilon";

  private static final String SET_NETWORKLOADING_SENDING_FLOW_GAP_EPSILON = "setNetworkLoadingSendingFlowGapEpsilon";

  private static final String SET_NETWORKLOADING_RECEIVING_FLOW_GAP_EPSILON = "setNetworkLoadingReceivingFlowGapEpsilon";

  private static final String SET_NETWORKLOADING_INITIAL_SCHEME = "setNetworkLoadingInitialScheme";

  private static final String SET_TYPE = "setType";

  private static final String ADD_TRACK_OD_FOR_LOGGING_ID = "addTrackOdForLoggingById";

  private static final String ADD_TRACK_OD_FOR_LOGGING_XML_ID = "addTrackOdForLoggingByXmlId";

  private static final String ADD_TRACK_OD_FOR_LOGGING_EXTERNAL_ID = "addTrackOdForLoggingByExternalId";

  // ---------------------- PATH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  private static final String SET_DISABLE_PATH_GENERATION_AFTER_ITERATION = "setDisablePathGenerationAfterIteration";

  private static final String SET_ACTIVATE_RELATIVE_SCALING_FACTOR = "setActivateRelativeScalingFactor";

  public static final String SET_DISABLE_RELATIVE_SCALING_FACTOR_UPDATE_AFTER_ITERATION = "setDisableRelativeScalingFactorUpdateAfterIteration";

  // ---------------------- BUSH BASED ONLY OPTIONS ----------------------------------------------------
  // todo: refactor in separate implementation proper at some point

  private static final String ACTIVATE_ENFORCE_MAX_ENTROPY_FLOW_DISTRIBUTION = "setEnforceMaxEntropyFlowSolution";

  private static final String ALLOW_OVERLAPPING_PAS_UPDATE = "setAllowOverlappingPasUpdate";

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public StaticLtmConfigurator() throws PlanItException {
    super(StaticLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
    createAndRegisterGapFunction(GapFunction.PATH_BASED_GAP);
    createAndRegisterSmoothing(Smoothing.MSA);
    createAndRegisterPhysicalCost(PhysicalCost.STEADY_STATE);
    createAndRegisterVirtualCost(VirtualCost.FIXED);

    disableLinkStorageConstraints(DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
    activateDetailedLogging(DEFAULT_ACTIVATE_DETAILED_LOGGING);
    setNetworkLoadingFlowAcceptanceGapEpsilon(StaticLtmSettings.DEFAULT_NETWORK_LOADING_GAP_EPSILON);
    setNetworkLoadingSendingFlowGapEpsilon(StaticLtmSettings.DEFAULT_NETWORK_LOADING_GAP_EPSILON);
    setNetworkLoadingReceivingFlowGapEpsilon(StaticLtmSettings.DEFAULT_NETWORK_LOADING_GAP_EPSILON);
    setAllowOverlappingPasUpdate(StaticLtmSettings.DEFAULT_ALLOW_OVERLAPPING_PAS_UPDATE);

    setType(DEFAULT_SLTM_TYPE);
    createAndRegisterPathChoice(PathChoice.STOCHASTIC);
  }

  /** default value used */
  public static boolean DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS = true;

  /** default value used */
  public static boolean DEFAULT_ACTIVATE_DETAILED_LOGGING = false;

  /** default value used */
  public static StaticLtmType DEFAULT_SLTM_TYPE = StaticLtmSettings.DEFAULT_SLTM_TYPE;

  //
  // Directly configurable options
  //

  /**
   * Dis-or enable enforcing any storage constraints on link(segments)
   *
   * @param flag to set
   */
  public void disableLinkStorageConstraints(boolean flag) {
    registerDelayedMethodCall(DISABLE_LINK_STORAGE_CONSTRAINTS, flag);
  }

  /**
   * (De)Activate additional detailed logging specifically for the sLTM assignment procedure
   * 
   * @param flag to set
   */
  public void activateDetailedLogging(boolean flag) {
    registerDelayedMethodCall(ACTIVATE_DETAILED_LOGGING, flag);
  }

  /**
   * Collect the gap epsilon set for network loading iterative sub procedure for determining the flow acceptance factors
   *
   * @return epsilon
   */
  public Double getNetworkLoadingFlowAcceptanceGapEpsilon() {
    return getTypedFirstParameterOfDelayedMethodCall(SET_NETWORKLOADING_FLOW_ACCEPTANCE_GAP_EPSILON);
  }

  /**
   * Set the gap epsilon set for network loading iterative sub procedure for determining the flow acceptance factors
   *
   * @param networkLoadingFlowAcceptanceGapEpsilon to use
   */
  public void setNetworkLoadingFlowAcceptanceGapEpsilon(Double networkLoadingFlowAcceptanceGapEpsilon) {
    registerDelayedMethodCall(SET_NETWORKLOADING_FLOW_ACCEPTANCE_GAP_EPSILON, networkLoadingFlowAcceptanceGapEpsilon);
  }

  /**
   * Collect the gap epsilon set for network loading iterative sub procedure for determining the sending flows
   *
   * @return epsilon
   */
  public Double getNetworkLoadingSendingFlowGapEpsilon() {
    return getTypedFirstParameterOfDelayedMethodCall(SET_NETWORKLOADING_SENDING_FLOW_GAP_EPSILON);
  }

  /**
   * Set the gap epsilon set for network loading iterative sub procedure for determining the sending flows
   *
   * @param networkLoadingSendingFlowGapEpsilon to use
   */
  public void setNetworkLoadingSendingFlowGapEpsilon(Double networkLoadingSendingFlowGapEpsilon) {
    registerDelayedMethodCall(SET_NETWORKLOADING_SENDING_FLOW_GAP_EPSILON, networkLoadingSendingFlowGapEpsilon);
  }

  /**
   * Collect the gap epsilon set for network loading iterative sub procedure for determining the receiving flows
   *
   * @return epsilon
   */
  public Double getNetworkLoadingReceivingFlowGapEpsilon() {
    return getTypedFirstParameterOfDelayedMethodCall(SET_NETWORKLOADING_RECEIVING_FLOW_GAP_EPSILON);
  }

  /**
   * Set the gap epsilon set for network loading iterative sub procedure for determining the receiving flows
   *
   * @param networkLoadingReceivingFlowGapEpsilon to use
   */
  public void setNetworkLoadingReceivingFlowGapEpsilon(Double networkLoadingReceivingFlowGapEpsilon) {
    registerDelayedMethodCall(SET_NETWORKLOADING_RECEIVING_FLOW_GAP_EPSILON, networkLoadingReceivingFlowGapEpsilon);
  }

  /**
   * Set a specific initial SLTM loading scheme to start with. Should be compatible with the storage constraints approach
   * chosen, i.e., you cannot set it to point queue when storage constraints are active.
   *
   * @param initialScheme scheme to start with
   */
  public void setNetworkLoadingInitialScheme(StaticLtmLoadingScheme initialScheme) {
    registerDelayedMethodCall(SET_NETWORKLOADING_INITIAL_SCHEME, initialScheme);
  }

  /**
   * Get the chosen  initial SLTM loading scheme to start with. If not explicitly overwritten this method returns
   * {@code  StaticLtmLoadingScheme.NONE}, otherwise the value set. If not overwritten the loading will choose the appropriate
   * initial scheme depending on configuration.
   *
   * @return loading scheme if set explicitly
   */
  public StaticLtmLoadingScheme getNetworkLoadingInitialScheme() {
    return (StaticLtmLoadingScheme) getTypedFirstParameterOfDelayedMethodCall(SET_NETWORKLOADING_RECEIVING_FLOW_GAP_EPSILON);
  }

  /**
   * Determine the type of sLTM assignment to use
   * 
   * @param type to set
   */
  public void setType(StaticLtmType type) {
    registerDelayedMethodCall(SET_TYPE, type);
    if(getType() != StaticLtmType.PATH_BASED){
      LOGGER.info(String.format("sLTM type changed to non-path based, unregistering previously registered PathChoice component"));
      unRegisterPathChoice();
    }
  }

  /**
   * Determine the type of sLTM assignment to use
   *
   * @return type set
   */
  public StaticLtmType getType() {
    return (StaticLtmType) getFirstParameterOfDelayedMethodCall(SET_TYPE);
  }

  /**
   * (De)Activate the bush based max entropy flow solution. If switched off, any equal cost solution for a PAS is considered correct, when switched on an attempt is made to obtain
   * unique equal flow distribution if possible. The latter is computationally more costly but results in unique solution.
   * <p>
   *   Only relevant for bush-based approach
   * </p>
   *
   * @param flag to set
   */
  public void activateMaxEntropyFlowDistribution(boolean flag) {
    registerDelayedMethodCall(ACTIVATE_ENFORCE_MAX_ENTROPY_FLOW_DISTRIBUTION, flag);
  }

  /**
   * Provide OD pairs to track extended logging for during assignment for debugging purposes.
   * (currently only supported for path based sLTM assignment)
   *
   * @param <T> type of ids
   * @param idType type of id the ods relate to
   * @param odPairs od pairs array based on chosen id type
   */
  public <T> void addTrackOdsForLogging(IdMapperType idType, Pair<T,T>... odPairs){
    String delayedCall;
    switch (idType) {
      case ID:
        delayedCall = ADD_TRACK_OD_FOR_LOGGING_ID;
        break;
      case XML:
        delayedCall = ADD_TRACK_OD_FOR_LOGGING_XML_ID;
        break;
      case EXTERNAL_ID:
        delayedCall = ADD_TRACK_OD_FOR_LOGGING_EXTERNAL_ID;
        break;
      default:
        throw new PlanItRunTimeException("Unrecognised id type for tracking ods");
    }
    Arrays.stream(odPairs).forEach( p -> registerDelayedMethodCall(delayedCall, p.first(), p.second()));
  }

  /**
   * the iteration after which we disable path generation
   * <p>
   * Path based only option
   * </p>
   * @return disablePathGenerationAfterIteration iteration number used
   */
  public Integer getDisablePathGenerationAfterIteration() {
    return (Integer) getFirstParameterOfDelayedMethodCall(SET_DISABLE_PATH_GENERATION_AFTER_ITERATION);
  }

  /**
   * choose iteration after which we disable path generation, if not set default is applied
   * <p>
   * Path based only option
   * </p>
   * @param disablePathGenerationAfterIteration choose iteration after which we disable path generation
   */
  public void setDisablePathGenerationAfterIteration(Integer disablePathGenerationAfterIteration) {
    registerDelayedMethodCall(SET_DISABLE_PATH_GENERATION_AFTER_ITERATION, disablePathGenerationAfterIteration);
  }

  /**
   * Choose whether scaling factor of path choice is to be made relative to the minimum cost
   * on each OD
   * <p>
   * Path based only option
   * </p>
   * @param flag to set
   */
  public void setActivateRelativeScalingFactor(Boolean flag){
    registerDelayedMethodCall(SET_ACTIVATE_RELATIVE_SCALING_FACTOR, flag);
  }

  /**
   * Check whether scaling factor of path choice is to be made relative to the minimum cost
   * on each OD
   * <p>
   * Path based only option
   * </p>
   * @return flag set
   */
  public Boolean isActivateRelativeScalingFactor(){
    return (Boolean) getFirstParameterOfDelayedMethodCall(SET_ACTIVATE_RELATIVE_SCALING_FACTOR);
  }

  /**
   * If relative scaling factor is active, disable updating it each iteration after the given iteration
   * <p>
   * Path based only option
   * </p>
   * @param disableRelativeScalingFactorUpdateAfterIteration iteration to disable relative scaling factor update after
   */
  public void setDisableRelativeScalingFactorUpdateAfterIteration(Integer disableRelativeScalingFactorUpdateAfterIteration){
    registerDelayedMethodCall(SET_DISABLE_RELATIVE_SCALING_FACTOR_UPDATE_AFTER_ITERATION, disableRelativeScalingFactorUpdateAfterIteration);
  }

  /**
   * Check setting regarding when relative scaling factor update would be disabled if active
   * <p>
   * Path based only option
   * </p>
   * @return iteration set
   */
  public Integer getDisableRelativeScalingFactorUpdateAfterIteration(){
    return (Integer) getFirstParameterOfDelayedMethodCall(SET_DISABLE_RELATIVE_SCALING_FACTOR_UPDATE_AFTER_ITERATION);
  }

  /**
   * Check setting regarding regarding flag on overlapping pas updates
   * <p>
   * Bush based only option
   * </p>
   * @return flag set
   */
  public boolean isAllowOverlappingPasUpdate() {
    return (Boolean) getFirstParameterOfDelayedMethodCall(ALLOW_OVERLAPPING_PAS_UPDATE);
  }

  /**
   * Set setting regarding regarding flag on overlapping pas updates
   * <p>
   * Bush based only option
   * </p>
   * @param flag set
   */
  public void setAllowOverlappingPasUpdate(Boolean flag) {
    registerDelayedMethodCall(ALLOW_OVERLAPPING_PAS_UPDATE, flag);
  }

  /**
   * choose a particular path choice implementation
   *
   * @param pathChoiceType type to choose
   * @return path choice configurator
   * @throws PlanItException thrown if error
   */
  @Override
  public PathChoiceConfigurator<? extends PathChoice> createAndRegisterPathChoice(final String pathChoiceType) throws PlanItException {
    if(getType() != StaticLtmType.PATH_BASED){
      setType(StaticLtmType.PATH_BASED);
      LOGGER.info(String.format("PathChoice activated, this requires sLTM type to be path based, switching to type: %s", getType()));
    }
    return super.createAndRegisterPathChoice(pathChoiceType);
  }


}
