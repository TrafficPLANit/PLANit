package org.planit.sdinteraction.smoothing;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * Smoothing class to smooth data, such as path flows or other types of flows or traffic data between iterations
 *
 * @author markr
 *
 */
public abstract class Smoothing extends TrafficAssignmentComponent<Smoothing> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -3124652824035047922L;

  /** short hand for configuring smoothing with MSA instance */
  public static final String MSA = MSASmoothing.class.getCanonicalName();

  /**
   * Base constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public Smoothing(final IdGroupingToken groupId) {
    super(groupId, Smoothing.class);
  }

  /**
   * Determine the stepsize for the passed in iteraction
   *
   * @param iterationIndex index of current iteration
   */
  public abstract void update(int iterationIndex);

  /**
   * Apply smoothing based on the current step size
   *
   * @param previousValue previous value
   * @param proposedValue proposed value
   * @return smoothedValue smoothed value
   */
  public abstract double applySmoothing(double previousValue, double proposedValue);

  /**
   * Apply smoothing based on the current step size
   *
   * @param previousValues array of previous values
   * @param proposedValues array of proposed values
   * @param numberOfValues number of proposed values
   * @return smoothedValues array of smoothed values
   */
  public abstract double[] applySmoothing(double[] previousValues, double[] proposedValues, int numberOfValues);

}
