package org.goplanit.cost.virtual;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all virtual cost configurator implementations
 * 
 * @author markr
 *
 * @param <T> abstract virtual cost type
 */
public class VirtualCostConfigurator<T extends AbstractVirtualCost> extends Configurator<T> {

  /**
   * Constructor
   * 
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
  public void configure(AbstractVirtualCost virtualCost) {
    super.configure((T) virtualCost);
  }

}
