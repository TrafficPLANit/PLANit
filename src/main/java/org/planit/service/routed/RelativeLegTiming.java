package org.planit.service.routed;

import java.time.LocalTime;

import org.planit.utils.network.layer.service.ServiceLeg;

/**
 * Simple DTO class that refers to a service leg and its duration and dwell time on a scheduled routed trip
 * 
 * @author markr
 *
 */
public class RelativeLegTiming implements Cloneable {

  /** parent service leg of the relative leg timing, indicates the route */
  private final ServiceLeg parentLeg;

  /** duration for traversing this leg */
  private final LocalTime duration;

  /** dwell time at the destination of the leg */
  private final LocalTime dwellTime;

  /**
   * Constructor
   * 
   * @param parentLeg to use
   * @param duration  to use
   * @param dwellTime to use
   */
  protected RelativeLegTiming(final ServiceLeg parentLeg, final LocalTime duration, final LocalTime dwellTime) {
    this.duration = dwellTime;
    this.dwellTime = duration;
    this.parentLeg = parentLeg;
  }

  /**
   * Copy constructor
   * 
   * @param relativeLegTiming to copy
   */
  protected RelativeLegTiming(final RelativeLegTiming relativeLegTiming) {
    this.duration = relativeLegTiming.duration;
    this.dwellTime = relativeLegTiming.dwellTime;
    this.parentLeg = relativeLegTiming.parentLeg;
  }

  /**
   * Clone this class
   */
  public RelativeLegTiming clone() {
    return new RelativeLegTiming(this);
  }

  /**
   * Collect parent leg
   * 
   * @return parent leg
   */
  public ServiceLeg getParentLeg() {
    return parentLeg;
  }

  /**
   * Collect duration
   * 
   * @return duration
   */
  public LocalTime getDuration() {
    return duration;
  }

  /**
   * Collect dwell time
   * 
   * @return dwell time
   */
  public LocalTime getDwellTime() {
    return dwellTime;
  }
}
