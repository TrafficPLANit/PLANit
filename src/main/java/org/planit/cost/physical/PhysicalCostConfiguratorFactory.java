package org.planit.cost.physical;

import org.planit.utils.exceptions.PlanItException;

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
   * @param physicalCostType   type of assignment the builder is created for
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static PhysicalCostConfigurator<? extends PhysicalCost> createConfigurator(final String physicalCostType) throws PlanItException {

    if (physicalCostType.equals(PhysicalCost.BPR)) {
      return new BPRConfigurator();
    } else {
      throw new PlanItException(String.format("unable to construct configurator for given physicalCostType %s", physicalCostType));
    }
  }
}
