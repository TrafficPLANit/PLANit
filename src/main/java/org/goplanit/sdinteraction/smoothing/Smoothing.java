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

  /** shorthand for configuring smoothing with a fixed step instance */
  public static final String FIXED_STEP = FixedStepSmoothing.class.getCanonicalName();

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
