package org.goplanit.demands.discrete.tour;

import org.goplanit.demands.discrete.person.Person;

import java.util.Objects;

/**
 * Implementation of a person's participation in a tour. Holds only what is specific to the participation, i.e. the
 * person, their role and whether they drive, and defers everything else to the underlying tour, see
 * {@link ParticipantTour}.
 * <p>
 * A participation is uniquely identified by the combination of its tour and its person, since a person takes part in
 * a given tour at most once. Equality and hashing are therefore delegated to the tour and person, which compare on
 * their own id as all {@link org.goplanit.utils.id.IdAble}s do, rather than being based on object identity. This
 * keeps hashing stable across runs, which object identity would not.
 * </p>
 * <p>
 * The tour, the person and the role are fixed: the tour and the person identify the participation, and the tour keeps
 * exactly one primary participant, first, so a change of role goes through the tour. Only whether the person drives
 * can be set on the participation.
 * </p>
 *
 * @author markr
 */
public class ParticipantTourImpl implements ParticipantTour {

  /** the tour participated in */
  private final Tour tour;

  /** the participating person */
  private final Person person;

  /** the role of the person on the tour */
  private final TourParticipantRole role;

  /** whether the person drives the tour's trips that need a driver, null when not known */
  private Boolean driver;

  /**
   * Constructor. Not intended for direct use, participations are created via
   * {@link Tour#addParticipant(Person, TourParticipantRole, Boolean)}
   *
   * @param tour that is participated in
   * @param person participating
   * @param role of the person on the tour
   * @param driver whether the person drives the tour's trips that need a driver, null when not known
   */
  protected ParticipantTourImpl(Tour tour, Person person, TourParticipantRole role, Boolean driver) {
    this.tour = tour;
    this.person = person;
    this.role = role;
    this.driver = driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Tour getTour() {
    return tour;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Person getPerson() {
    return person;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TourParticipantRole getRole() {
    return role;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean getDriver() {
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDriver(Boolean driver) {
    this.driver = driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParticipantTour)) {
      return false;
    }
    var other = (ParticipantTour) o;
    /* tour and person resolve this via their own id based equality, see IdAble.idEquals */
    return Objects.equals(getTour(), other.getTour()) && Objects.equals(getPerson(), other.getPerson());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    /* relies on the id based hash of tour and person, see IdAble.idHashCode */
    return Objects.hash(getTour(), getPerson());
  }

  /**
   * Output this object as a String
   *
   * @return String containing the value of this
   */
  @Override
  public String toString() {
    return String.format("participation of person (%s) in tour (%s) as %s",
        person != null ? person.getIdsAsString() : "-", tour != null ? tour.getIdsAsString() : "-", role);
  }
}
