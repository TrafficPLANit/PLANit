package org.planit.cost.virtual;

import org.planit.utils.configurator.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all virtual cost configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class VirtualCostConfigurator<T extends VirtualCost> extends Configurator<T> {

  /**
   * Constructor 
   * @param instanceType to configure on
   */
  public VirtualCostConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends VirtualCost
   * 
   * @param virtualCost the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(VirtualCost virtualCost) throws PlanItException {
    super.configure((T) virtualCost);
  }

  
}
