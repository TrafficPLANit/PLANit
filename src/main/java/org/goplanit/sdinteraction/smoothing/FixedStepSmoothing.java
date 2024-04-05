package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.id.IdGroupingToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixed step smoothing implementation
 *
 * @author markr
 *
 */
public class FixedStepSmoothing extends Smoothing {

  /** generated UID */
  private static final long serialVersionUID = -3016251188673804117L;

  /**
   * Step size
   */
  protected double stepSize = DEFAULT_STEP_SIZE;

  /**
   * The default step size to use
   */
  public static final double DEFAULT_STEP_SIZE = 0.25;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public FixedStepSmoothing(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public FixedStepSmoothing(FixedStepSmoothing other, boolean deepCopy) {
    super(other, deepCopy);
    this.stepSize = other.stepSize;
  }

  /**
   * Set the new fixed step size to use
   *
   * @param stepSize step to use
   */
  public void setStepSize(double stepSize) {
    this.stepSize = stepSize;
  }

  /**
   * Get the fixed step size
   *
   * @return stepSize step to use
   */
  public double getStepSize(double stepSize) {
    return this.stepSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double execute(final double previousValue, final double proposedValue) {
    return (1 - stepSize) * previousValue + stepSize * proposedValue;
  }

  /**
   * Update stepSize
   */
  @Override
  public void updateStepSize() {
    // N/A
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] execute(final double[] previousValues, final double[] proposedValues, final int numberOfValues) {
    final double[] smoothedValues = new double[numberOfValues];
    for (int i = 0; i < numberOfValues; ++i) {
      smoothedValues[i] = (1 - stepSize) * previousValues[i] + stepSize * proposedValues[i];
    }
    return smoothedValues;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FixedStepSmoothing shallowClone() {
    return new FixedStepSmoothing(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FixedStepSmoothing deepClone() {
    return new FixedStepSmoothing(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // No internal state (yet), do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var settingsMap = new HashMap<String, String>();
    settingsMap.put("fixed step-size", "" + stepSize);
    return settingsMap;
  }

}
