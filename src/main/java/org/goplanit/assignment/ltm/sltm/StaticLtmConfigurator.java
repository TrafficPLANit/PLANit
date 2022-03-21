package org.goplanit.assignment.ltm.sltm;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.assignment.ltm.LtmConfigurator;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.virtual.VirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * <li>Smoothing: MSA</li>
 * <li>Gap function: NORM BASED (defaults: 1 norm + averaged))</li>
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

  private static final String DISABLE_LINK_STORAGE_CONSTRAINTS = "setDisableLinkStorageConstraints";

  private static final String ACTIVATE_DETAILED_LOGGING = "setActivateDetailedLogging";

  private static final String ACTIVATE_BUSH_BASED = "setActivateBushBased";

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
    createAndRegisterGapFunction(GapFunction.LINK_BASED_RELATIVE_GAP);
    createAndRegisterSmoothing(Smoothing.MSA);
    createAndRegisterPhysicalCost(PhysicalCost.STEADY_STATE);
    createAndRegisterVirtualCost(VirtualCost.FIXED);

    disableLinkStorageConstraints(DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
    activateDetailedLogging(DEFAULT_ACTIVATE_DETAILED_LOGGING);
    activateBushBased(DEFAULT_ACTIVATE_BUSH_BASED);
  }

  /** default value used */
  public static boolean DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS = true;

  /** default value used */
  public static boolean DEFAULT_ACTIVATE_DETAILED_LOGGING = false;

  /** default value used */
  public static boolean DEFAULT_ACTIVATE_BUSH_BASED = true;

  //
  // Directly configurable options
  //

  /**
   * Dis or enable enforcing any storage constraints on link(segments)
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
   * (De)Activate the bush based assignment strategy. If switched off, a path absed approach is applied
   * 
   * @param flag to set
   */
  public void activateBushBased(boolean flag) {
    registerDelayedMethodCall(ACTIVATE_BUSH_BASED, flag);
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
