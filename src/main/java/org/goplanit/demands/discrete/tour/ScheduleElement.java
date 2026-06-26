package org.goplanit.demands.discrete.tour;

import java.time.LocalTime;

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
   * Access to schedule if it has one
   * @return schedule, null if not present
   */
  public abstract Schedule getSchedule();

}
