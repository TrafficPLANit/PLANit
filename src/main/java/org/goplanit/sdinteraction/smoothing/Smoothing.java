package org.goplanit.sdinteraction.smoothing;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Smoothing class to smooth data, such as path flows or other types of flows or traffic data between iterations
 *
 * @author markr
 *
 */
public abstract class Smoothing extends PlanitComponent<Smoothing> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -3124652824035047922L;

  /** shorthand for configuring smoothing with MSA instance */
  public static final String MSA = MSASmoothing.class.getCanonicalName();

  /** shorthand for configuring smoothing with a self-regulating average instance */
  public static final String MSRA = MSRASmoothing.class.getCanonicalName();

  /** shorthand for configuring smoothing with a fixed step instance */
  public static final String FIXED_STEP = FixedStepSmoothing.class.getCanonicalName();

  /**
   * General helper method for those derived implementations that may want to use it
   *
   * @param step between 0 and 1
   * @param previousValue to use
   * @param proposedValue to use
   * @return (1- step) * prevValue + step * proposedValue
   */
  protected static double smooth(final double step, final double previousValue, final double proposedValue){
    return (1 - step) * previousValue + step * proposedValue;
  }

  /**
   * General helper method for those derived implementations that may want to use it
   *
   * @param step between 0 and 1
   * @param previousValues to use
   * @param proposedValues to use
   * @param numberOfValues to apply
   * @return (1- step) * prevValue[i] + step * proposedValue[i] for all i up to numberOfValues
   */
  protected static double[] smooth(final double step, final double[] previousValues, final double[] proposedValues, final int numberOfValues) {
    final double[] smoothedValues = new double[numberOfValues];
    for (int i = 0; i < numberOfValues; ++i) {
      smoothedValues[i] = smooth(step, previousValues[i],proposedValues[i]);
    }
    return smoothedValues;
  }

  /**
   * Base constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public Smoothing(final IdGroupingToken groupId) {
    super(groupId, Smoothing.class);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Smoothing(Smoothing other, boolean deepCopy) {
    super(other, deepCopy);
  }


  /**
   * Perform updating of step size based on smoothing implementation at hand (Assuming all required information has been
   * set beforehand)
   */
  public abstract void updateStepSize();

  /**
   * Apply smoothing based on the current step size
   *
   * @param previousValue previous value
   * @param proposedValue proposed value
   * @return smoothedValue smoothed value
   */
  public abstract double execute(double previousValue, double proposedValue);

  /**
   * Apply smoothing based on the current step size
   *
   * @param previousValues array of previous values
   * @param proposedValues array of proposed values
   * @param numberOfValues number of proposed values
   * @return smoothedValues array of smoothed values
   */
  public abstract double[] execute(double[] previousValues, double[] proposedValues, int numberOfValues);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract Smoothing shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract Smoothing deepClone();

}
