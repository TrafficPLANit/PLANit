package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.utils.mode.Mode;

import java.time.LocalTime;

/**
 * A single person's participation in a tour. This, rather than the tour itself, is what appears on a person's
 * {@link ActivitySchedule}, so that one tour can be shared by several persons - for example a joint household tour -
 * while each person's schedule remains their own.
 * <p>
 * All travel related information is held by the underlying {@link Tour} and is exposed here read-only, since
 * participants by definition share the same origin, destination, times and trips. Only the person and their role are
 * specific to the participation. Changing any of the shared information is deliberately <b>not</b> possible through
 * this participation: obtain the tour via {@link #getTour()} and change it there, so it is evident at the call site
 * that every participant is affected.
 * </p>
 */
public interface ParticipantTour extends ScheduleElement {

  /**
   * The tour this is a participation in. Use this to alter any shared tour information
   *
   * @return tour
   */
  Tour getTour();

  /**
   * The person participating in the tour
   *
   * @return person
   */
  Person getPerson();

  /**
   * The role this person takes on the tour
   *
   * @return role
   */
  TourParticipantRole getRole();

  /**
   * Verify if this participation is the primary one, i.e. the tour belongs to this person
   *
   * @return true when primary, false otherwise
   */
  default boolean isPrimary() {
    return getRole() != null && getRole().isPrimary();
  }

  /**
   * End time of the underlying tour, i.e., moment of arrival back at its origin
   *
   * @return end time
   */
  default LocalTime getEndTime() {
    return getTour().getEndTime();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default LocalTime getStartTime() {
    return getTour().getStartTime();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default String getPurpose() {
    return getTour().getPurpose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default ActivitySchedule getSchedule() {
    return getTour().getSchedule();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Mode getOutboundMode() {
    return getTour().getOutboundMode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Mode getInboundMode() {
    return getTour().getInboundMode();
  }
}
