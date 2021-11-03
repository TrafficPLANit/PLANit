package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.network.layer.service.ServiceLegSegment;

/**
 * Simple POJO class that refers to a service leg and its duration and dwell time on a scheduled routed trip
 * 
 * @author markr
 *
 */
public class RelativeLegTiming implements Cloneable {

  /** parent service leg segment (directed leg) of the relative leg timing, indicates the route */
  private final ServiceLegSegment parentLegSegment;

  /** duration for traversing this leg */
  private final LocalTime duration;

  /** dwell time at the destination of the leg */
  private final LocalTime dwellTime;

  /**
   * Constructor
   * 
   * @param parentLegSegment to use
   * @param duration         to use
   * @param dwellTime        to use
   */
  protected RelativeLegTiming(final ServiceLegSegment parentLegSegment, final LocalTime duration, final LocalTime dwellTime) {
    this.duration = duration;
    this.dwellTime = dwellTime;
    this.parentLegSegment = parentLegSegment;
  }

  /**
   * Copy constructor
   * 
   * @param relativeLegTiming to copy
   */
  protected RelativeLegTiming(final RelativeLegTiming relativeLegTiming) {
    this.duration = relativeLegTiming.duration;
    this.dwellTime = relativeLegTiming.dwellTime;
    this.parentLegSegment = relativeLegTiming.parentLegSegment;
  }

  /**
   * Clone this class
   */
  public RelativeLegTiming clone() {
    return new RelativeLegTiming(this);
  }

  /**
   * Collect parent leg segment
   * 
   * @return parent leg segment
   */
  public ServiceLegSegment getParentLegSegment() {
    return parentLegSegment;
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
