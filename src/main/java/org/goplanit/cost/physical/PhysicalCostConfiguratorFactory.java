package org.goplanit.cost.physical;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * Traffic assignment builder factory for the physical cost types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class PhysicalCostConfiguratorFactory {

  /**
   * Create a configurator for given physical cost type
   * 
   * @param physicalCostType type of assignment the builder is created for
   * @return the created configurator
   */
  public static PhysicalCostConfigurator<? extends AbstractPhysicalCost> createConfigurator(
      final String physicalCostType){

    if (physicalCostType.equals(PhysicalCost.BPR)) {
      return new BPRConfigurator();
    } else if (physicalCostType.equals(PhysicalCost.FREEFLOW)) {
      return new FreeFlowLinkTravelTimeConfigurator();
    } else if (physicalCostType.equals(PhysicalCost.STEADY_STATE)) {
      return new SteadyStateTravelTimeConfigurator();
    } else {
      // TODO when not explicitly available try to instantiate using passed in string and reflection instead
      throw new PlanItRunTimeException(String.format(
          "Unable to construct configurator for given PhysicalCostType %s", physicalCostType));
    }
  }
}
