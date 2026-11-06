package org.goplanit.cost.virtual;

import org.goplanit.cost.physical.PhysicalCostConfigurator;
import org.goplanit.cost.physical.SteadyStateTravelTimeCost;

/**
 * Configurator for SteadyStateConnectoidTravelTimeCost implementation
 * 
 * @author markr
 */
public class SteadyStateVirtualCostConfigurator extends VirtualCostConfigurator<SteadyStateConnectoidTravelTimeCost> {

  /**
   * Constructor
   *
   */
  protected SteadyStateVirtualCostConfigurator() {
    super(SteadyStateConnectoidTravelTimeCost.class);
  }

  /* currently no options available */

}
