package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for tours shared by more than one participant, i.e. joint tours. Such a tour is held once and appears on
 * each participant's schedule through its own {@link ParticipantTour}, so the times within it are common to all of
 * them and must be drawn exactly once.
 */
class JointTourScheduleTest {

  private DiscreteDemands discreteDemands;

  /** ActivitySim style window, 3AM to 3AM the next day */
  private static final int WINDOW_START = 3 * 3600;
  private static final int WINDOW_END = WINDOW_START + 24 * 3600;

  /** spread width, a 30 minute bin at ratio 1 */
  private static final int BIN_SECONDS = 1800;

  private static final LocalTime NOON = LocalTime.of(12, 0);

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * Register a tour with its outbound and inbound trip for the given primary participant
   */
  private Tour registerTourFor(
      Person primaryParticipant, LocalTime tourStart, LocalTime tourEnd, LocalTime outbound, LocalTime inbound) {
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        primaryParticipant, null, null, tourStart, tourEnd, true);

    Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        tour, DirectionBound.OUTBOUND, true);
    outboundTrip.setStartTime(outbound);

    Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        tour, DirectionBound.INBOUND, true);
    inboundTrip.setStartTime(inbound);
    return tour;
  }

  /**
   * Add an accompanying participant to an existing tour and place that participation on their schedule
   */
  private ParticipantTour addAccompanyingParticipant(Tour tour, Person person) {
    var participation = tour.addParticipant(person, TourParticipantRole.ACCOMPANYING);
    person.getSchedule().add(participation);
    return participation;
  }

  private Person registerPerson() {
    return discreteDemands.getPersons().getFactory().registerNew();
  }

  private void applySubBinJitterToAll() {
    discreteDemands.getDiscreteDemandsModifier().adjustPersonsScheduleSubBinJitter(
        BIN_SECONDS, WINDOW_START, WINDOW_END);
  }

  /**
   * A tour of three participants sits wholly within one bin. Its times are common to all of them, so they are drawn
   * once: were they drawn once per participant, the later draws would start from the already moved times and walk
   * the tour out of its bin
   */
  @Test
  void testSharedTourIsDrawnOnceAndStaysWithinItsBin() {
    var primary = registerPerson();
    Tour jointTour = registerTourFor(primary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(jointTour, registerPerson());
    addAccompanyingParticipant(jointTour, registerPerson());

    applySubBinJitterToAll();

    var binEnd = NOON.plusSeconds(BIN_SECONDS);
    assertFalse(jointTour.getStartTime().isBefore(NOON), "joint tour start moved before its bin");
    assertTrue(jointTour.getEndTime().isBefore(binEnd),
        "joint tour end moved beyond its bin, indicating it was drawn more than once: " + jointTour.getEndTime());
    for (var element : jointTour.getSchedule()) {
      assertFalse(element.getStartTime().isBefore(NOON), "trip of joint tour moved before its bin");
      assertTrue(element.getStartTime().isBefore(binEnd), "trip of joint tour moved beyond its bin");
    }
  }

  /**
   * An accompanying participant with a trip of their own in the same bin as the joint tour's end may not depart
   * before that tour has ended, even though the tour's times were drawn elsewhere
   */
  @Test
  void testAccompanyingParticipantDoesNotDepartBeforeTheSharedTourEnds() {
    var primary = registerPerson();
    var accompanying = registerPerson();

    Tour jointTour = registerTourFor(primary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(jointTour, accompanying);

    /* the accompanying participant's own tour departs in the same bin, after the joint tour */
    Tour ownTour = registerTourFor(accompanying, NOON, LocalTime.of(14, 0), NOON, LocalTime.of(13, 30));

    applySubBinJitterToAll();

    var ownDeparture = ownTour.getSchedule().getFirst().getStartTime();
    assertFalse(ownDeparture.isBefore(jointTour.getEndTime()),
        String.format("own trip (%s) departs before the joint tour it follows ends (%s)",
            ownDeparture, jointTour.getEndTime()));
  }

  /**
   * Drawing is seeded per participant, so the same input yields the same output for joint tours as it does for
   * any other
   */
  @Test
  void testJointTourJitterIsReproducible() {
    var primary = registerPerson();
    Tour firstRunTour = registerTourFor(primary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(firstRunTour, registerPerson());
    applySubBinJitterToAll();
    var firstRunStart = firstRunTour.getStartTime();
    var firstRunEnd = firstRunTour.getEndTime();

    setUp(); // identical scenario from scratch
    var repeatPrimary = registerPerson();
    Tour secondRunTour = registerTourFor(repeatPrimary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(secondRunTour, registerPerson());
    applySubBinJitterToAll();

    assertEquals(firstRunStart, secondRunTour.getStartTime(), "joint tour start not reproducible");
    assertEquals(firstRunEnd, secondRunTour.getEndTime(), "joint tour end not reproducible");
  }

  /**
   * A shared tour moves with its primary participant. Shifting an accompanying participant's schedule leaves it
   * where it is, since it cannot move for one participant without moving for all
   */
  @Test
  void testShiftFromAccompanyingScheduleLeavesSharedTourUntouched() {
    var primary = registerPerson();
    var accompanying = registerPerson();
    Tour jointTour = registerTourFor(primary, NOON, LocalTime.of(13, 0), NOON, LocalTime.of(12, 30));
    addAccompanyingParticipant(jointTour, accompanying);

    accompanying.getSchedule().shift(600);
    assertEquals(NOON, jointTour.getStartTime(), "shared tour moved from an accompanying participant's schedule");

    primary.getSchedule().shift(600);
    assertEquals(NOON.plusSeconds(600), jointTour.getStartTime(),
        "shared tour did not move with its primary participant");
  }

  /**
   * Removing an accompanying participant only ends their participation, the tour remains for the others
   */
  @Test
  void testRemovingAccompanyingParticipantKeepsTheTour() {
    var primary = registerPerson();
    var accompanying = registerPerson();
    Tour jointTour = registerTourFor(primary, NOON, LocalTime.of(13, 0), NOON, LocalTime.of(12, 30));
    addAccompanyingParticipant(jointTour, accompanying);

    discreteDemands.getDiscreteDemandsModifier().removePerson(accompanying);

    assertEquals(1, jointTour.getParticipantTours().size(), "participation not removed");
    assertEquals(primary, jointTour.getPrimaryParticipant(), "primary participant lost");
    assertTrue(discreteDemands.getTours().containsKey(jointTour.getId()), "tour removed while a participant remains");
    assertEquals(1, primary.getSchedule().size(), "primary participant's schedule altered");
  }

  /**
   * Removing the last participant leaves nobody on the tour, so the tour itself goes with them
   */
  @Test
  void testRemovingLastParticipantRemovesTheTour() {
    var primary = registerPerson();
    var accompanying = registerPerson();
    Tour jointTour = registerTourFor(primary, NOON, LocalTime.of(13, 0), NOON, LocalTime.of(12, 30));
    addAccompanyingParticipant(jointTour, accompanying);

    discreteDemands.getDiscreteDemandsModifier().removePerson(accompanying);
    discreteDemands.getDiscreteDemandsModifier().removePerson(primary);

    assertTrue(jointTour.getParticipantTours().isEmpty(), "participations remain");
    assertFalse(discreteDemands.getTours().containsKey(jointTour.getId()),
        "tour retained although it has no participants left");
  }
}
