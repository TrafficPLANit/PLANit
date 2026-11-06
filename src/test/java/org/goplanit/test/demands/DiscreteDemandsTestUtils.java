package org.goplanit.test.demands;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;

/**
 * Builds discrete demands in tests
 */
public final class DiscreteDemandsTestUtils {

  /**
   * Add an accompanying participant to an existing tour, without knowing whether they drive, and place that
   * participation on their schedule
   *
   * @param tour to add the participant to
   * @param person to add
   * @return the participation
   */
  public static ParticipantTour addAccompanyingParticipant(Tour tour, Person person) {
    return addAccompanyingParticipant(tour, person, null);
  }

  /**
   * Add an accompanying participant to an existing tour and place that participation on their schedule
   *
   * @param tour to add the participant to
   * @param person to add
   * @param driver whether the person drives the tour's trips by a mode that needs a driver, null when not known
   * @return the participation
   */
  public static ParticipantTour addAccompanyingParticipant(Tour tour, Person person, Boolean driver) {
    var participation = tour.addParticipant(person, TourParticipantRole.ACCOMPANYING, driver);
    person.getSchedule().add(participation);
    return participation;
  }
}
