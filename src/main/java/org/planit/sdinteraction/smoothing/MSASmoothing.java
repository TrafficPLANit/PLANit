package org.planit.sdinteraction.smoothing;

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
  protected double stepSize = 1;

  /**
   * Constructor
   */
  public MSASmoothing() {
    super();
  }

  /**
   * Update stepSize to 1/iterationIndex
   *
   * @see org.planit.sdinteraction.smoothing.Smoothing#update(int)
   */
  @Override
  public void update(final int iterationIndex) {
    this.stepSize = 1.0 / (iterationIndex + 1);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double applySmoothing(final double previousValue, final double proposedValue) {
    return (1 - stepSize) * previousValue + stepSize * proposedValue;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public double[] applySmoothing(final double[] previousValues, final double[] proposedValues,
      final int numberOfValues) {
    final double[] smoothedValues = new double[numberOfValues];
    for (int i = 0; i < numberOfValues; ++i) {
      smoothedValues[i] = (1 - stepSize) * previousValues[i] + stepSize * proposedValues[i];
    }
    return smoothedValues;
  }

}
