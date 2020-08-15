package org.planit.gap;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all stop criterion configurator implementations
 * 
 * @author markr
 *
 * @param <T>
 */
public class StopCriterionConfigurator extends Configurator<StopCriterion> {

  public static final String SET_MAX_ITERATIONS = "setMaxIterations";

  public static final String SET_EPSILON = "setEpsilon";

  /**
   * Constructor
   * 
   */
  public StopCriterionConfigurator() {
    super(StopCriterion.class);

    /* set defaults */
    setMaxIterations(StopCriterion.DEFAULT_MAX_ITERATIONS);
    setEpsilon(StopCriterion.DEFAULT_EPSILON);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends GapFunction
   * 
   * @param stopCriterion the instance to configure
   */
  @Override
  public void configure(StopCriterion stopCriterion) throws PlanItException {
    super.configure((StopCriterion) stopCriterion);
  }

  /**
   * Return the maximum allowable number of iterations
   * 
   * @return the maximum allowable number of iterations
   */
  public int getMaxIterations() {
    return (int) getFirstParameterOfDelayedMethodCall(SET_MAX_ITERATIONS);
  }

  /**
   * Set the maximum allowable number of iterations
   * 
   * @param maxIterations the maximum allowable number of iterations
   */
  public void setMaxIterations(int maxIterations) {
    registerDelayedMethodCall(SET_MAX_ITERATIONS, maxIterations);
  }

  /**
   * Return the epsilon of this stopping criterion
   * 
   * @return the epsilon of this stopping criterion
   */
  public double getEpsilon() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_EPSILON);
  }

  /**
   * Set the epsilon of this stopping criterion
   * 
   * @param epsilon the epsilon of this stopping criterion
   */
  public void setEpsilon(double epsilon) {
    registerDelayedMethodCall(SET_EPSILON, epsilon);
  }

}
