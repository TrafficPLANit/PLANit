package org.planit.cost.physical;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all physical cost configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class PhysicalCostConfigurator<T extends AbstractPhysicalCost> extends Configurator<T> {

  /**
   * Constructor 
   * @param instanceType to configure on
   */
  public PhysicalCostConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends PhysicalCost
   * 
   * @param physicalCost the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(AbstractPhysicalCost physicalCost) throws PlanItException {
    super.configure((T) physicalCost);
  }

  
}
