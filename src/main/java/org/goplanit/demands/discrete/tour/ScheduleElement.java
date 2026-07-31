package org.goplanit.demands.discrete.tour;

import java.time.LocalTime;
import java.util.function.Predicate;

/**
 * An element of a schedule can contain other tours or individual trips
 * such that whenever it is a tour, that tour has a schedule with other sub tours and or trips, only after which
 * the next scheduled element at the same level is performed
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
   * Set the start time of the element
   * @param startTime to use
   */
  public abstract void setStartTime(LocalTime startTime);

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

}
