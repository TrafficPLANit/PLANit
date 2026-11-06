package org.goplanit.sdinteraction.smoothing;

/**
 * Configurator for Fixed-step smoothing implementation
 * 
 * @author markr
 */
public class FixedStepSmoothingConfigurator extends SmoothingConfigurator<FixedStepSmoothing> {

  /** method name for setting step size on actual instance */
  public static final String SET_STEP_SIZE = "setStepSize";

  /**
   * Constructor
   *
   */
  protected FixedStepSmoothingConfigurator() {
    super(FixedStepSmoothing.class);
  }

  /**
   * Return the maximum allowable number of iterations
   *
   * @return the maximum allowable number of iterations
   */
  public double getStepSize() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_STEP_SIZE);
  }

  /**
   * Set the maximum allowable number of iterations
   *
   * @param stepSize the maximum allowable number of iterations
   */
  public void setStepSize(double stepSize) {
    registerDelayedMethodCall(SET_STEP_SIZE, stepSize);
  }
}
