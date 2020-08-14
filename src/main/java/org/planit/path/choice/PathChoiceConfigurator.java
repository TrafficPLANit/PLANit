package org.planit.path.choice;

import org.planit.utils.configurator.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all path choice configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class PathChoiceConfigurator<T extends PathChoice> extends Configurator<T> {

  /**
   * Constructor 
   * @param instanceType to configure on
   */
  public PathChoiceConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends Smoothing
   * 
   * @param path choice the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(PathChoice pathChoice) throws PlanItException {
    super.configure((T) pathChoice);
  }
}
