package org.goplanit.demands.discrete.trip;

import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;

import java.time.Duration;
import java.time.LocalTime;

public interface Trip extends ExternalIdAble, ManagedId, ScheduleElement {

  /** id class for generating ids */
  public static final Class<Trip> TRIP_ID_CLASS = Trip.class;

  /**
   * Purpose
   *
   * @return purpose
   */
  String getPurpose();

  /**
   * Purpose
   *
   * @param purpose to set
   */
  void setPurpose(String purpose);

  /**
   * mode
   *
   * @return mode
   */
  Mode getMode();

  /**
   * mode
   *
   * @param mode to set
   */
  void setMode(Mode mode);

  /**
   * Access to tour
   *
   * @return tour
   */
  Tour getTour();

  /**
   * set tour
   *
   * @param tour to use
   */
  void setTour(Tour tour);

  /**
   * Directionality
   *
   * @return directionality
   */
  DirectionBound getDirection();

  /**
   * Set directionality
   *
   * @param direction to use
   */
  void setDirection(DirectionBound direction);

  /**
   * set departure time
   *
   * @param startTime to use
   */
  void setStartTime(LocalTime startTime);

  /**
   * A trip has no internal schedule as it is always a "leaf" element of a schedule
   */
  ActivitySchedule getSchedule();

  /**
   * When this is invoked, we sync the start time to the start time of the trip's tour.
   * This generally only makes sense when this is the outbound trip of a tour, because otherwise we'd
   * expect it to be a departure at the destination of the tour that arrives round the end time of the tour.
   * In that case use the syncStartTimeToTourWithNegativeOffset
   */
  default void syncStartTimeToTourStartTime() {
    if (getTour() == null) {
      return;
    }
    setStartTime(getTour().getStartTime());
  }

  /**
   * same as {@link #syncStartTimeToTourStartTime()} but we subtract time from the tour END time to depart earlier
   *
   * @param subtractFromTourEndTime to subtract
   */
  default void syncStartTimeToTourEndWithNegativeOffset(Duration subtractFromTourEndTime) {
    if (getTour() == null) {
      return;
    }
    setStartTime(getTour().getEndTime().minus(subtractFromTourEndTime));
  }

  /**
   * Derive purpose from the direction of trip and the purpose of the tour, e.g.
   * when outbound: "to:*name_of_tour_purpose*", when inbound "from:*name_of_tour_purpose*"
   *
   * @return string set as purpose
   */
  default String derivePurposeFromDirectionAndTour() {
    switch (getDirection()) {
      case INBOUND:
        setPurpose("from:" + getTour().getPurpose());
        break;
      case OUTBOUND:
        setPurpose("to:" + getTour().getPurpose());
        break;
      default:
        throw new PlanItRunTimeException("Unknown direction type %s, unable to derive trip (%s) purpose",
            getDirection(), getIdsAsString());
    }
    return getPurpose();
  }

  /**
   * Verify if origin is set explicitly on trip
   *
   * @return flag
   */
  default boolean isOriginSet() {
    return getOrigin(false) != null;
  }

  /**
   * Get the value of origin.
   *
   * @param useTourIfNotSet if true derive from tour based on direction logic, only valid in single trip tour leg
   * @return value of origin
   */
  OdZone getOrigin(boolean useTourIfNotSet);

  /**
   * Set the value of origin.
   *
   * @param origin value of origin
   */
  void setOrigin(OdZone origin);

  /**
   * Verify if destination is set explicitly on trip
   *
   * @return flag
   */
  default boolean isDestinationSet() {
    return getDestination(false) != null;
  }

  /**
   * Get the value of destination.
   *
   * @param useTourIfNotSet if true derive from tour based on direction logic, only valid in single trip tour leg
   * @return value of destination
   */
  OdZone getDestination(boolean useTourIfNotSet);

  /**
   * Set the value of destination.
   *
   * @param destination value of destination
   */
  void setDestination(OdZone destination);
}
