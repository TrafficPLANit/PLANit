package org.goplanit.path.choice;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all path choice configurator implementations
 * 
 * @author markr
 *
 * @param <T> path choice type
 */
public class PathChoiceConfigurator<T extends PathChoice> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public PathChoiceConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends path choice
   * 
   * @param pathChoice the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(PathChoice pathChoice){
    super.configure((T) pathChoice);
  }
}
