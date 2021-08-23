package org.planit.assignment.ltm.sltm;

import org.planit.algorithms.nodemodel.NodeModel;
import org.planit.assignment.ltm.LtmConfigurator;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.gap.GapFunction;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * <li>Smoothing: MSA</li>
 * <li>Gap function: NORM BASED (defaults: 1 norm + averaged))</li>
 * <li>Physical Cost: FREEFLOW</li>
 * <li>Virtual Cost: FIXED</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class StaticLtmConfigurator extends LtmConfigurator<StaticLtm> {

  private static final String DISABLE_LINK_STORAGE_CONSTRAINTS = "setDisableLinkStorageConstraints";

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public StaticLtmConfigurator() throws PlanItException {
    super(StaticLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
    createAndRegisterGapFunction(GapFunction.NORM_BASED_GAP);
    createAndRegisterSmoothing(Smoothing.MSA);
    createAndRegisterPhysicalCost(PhysicalCost.FREEFLOW); // TODO: change into STATIC_EXITFLOW (which would allow for either path or link based setting)
    createAndRegisterVirtualCost(VirtualCost.FIXED);
  }

  //
  // Directly configurable options
  //

  /**
   * Disable enforcing any storage constraints on link(segments)
   * 
   */
  public void disableLinkStorageConstraints() {
    registerDelayedMethodCall(DISABLE_LINK_STORAGE_CONSTRAINTS, true);
  }

}
