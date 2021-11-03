package org.goplanit.cost.physical;

import org.goplanit.utils.exceptions.PlanItException;

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
   * @throws PlanItException thrown if error
   */
  public static PhysicalCostConfigurator<? extends AbstractPhysicalCost> createConfigurator(final String physicalCostType) throws PlanItException {

    if (physicalCostType.equals(AbstractPhysicalCost.BPR)) {
      return new BPRConfigurator();
    } else if (physicalCostType.equals(AbstractPhysicalCost.FREEFLOW)) {
      return new FreeFlowLinkTravelTimeConfigurator();
    } else if (physicalCostType.equals(AbstractPhysicalCost.STEADY_STATE)) {
      return new SteadyStateTravelTimeConfigurator();
    } else {
      // TODO when not explicitly available try to instantiate using passed in string and reflection instead
      throw new PlanItException(String.format("Unable to construct configurator for given PhysicalCostType %s", physicalCostType));
    }
  }
}
