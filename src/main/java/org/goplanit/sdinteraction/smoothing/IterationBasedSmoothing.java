package org.goplanit.sdinteraction.smoothing;

import org.goplanit.utils.id.IdGroupingToken;

import java.io.Serializable;

/**
 * Smoothing class to smooth data, such as path flows or other types of flows or traffic data between iterations
 *
 * @author markr
 *
 */
public abstract class IterationBasedSmoothing extends Smoothing implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -3124652824035047922L;

  /** track iteration index */
  private int iterationIndex = 0;

  /**
   * Base constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public IterationBasedSmoothing(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public IterationBasedSmoothing(IterationBasedSmoothing other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Update the iteration upon which the step size will be determined
   *
   * @param iterationIndex index of current iteration
   */
  public void updateIteration(int iterationIndex){
    iterationIndex = iterationIndex;
  }

  /**
   * the iteration index as it is currently set
   * @return iteration index
   */
  public int getIteration(){
    return iterationIndex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IterationBasedSmoothing shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IterationBasedSmoothing deepClone();

}
