package org.planit.cost.physical.initial;

import org.planit.time.TimePeriod;

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
   */
  public InitialLinkSegmentCostPeriod() {
    super();
  }

  // Getters Setters

  public TimePeriod getTimePeriod() {
    return timePeriod;
  }

  public void setTimePeriod(final TimePeriod timePeriod) {
    this.timePeriod = timePeriod;
  }
}
