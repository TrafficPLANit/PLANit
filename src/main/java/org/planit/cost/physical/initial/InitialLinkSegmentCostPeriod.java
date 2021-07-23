package org.planit.cost.physical.initial;

import org.planit.time.TimePeriodImpl;
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
   */
  protected final TimePeriod timePeriod;

  /**
   * Constructor. See https://github.com/TrafficPLANit/PLANitUtils/issues/7 for reason on why we have this constructor. Until fixed, we rely on this to ensure reflection based
   * instantiation doesn't fail.
   * 
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param timePeriod to use
   */
  public InitialLinkSegmentCostPeriod(final IdGroupingToken groupId, final TimePeriodImpl timePeriod) {
    this(groupId, (TimePeriod) timePeriod);
  }

  /**
   * Constructor
   * 
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param timePeriod to use
   */
  public InitialLinkSegmentCostPeriod(final IdGroupingToken groupId, final TimePeriod timePeriod) {
    super(groupId);
    this.timePeriod = timePeriod;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public InitialLinkSegmentCostPeriod clone() {
    return new InitialLinkSegmentCostPeriod(this);
  }
}
