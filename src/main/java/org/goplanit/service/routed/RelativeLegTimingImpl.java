package org.goplanit.service.routed;

import java.time.LocalTime;

import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.service.routed.RelativeLegTiming;

/**
 * Simple POJO class that refers to a service leg and its duration and dwell time on a scheduled routed trip
 * 
 * @author markr
 *
 */
public class RelativeLegTimingImpl implements RelativeLegTiming {

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
  protected RelativeLegTimingImpl(final ServiceLegSegment parentLegSegment, final LocalTime duration, final LocalTime dwellTime) {
    this.duration = duration;
    this.dwellTime = dwellTime;
    this.parentLegSegment = parentLegSegment;
  }

  /**
   * Copy constructor
   * 
   * @param relativeLegTiming to copy
   */
  protected RelativeLegTimingImpl(final RelativeLegTimingImpl relativeLegTiming) {
    this.duration = relativeLegTiming.duration;
    this.dwellTime = relativeLegTiming.dwellTime;
    this.parentLegSegment = relativeLegTiming.parentLegSegment;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public RelativeLegTiming clone() {
    return new RelativeLegTimingImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegment getParentLegSegment() {
    return parentLegSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalTime getDuration() {
    return duration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalTime getDwellTime() {
    return dwellTime;
  }
}
