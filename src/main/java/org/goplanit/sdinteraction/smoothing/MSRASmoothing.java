package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.id.IdGroupingToken;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MSRA smoothing (Method of self-regulating averages, as per Liu et al., 2009)
 * <p>
 *   Note that we do not force a particular approach on the the user to decide what to apply: kappa or gamma step, as was proposed in Liu et al., instead
 *   it is assumed the user performs the two values to compare outside of this class and then provides these to the MSRA smoothing to decide whether we encountered a
 *   bad iteration (use kappa), or not (use gamma). Also, different to Liu et al, we do not look at whether the current value is better or worse directly but apply
 *   a user configurable threshold on how much worse a step needs to be before we impose the label "bad iteration". As being more lenient can benefit the convergence
 *   in the long term as a smaller step size by definition leads to slower convergence.
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

  /** threshold value for deciding whether an iteration is bad or not. This represents a proportional deterioration, i.e.,
   * when previous was 0.85 and current is 1, then it has worsened by more than 0.9, i.e., 0.85/1 and thus it is considered
   * a bad iteration, if it is 0.95 and 1, then it is worse than before, but not below the threshold and therefore it is not
   * a bad iteration */
  private double badIterationThreshold = 0.9;
  private boolean badIteration = false;

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
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public MSRASmoothing(MSRASmoothing other, boolean deepCopy) {
    super(other, deepCopy);
    this.kappaStep = other.kappaStep;
    this.gammaStep = other.gammaStep;
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
      LOGGER.warning("MSRA smoothing was not updated whether the most recent iteration improved upon previous or not, consider setting flag, assuming gap reduced");
    }
    isBadIterationFlagUpdated = false;

    previousBeta = beta;
    if(getIteration() <=1){
      beta = 2;
    }else if(isBadIteration()){
      beta = previousBeta + kappaStep;
      LOGGER.warning(String.format("*************** STEPSIZE: %.4f *******************", 1.0 / beta));
    }else{
      beta = previousBeta + gammaStep;
    }
    this.stepSize = 1.0 / beta;
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
    settingsMap.put("badIterationThreshold", "" + badIterationThreshold);
    return settingsMap;
  }

  /**
   * Based on threshold value determine if we should consider upcoming iteration as a bad Iteration or not. Provide
   *  previous and current reference values to do this, i.e., if
   *  previousIterationReferenceValue/currentIterationReferenceValue < {@link #badIterationThreshold} then it is a bad
   *  iteration otherwise not
   *
   * @param previousIterationReferenceValue previous reference value to use
   * @param currentIterationReferenceValue current reference value to use
   */
  public void updateBadIteration(double previousIterationReferenceValue, double currentIterationReferenceValue) {
    badIteration = previousIterationReferenceValue/currentIterationReferenceValue < badIterationThreshold;
    isBadIterationFlagUpdated = true;
  }

  /**
   * Flag indicating if last iteration is to be considered a bad iteration which determines what step to apply
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
}
