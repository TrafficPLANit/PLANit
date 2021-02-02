package org.planit.sdinteraction.smoothing;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all smoothing configurator implementations
 * 
 * @author markr
 *
 * @param <T> smoothing type
 */
public class SmoothingConfigurator<T extends Smoothing> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public SmoothingConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends Smoothing
   * 
   * @param smoothing the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(Smoothing smoothing) throws PlanItException {
    super.configure((T) smoothing);
  }

}
