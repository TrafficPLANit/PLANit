package org.goplanit.demands.discrete.tour;

/**
 * An element of a tour schedule can contain other tours or individual trips
 * such that whenever it is a tour, that tour has a schedule with other sub tours and or trips, only after which
 * the next scheduled element at the same level is performed
 */
public interface TourScheduleElement {

  /**
   * Check if schedule exists
   * @return true when present false otherwise
   */
  public default boolean hasSchedule(){
    return getSchedule() != null;
  }

  /**
   * Access to schedule if it has one
   * @return schedule, null if not present
   */
  public abstract TourSchedule getSchedule();
}
