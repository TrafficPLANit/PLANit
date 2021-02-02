package org.planit.gap;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all vgap function configurator implementations
 * 
 * @author markr
 *
 * @param <T> gap function type
 */
public class GapFunctionConfigurator<T extends GapFunction> extends Configurator<T> {

  /**
   * the configurator for the stop critetion
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
  public void configure(GapFunction gapFunction) throws PlanItException {
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
