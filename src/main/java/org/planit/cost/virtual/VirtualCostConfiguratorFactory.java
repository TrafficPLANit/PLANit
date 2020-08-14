package org.planit.cost.virtual;

import org.planit.utils.exceptions.PlanItException;

/**
 * factory for the virtual cost types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class VirtualCostConfiguratorFactory {

  /**
   * Create a configurator for given virtual cost type
   * 
   * @param virtualCostType   type of assignment the builder is created for
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static VirtualCostConfigurator<? extends VirtualCost> createConfigurator(final String virtualCostType) throws PlanItException {

    if (virtualCostType.equals(VirtualCost.FIXED)) {
      return new FixedVirtualCostConfigurator();
    }else if(virtualCostType.equals(VirtualCost.SPEED)) {
      return new SpeedVirtualCostConfigurator();
    }else {
      throw new PlanItException(String.format("unable to construct configurator for given virtualCostType %s", virtualCostType));
    }
  }
}
