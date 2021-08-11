package org.planit.assignment.ltm.sltm;

import org.planit.algorithms.nodemodel.NodeModel;
import org.planit.assignment.ltm.LtmConfigurator;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator for sLTM. Adopting the following defaults:
 * 
 * <ul>
 * <li>Fundamental diagram: NEWELL</li>
 * <li>Node Model: TAMPERE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class StaticLtmConfigurator extends LtmConfigurator<StaticLtm> {

  private static final String DISABLE_LINK_STORAGE_CONSTRAINTS = "disableLinkStorageConstraints";

  /**
   * Constructor
   * 
   * @throws PlanItException thrown when error
   */
  public StaticLtmConfigurator() throws PlanItException {
    super(StaticLtm.class);
    createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
    createAndRegisterNodeModel(NodeModel.TAMPERE);
  }

  //
  // Directly configurable options
  //

  /**
   * Disable enforcing any storage constraints on link(segments)
   * 
   */
  public void disableLinkStorageConstraints() {
    registerDelayedMethodCall(DISABLE_LINK_STORAGE_CONSTRAINTS);
  }

}
