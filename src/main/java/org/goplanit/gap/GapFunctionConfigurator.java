package org.goplanit.gap;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all gap function configurator implementations
 * 
 * @author markr
 *
 * @param <T> gap function type
 */
public class GapFunctionConfigurator<T extends GapFunction> extends Configurator<T> {

  /**
   * the configurator for the stop criterion
   */
  protected final StopCriterionConfigurator stopCriterionConfigurator;

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public GapFunctionConfigurator(Class<T> instanceType) {
    super(instanceType);
    this.stopCriterionConfigurator = new StopCriterionConfigurator();
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends GapFunction
   * 
   * @param gapFunction the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(GapFunction gapFunction) {
    super.configure((T) gapFunction);
  }

  /**
   * Return the StopCriterion object
   * 
   * @return StopCriterion object being used
   */
  public StopCriterionConfigurator getStopCriterion() {
    return stopCriterionConfigurator;
  }

}
