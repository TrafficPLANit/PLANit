package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.ltm.LtmConfigurator;
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

  private static final String SET_TYPE = "setType";

  private static final String ACTIVATE_ENFORCE_MAX_ENTROPY_FLOW_DISTRIBUTION = "setEnforceMaxEntropyFlowSolution";

  private static final String ADD_TRACK_OD_FOR_LOGGING_ID = "addTrackOdForLoggingById";

  private static final String ADD_TRACK_OD_FOR_LOGGING_XML_ID = "addTrackOdForLoggingByXmlId";

  private static final String ADD_TRACK_OD_FOR_LOGGING_EXTERNAL_ID = "addTrackOdForLoggingByExternalId";

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
    registerDelayedMethodCall(SET_NETWORKLOADING_SENDING_FLOW_GAP_EPSILON, networkLoadingReceivingFlowGapEpsilon);
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

}
