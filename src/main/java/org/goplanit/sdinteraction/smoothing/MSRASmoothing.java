package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.id.IdGroupingToken;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MSRA smoothing (Method of self-regulating averages, as per Liu et al., 2009) + additional lambda power function
 * <p>
 *   Unlike Liu et al., no particular approach is forced on the user to decide whether to apply a kappa or gamma step.
 *   Instead the user is assumed to compute the two values to compare outside this class and provide them to the MSRA
 *   smoothing, which decides whether the iteration was bad (use kappa) or not (use gamma). Also unlike Liu et al., it
 *   does not look directly at whether the current value is better or worse, but applies a user configurable threshold
 *   on how much worse a step needs to be before it is labelled a "bad iteration". Being more lenient can benefit
 *   convergence in the long term, as a smaller step size by definition leads to slower convergence.
 * </p>
 *
 * @author markr
 *
 */
public class MSRASmoothing extends IterationBasedSmoothing {

  /** logger to use */
  private static Logger LOGGER = Logger.getLogger(MSRASmoothing.class.getCanonicalName());

  private int failSafeLastIterationUpdate = 0;

  /** track to see if badIteration flag has been updated. If not done every iteration issue warning as it is likely
   * forgotten by user as it requires additional work compared to "simple" MSA */
  private boolean isBadIterationFlagUpdated = true;

  /**
   * Current iteration smoothing value
   */
  protected double stepSize = DEFAULT_INITIAL;

  /** threshold value for deciding whether an iteration is bad or not, as a proportional deterioration. For example,
   *  if set to 0.9, a previous value of 0.85 and a current value of 1 have worsened by more than 0.9 (0.85/1), so it is
   *  a bad iteration. With 0.95 and 1 it is worse than before, but not below the threshold, so not a bad iteration */
  private double badIterationThreshold = 1;
  private boolean badIteration = false;

  /** dampen impact of beta on stepsize over iterations using a iteration depending lambda power function */
  private boolean activateLambda = DEFAULT_LAMBDA_FLAG;

  /** denominator of stepsize, i.e., 1/beta_iter is the final step size applied */
  protected double beta = DEFAULT_INITIAL;

  /** previous iteration denominator of stepsize, used to compute new beta of current iteration */
  protected double previousBeta = beta;

  /**
   * Step size change after a bad iteration, i.e., when last iteration went badly we should take a smaller step hence
   * kappa is large as it is applied in denominator
   */
  protected double kappaStep = DEFAULT_KAPPA_STEP;

  /**
   * Step size change after a good iteration, i.e., when last iteration went well we should take a larger step hence
   * gamma is small as it is applied in denominator
   */
  protected double gammaStep = DEFAULT_GAMMA_STEP;

  /**
   * The default kappa step size to use
   */
  public static final double DEFAULT_KAPPA_STEP = 2;

  /**
   * The default gamma step size to use
   */
  public static final double DEFAULT_GAMMA_STEP = 0.02;

  /**
   * Initial value to use
   */
  public static final boolean DEFAULT_LAMBDA_FLAG = true;

  /**
   * Initial value to use
   */
  public static final double DEFAULT_INITIAL = 1;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MSRASmoothing(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param activateLambda flag
   */
  public MSRASmoothing(IdGroupingToken groupId, boolean activateLambda) {
    super(groupId);
    this.activateLambda = activateLambda;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public MSRASmoothing(MSRASmoothing other, boolean deepCopy) {
    super(other, deepCopy);
    this.kappaStep = other.kappaStep;
    this.gammaStep = other.gammaStep;
    this.activateLambda = other.activateLambda;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double execute(final double previousValue, final double proposedValue) {
    return smooth(stepSize, previousValue, proposedValue);
  }

  /**
   * Update stepSize to 1/beta, where we update beta to
   * beta = prevBeta + kappa if bad iteration or prevBeta + gamma if !badIteration.
   */
  @Override
  public void updateStepSize() {
    // make sure we do not accidentally update step size multiple times per iteration
    if(failSafeLastIterationUpdate == getIteration()){
      return;
    }
    failSafeLastIterationUpdate = getIteration();

    if(!isBadIterationFlagUpdated){
      LOGGER.warning("MSRA smoothing was not updated whether the most recent iteration improved upon " +
          "previous or not, consider setting flag, assuming gap reduced");
    }
    isBadIterationFlagUpdated = false;

    double lambdaPower = 1;
    if(isActivateLambda()) {
      lambdaPower = 0.66;
    }

    previousBeta = beta;
    if(getIteration() <=1){
      beta = 1;
    }else if(isBadIteration()){
      beta = previousBeta + kappaStep;
    }else{
      beta = Math.max(1,previousBeta + gammaStep);
    }
    this.stepSize = 1.0 / Math.max(1, Math.pow(Math.abs(beta),lambdaPower));
    //LOGGER.info(String.format("Stepsize: %.10f  (lambda=%.2f beta=%.2f)",stepSize, lambdaPower, beta));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] execute(final double[] previousValues, final double[] proposedValues, final int numberOfValues) {
    return smooth(stepSize, previousValues, proposedValues, numberOfValues);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MSRASmoothing shallowClone() {
    return new MSRASmoothing(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MSRASmoothing deepClone() {
    return new MSRASmoothing(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.badIteration = false;
    this.isBadIterationFlagUpdated = true;
    this.beta = DEFAULT_INITIAL;
    this.previousBeta = beta;
    this.stepSize = DEFAULT_INITIAL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var settingsMap = new HashMap<String, String>();
    settingsMap.put("gamma", "" + gammaStep);
    settingsMap.put("kappa", "" + kappaStep);
    settingsMap.put("lambda active", "" + isActivateLambda());
    settingsMap.put("badIterationThreshold", "" + badIterationThreshold);
    return settingsMap;
  }

  /**
   * Determine from the threshold value and the previous and current reference values whether the upcoming iteration
   * is a bad iteration: it is when previousIterationReferenceValue/currentIterationReferenceValue is smaller than
   * {@link #badIterationThreshold}
   *
   * @param previousIterationReferenceValue previous reference value to use
   * @param currentIterationReferenceValue current reference value to use
   */
  public void updateIsBadIteration(double previousIterationReferenceValue, double currentIterationReferenceValue) {
    badIteration = previousIterationReferenceValue/currentIterationReferenceValue < badIterationThreshold;
    isBadIterationFlagUpdated = true;
  }

  /**
   * Flag indicating if last iteration is to be considered a bad iteration which determines what step to apply
   *
   * @return true if bad iteration, false otherwise
   */
  public boolean isBadIteration() {
    return badIteration;
  }

  public void setKappaStep(double kappaStep) {
    this.kappaStep = kappaStep;
  }

  public double getKappaStep() {
    return kappaStep;
  }

  public double getGammaStep() {
    return gammaStep;
  }

  public void setGammaStep(double gammaStep) {
    this.gammaStep = gammaStep;
  }

  public double getBadIterationThreshold() {
    return badIterationThreshold;
  }

  public void setBadIterationThreshold(double badIterationThreshold) {
    this.badIterationThreshold = badIterationThreshold;
  }

  public boolean isActivateLambda() {
    return activateLambda;
  }

  public void setActivateLambda(boolean activateLambda) {
    this.activateLambda = activateLambda;
  }

  public void logStepSize() {
    LOGGER.info(String.format("*************** STEPSIZE: %.4f *******************", stepSize));
  }
}
