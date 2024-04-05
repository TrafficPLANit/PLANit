package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.ltm.LtmConfigurator;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.virtual.VirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.PathChoiceConfigurator;
import org.goplanit.path.choice.PathChoiceConfiguratorFactory;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;

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

  private static final String SET_TYPE = "setType";

  private static final String ACTIVATE_ENFORCE_MAX_ENTROPY_FLOW_DISTRIBUTION = "setEnforceMaxEntropyFlowSolution";

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
   * 
   * @param flag to set
   */
  public void activateMaxEntropyFlowDistribution(boolean flag) {
    registerDelayedMethodCall(ACTIVATE_ENFORCE_MAX_ENTROPY_FLOW_DISTRIBUTION, flag);
  }

}
