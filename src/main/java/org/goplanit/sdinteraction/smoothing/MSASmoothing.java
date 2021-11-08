package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.id.IdGroupingToken;

/**
 * MSA smoothing object
 *
 * @author markr
 *
 */
public class MSASmoothing extends Smoothing {

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
   */
  public MSASmoothing(MSASmoothing other) {
    super(other);
    this.stepSize = other.stepSize;
  }

  /**
   * Update stepSize to 1/iterationIndex
   *
   * @see org.goplanit.sdinteraction.smoothing.Smoothing#updateStep(int)
   */
  @Override
  public void updateStep(final int iterationIndex) {
    this.stepSize = 1.0 / (iterationIndex + 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double execute(final double previousValue, final double proposedValue) {
    return (1 - stepSize) * previousValue + stepSize * proposedValue;
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
  public MSASmoothing clone() {
    return new MSASmoothing(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.stepSize = DEFAULT_INITIAL_STEP_SIZE;
  }

}
