package org.goplanit.cost.virtual;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

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
   */
  public static VirtualCostConfigurator<? extends AbstractVirtualCost> createConfigurator(
          final String virtualCostType){

    if (virtualCostType.equals(VirtualCost.FIXED)) {
      return new FixedVirtualCostConfigurator();
    }else if(virtualCostType.equals(VirtualCost.SPEED)) {
      return new SpeedVirtualCostConfigurator();
    }else {
      throw new PlanItRunTimeException(
              String.format("unable to construct configurator for given virtualCostType %s", virtualCostType));
    }
  }
}
