package org.goplanit.sdinteraction.smoothing;

/**
 * configurator for MSRA smoothing implementation, loosely based on liu et al, 2009.
 * 
 * @author markr
 */
public class MSRASmoothingConfigurator extends SmoothingConfigurator<MSRASmoothing> {

  /** method name for setting gamma step size on actual instance */
  public static final String SET_GAMMA_STEP = "setGammaStep";

  /** method name for setting gamma step size on actual instance */
  public static final String SET_KAPPA_STEP = "setKappaStep";

  /** method name for setting bad iteration threshold on actual instance */
  public static final String SET_BAD_ITERATION_THRESHOLD = "setBadIterationThreshold";

  /** method name for setting lambda activation on actual instance */
  public static final String SET_ACTIVATE_LAMBDA = "setActivateLambda";

  /**
   * Constructor
   *
   */
  protected MSRASmoothingConfigurator() {
    super(MSRASmoothing.class);
  }

  /**
   * Return the gamma step to use
   *
   * @return the gamma step
   */
  public double getGammaStep() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_GAMMA_STEP);
  }

  /**
   * Set the gamma step to use
   *
   * @param gammaStep the gamma step
   */
  public void setGammaStep(double gammaStep) {
    registerDelayedMethodCall(SET_GAMMA_STEP, gammaStep);
  }

  /**
   * Return the kappa step to use
   *
   * @return the kappa step
   */
  public double getKappaStep() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_KAPPA_STEP);
  }

  /**
   * Set the kappa step to use
   *
   * @param kappaStep the kappa step
   */
  public void setKappaStep(double kappaStep) {
    registerDelayedMethodCall(SET_KAPPA_STEP, kappaStep);
  }

  /**
   * Get the lambda flag
   *
   * @return activateLambda flag
   */
  public Boolean isActivateLambda() {
    return (Boolean) getFirstParameterOfDelayedMethodCall(SET_ACTIVATE_LAMBDA);
  }

  public void setActivateLambda(Boolean activateLambda) {
    registerDelayedMethodCall(SET_ACTIVATE_LAMBDA, activateLambda);
  }

  /**
   * Return the badIterationThreshold to use
   *
   * @return the badIterationThreshold
   */
  public double getBadIterationThreshold() {
    return (double) getFirstParameterOfDelayedMethodCall(SET_BAD_ITERATION_THRESHOLD);
  }

  /**
   * Set the badIterationThreshold to use
   * <p>
   *   threshold value for deciding whether an iteration is bad or not. This represents a proportional deterioration, i.e.,
   *   when previous was 0.85 and current is 1, then it has worsened by more than 0.9, i.e., 0.85/1 and thus it is considered
   *   a bad iteration, if it is 0.95 and 1, then it is worse than before, but not below the threshold and therefore it is not
   *   a bad iteration
   * </p>
   *
   * @param badIterationThreshold the kappa step
   */
  public void setBadIterationThreshold(double badIterationThreshold) {
    registerDelayedMethodCall(SET_BAD_ITERATION_THRESHOLD, badIterationThreshold);
  }

}
