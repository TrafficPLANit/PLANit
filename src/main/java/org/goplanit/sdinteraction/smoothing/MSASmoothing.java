package org.goplanit.sdinteraction.smoothing;

import java.util.HashMap;
import java.util.Map;

import org.goplanit.utils.id.IdGroupingToken;

/**
 * MSA smoothing object
 *
 * @author markr
 *
 */
public class MSASmoothing extends IterationBasedSmoothing {

  /** generated UID */
  private static final long serialVersionUID = -3016251188673804117L;

  /**
   * Step size
   */
  protected double stepSize = DEFAULT_INITIAL_STEP_SIZE;

  /**
   * The default initial step size to use
   */
  public static final double DEFAULT_INITIAL_STEP_SIZE = 1.0;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MSASmoothing(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public MSASmoothing(MSASmoothing other, boolean deepCopy) {
    super(other, deepCopy);
    this.stepSize = other.stepSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double execute(final double previousValue, final double proposedValue) {
    return smooth(stepSize, previousValue, proposedValue);
  }

  /**
   * Update stepSize to 1/iterationIndex
   */
  @Override
  public void updateStepSize() {
    this.stepSize = 1.0 / (getIteration() + 1);
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
  public MSASmoothing shallowClone() {
    return new MSASmoothing(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MSASmoothing deepClone() {
    return new MSASmoothing(this, true);
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
    settingsMap.put("step-size", "" + stepSize);
    return settingsMap;
  }

}
