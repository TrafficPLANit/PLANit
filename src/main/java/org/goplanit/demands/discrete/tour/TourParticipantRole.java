package org.goplanit.demands.discrete.tour;

/**
 * The role a person takes on a tour they participate in. Deliberately mode agnostic: it expresses who the tour
 * belongs to, not who operates a vehicle, because a joint tour may just as well be made on foot or by public
 * transport where no such distinction exists. Who drives the trips by a mode that needs a driver is the driver value
 * of the participation, {@link ParticipantTour#getDriver()}; where it is not known, the primary participant is
 * assumed to drive, {@link ParticipantTour#isAssumedAsDriver()}.
 */
public enum TourParticipantRole {

  /** the person the tour belongs to. A tour has exactly one primary participant */
  PRIMARY,

  /** a person travelling along with the primary participant for the entirety of the tour */
  ACCOMPANYING;

  /**
   * Verify if this is the primary role
   *
   * @return true when primary, false otherwise
   */
  public boolean isPrimary() {
    return this == PRIMARY;
  }
}
