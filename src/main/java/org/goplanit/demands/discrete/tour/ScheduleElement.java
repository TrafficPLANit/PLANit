package org.goplanit.demands.discrete.tour;

import org.goplanit.utils.mode.Mode;

import java.time.LocalTime;
import java.util.function.Predicate;

/**
 * An element of a schedule is a tour or an individual trip. A tour has a schedule of its own, with sub tours and/or
 * trips, all performed before the next element at the same level
 * <p>
 * Timing is exposed read-only here. To move an element in time, act on the
 * {@link org.goplanit.demands.discrete.trip.Trip} or {@link Tour} itself. A tour may be shared by several
 * participants, and changing its timing affects all of them, so this is made explicit at the call site
 * </p>
 */
public interface ScheduleElement {

  /**
   * Check if schedule exists
   * @return true when present false otherwise
   */
  public default boolean hasSchedule(){
    return getSchedule() != null;
  }

  /**
   * Access the start time of this scheduled element
   * @return the start time
   */
  LocalTime getStartTime();

  /**
   * Each schedule element has a purpose
   *
   * @return purpose
   */
  String getPurpose();

  /**
   * Access to schedule if it has one
   * @return schedule, null if not present
   */
  public abstract ActivitySchedule getSchedule();

  /**
   * Check if this element, any of the schedule elements or the schedules of the schedule elements conform to
   * the predicate
   *
   * @param predicate to apply
   * @return result of any predicate matching
   */
  public default boolean testNested(Predicate<ScheduleElement> predicate){
    return predicate.test(this) || hasSchedule() && getSchedule().testNested(predicate);
  }

  /**
   * Find the outbound mode
   *
   * @return returns the (representative) outbound mode
   */
  public abstract Mode getOutboundMode();

  /**
   * Find the inbound mode
   *
   * @return returns the inbound mode
   */
  public abstract Mode getInboundMode();

}
