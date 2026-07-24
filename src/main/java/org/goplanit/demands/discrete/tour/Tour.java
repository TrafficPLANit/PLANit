package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;

/**
 * Tour interface
 */
public interface Tour extends ExternalIdAble, ManagedId, ScheduleElement {
  /**
   * id class for generating ids
   */
  Class<Tour> TOUR_ID_CLASS = Tour.class;

  /**
   * Access to person
   *
   * @return person
   */
  Person getPerson();

  /**
   * person
   *
   * @param person to set
   */
  void setPerson(Person person);

  /**
   * Access to origin
   *
   * @return origin
   */
  OdZone getOrigin();

  /**
   * origin
   *
   * @param origin to set
   */
  void setOrigin(OdZone origin);

  /**
   * Access to destination
   *
   * @return destination
   */
  OdZone getDestination();

  /**
   * destination
   *
   * @param destination to set
   */
  void setDestination(OdZone destination);

  /**
   * origin and destination
   *
   * @param origin      to set
   * @param destination to set
   */
  void setOriginDestination(OdZone origin, OdZone destination);

  /**
   * Purpose
   *
   * @param purpose to set
   */
  void setPurpose(String purpose);

  /**
   * Access to parent tour (if any)
   *
   * @return parent
   */
  Tour getParentTour();

  /**
   * parent tour
   * @param parent to set
   */
  public void setParentTour(Tour parent);

  /**
   * check presence
   *
   * @return true when present
   */
  default boolean hasParentTour() {
    return getParentTour() != null;
  }

  /**
   * Set start time of the tour, i.e., departure time from the origin
   *
   * @param startTime to set
   */
  void setStartTime(LocalTime startTime);

  /**
   * Set end time of tour, i.e., arrival time back at the origin
   *
   * @param endTime to set
   */
  void setEndTime(LocalTime endTime);

  /**
   * Set start end time
   *
   * @param startTime to set
   * @param endTime   to set
   */
  default void setStartEndTime(LocalTime startTime, LocalTime endTime) {
    setStartTime(startTime);
    setEndTime(endTime);
  }

  /**
   * arrivalTime time
   *
   * @return arrivalTime
   */
  LocalTime getEndTime();

  /**
   * set the schedule
   *
   * @param schedule to use
   */
  void setSchedule(ActivitySchedule schedule);
}
