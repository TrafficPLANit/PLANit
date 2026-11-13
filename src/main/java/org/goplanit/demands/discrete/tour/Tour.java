package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.OdZone;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tour interface.
 * <p>
 * A tour is not itself a {@link ScheduleElement}; a person's schedule holds a {@link ParticipantTour} instead. This
 * allows a single tour to be shared by more than one person, for example a joint household tour, while each person
 * keeps their own schedule. A tour with a single participant is expressed the same way, so there is exactly one
 * mechanism regardless of how many people travel together.
 * </p>
 */
public interface Tour extends ExternalIdAble, ManagedId {
  /**
   * id class for generating ids
   */
  Class<Tour> TOUR_ID_CLASS = Tour.class;

  /**
   * Access to the participations in this tour, primary participant first
   *
   * @return participant tours
   */
  List<ParticipantTour> getParticipantTours();

  /**
   * Register a person as participating in this tour, without knowing whether they drive. The participation is not
   * placed on the person's schedule here, see {@link ToursFactory} for the regular creation path
   *
   * @param person to register
   * @param role the person takes on this tour
   * @return created participation
   */
  default ParticipantTour addParticipant(Person person, TourParticipantRole role) {
    return addParticipant(person, role, null);
  }

  /**
   * Register a person as participating in this tour. The participation is not placed on the person's schedule
   * here, see {@link ToursFactory} for the regular creation path
   *
   * @param person to register
   * @param role the person takes on this tour
   * @param driver whether the person drives the tour's trips by a mode that needs a driver, null when not known
   * @return created participation
   */
  ParticipantTour addParticipant(Person person, TourParticipantRole role, Boolean driver);

  /**
   * Set the driver of this tour: the given participation drives the tour's trips by a mode that needs a driver and
   * every other participation does not. To set the value of one participation only, see
   * {@link ParticipantTour#setDriver(Boolean)}
   *
   * @param driver participation on this tour that drives
   * @throws PlanItRunTimeException when the participation is not on this tour, leaving every value as it was
   */
  void setDriver(ParticipantTour driver);

  /**
   * Verify if this tour's driver is known, which is so once any of its participations says whether its person drives.
   * An own tour whose person does not drive counts as known, as someone not on the tour drives it
   *
   * @return true when known, false otherwise
   */
  default boolean isDriverKnown() {
    return getParticipantTours().stream().anyMatch(participantTour -> participantTour.getDriver() != null);
  }

  /**
   * Remove a participation from this tour
   *
   * @param participantTour to remove
   * @return true when removed, false when not present
   */
  boolean removeParticipant(ParticipantTour participantTour);

  /**
   * The participation with role {@link TourParticipantRole#PRIMARY}, i.e. the one placed on the schedule of the
   * person the tour belongs to
   *
   * @return primary participation, null when there is none yet
   */
  default ParticipantTour getPrimaryParticipation() {
    return getParticipantTours().stream().filter(ParticipantTour::isPrimary).findFirst().orElse(null);
  }

  /**
   * The person this tour belongs to, i.e. the participant with role {@link TourParticipantRole#PRIMARY}
   *
   * @return primary participant, null when there are no participants yet
   */
  default Person getPrimaryParticipant() {
    var primaryParticipation = getPrimaryParticipation();
    return primaryParticipation != null ? primaryParticipation.getPerson() : null;
  }

  /**
   * Verify if a primary participant is present, i.e. the tour belongs to someone. A tour may briefly exist without
   * one while its participants are still being registered, so that no ordering of registration is assumed, but it
   * is not valid to persist it in that state
   *
   * @return true when present, false otherwise
   */
  default boolean hasPrimaryParticipant() {
    return getPrimaryParticipant() != null;
  }

  /**
   * All persons participating in this tour
   *
   * @return participating persons
   */
  default Collection<Person> getParticipants() {
    return getParticipantTours().stream().map(ParticipantTour::getPerson).collect(Collectors.toList());
  }

  /**
   * Verify if more than one person participates, i.e. this is a joint tour
   *
   * @return true when more than one participant, false otherwise
   */
  default boolean hasMultipleParticipants() {
    return getParticipantTours().size() > 1;
  }

  /**
   * The role of the given person on this tour
   *
   * @param person to collect role for
   * @return role found, null when the person does not participate
   */
  default TourParticipantRole getRole(Person person) {
    return getParticipantTours().stream().filter(pt -> pt.getPerson().equals(person)).map(
        ParticipantTour::getRole).findFirst().orElse(null);
  }

  /**
   * Start time of the tour, i.e., departure time from the origin
   *
   * @return start time
   */
  LocalTime getStartTime();

  /**
   * Purpose of the tour, encompassing the entire tour rather than a single trip
   *
   * @return purpose
   */
  String getPurpose();

  /**
   * Access to the schedule holding this tour's trips and any sub tours
   *
   * @return schedule
   */
  ActivitySchedule getSchedule();

  /**
   * Verify if a schedule is present
   *
   * @return true when present, false otherwise
   */
  default boolean hasSchedule() {
    return getSchedule() != null;
  }

  /**
   * Verify if one of this tour's trips is by a mode the predicate accepts. The trips of a sub tour belong to the sub
   * tour, not to this tour
   *
   * @param modePredicate to test the mode of each trip with
   * @return true when such a trip is found, false otherwise
   */
  default boolean hasTripByMode(Predicate<Mode> modePredicate) {
    return hasSchedule() && getSchedule().stream()
        .filter(Trip.class::isInstance)
        .map(element -> ((Trip) element).getMode())
        .anyMatch(mode -> mode != null && modePredicate.test(mode));
  }

  /**
   * The (representative) mode used on the outbound leg of this tour
   *
   * @return outbound mode
   */
  Mode getOutboundMode();

  /**
   * The mode used on the inbound leg of this tour
   *
   * @return inbound mode
   */
  Mode getInboundMode();

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
