package org.planit.gap;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all vgap function configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class GapFunctionConfigurator<T extends GapFunction> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public GapFunctionConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends GapFunction
   * 
   * @param gapFunction the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(GapFunction gapFunction) throws PlanItException {
    super.configure((T) gapFunction);
  }

}
