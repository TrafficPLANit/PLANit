package org.planit.cost.physical.initial;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.time.TimePeriod;

/**
 * Identical to InitialLinkSegmentCost except that it is directly attached to a particular time period.
 * 
 * @author markr
 */
public class InitialLinkSegmentCostPeriod extends InitialLinkSegmentCost {

  /** generated UID */
  private static final long serialVersionUID = 2164407379859550420L;

  /**
   * The time period which this initial cost object applies to.
   *
   * If this property is not set, this initial cost object applies to all time periods.
   */
  protected TimePeriod timePeriod = null;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public InitialLinkSegmentCostPeriod(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public InitialLinkSegmentCostPeriod(InitialLinkSegmentCostPeriod other) {
    super(other);
    timePeriod = other.timePeriod;
  }

  // Getters Setters

  public TimePeriod getTimePeriod() {
    return timePeriod;
  }

  public void setTimePeriod(final TimePeriod timePeriod) {
    this.timePeriod = timePeriod;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InitialLinkSegmentCostPeriod clone() {
    return new InitialLinkSegmentCostPeriod(this);
  }
}
