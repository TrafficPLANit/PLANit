package org.goplanit.cost.physical;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all physical cost configurator implementations
 * 
 * @author markr
 *
 * @param <T> abstract physical cost type
 */
public class PhysicalCostConfigurator<T extends AbstractPhysicalCost> extends Configurator<T> {

  /**
   * Constructor
   * 
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
  public void configure(AbstractPhysicalCost physicalCost) {
    super.configure((T) physicalCost);
  }

}
