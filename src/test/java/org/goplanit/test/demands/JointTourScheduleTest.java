package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
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
   * The mirror of the previous case. A participant's own tour that precedes the joint tour in the same bin may not
   * be drawn past it. The joint tour was drawn in its own pass and cannot be moved, so it is the own tour that has
   * to give way, which only a backward clamp can achieve: moving the joint tour up instead would take it away from
   * the other participants who share it
   */
  @Test
  void testOwnTourPrecedingASharedTourIsNotDrawnPastIt() {
    var primary = registerPerson();
    var accompanying = registerPerson();

    /* the accompanying participant's own tour runs wholly within the bin and comes first on their schedule */
    Tour ownTour = registerTourFor(accompanying, NOON, NOON, NOON, NOON);

    Tour jointTour = registerTourFor(primary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(jointTour, accompanying);

    applySubBinJitterToAll();

    assertFalse(ownTour.getStartTime().isAfter(jointTour.getStartTime()),
        String.format("own tour starts (%s) after the joint tour that follows it (%s)",
            ownTour.getStartTime(), jointTour.getStartTime()));
    assertFalse(ownTour.getEndTime().isAfter(jointTour.getStartTime()),
        String.format("own tour ends (%s) after the joint tour that follows it starts (%s)",
            ownTour.getEndTime(), jointTour.getStartTime()));
    for (var element : ownTour.getSchedule()) {
      assertFalse(element.getStartTime().isAfter(jointTour.getStartTime()),
          String.format("trip of the own tour (%s) departs after the joint tour that follows it starts (%s)",
              element.getStartTime(), jointTour.getStartTime()));
    }
  }

  /**
   * A tour that still has participants must still have a primary among them, so when the primary is the one leaving
   * the next participant takes over. Without this the tour survives with nobody its shared information is attributed
   * to, which the XML writer rightly refuses to persist
   */
  @Test
  void testRemovingThePrimaryPromotesTheNextParticipant() {
    var primary = registerPerson();
    var accompanying = registerPerson();

    Tour jointTour = registerTourFor(primary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(jointTour, accompanying);

    discreteDemands.getDiscreteDemandsModifier().clearPersonSchedule(primary);

    assertTrue(jointTour.hasPrimaryParticipant(), "joint tour left without a primary participant");
    assertEquals(accompanying, jointTour.getPrimaryParticipant(),
        "remaining participant did not take over as primary");
    assertFalse(jointTour.hasMultipleParticipants(), "departed participant still counted");

    /* the promoted participation must have taken the old one's place on the schedule, not been appended */
    var promoted = jointTour.getPrimaryParticipation();
    assertEquals(promoted, accompanying.getSchedule().getFirst(),
        "promoted participation is not the one on the participant's schedule");
    assertTrue(promoted.isPrimary(), "participation on the schedule still carries the accompanying role");
  }

  /**
   * KNOWN FAILURE, see PLANit issue "sub bin jitter cannot order a person taking part in several shared tours".
   * <p>
   * A person sits between two shared tours that meet at a bin boundary, one ending where the next begins, with a
   * tour of their own in between. Each shared tour is drawn once for all of its participants, which is what keeps
   * them consistent for everyone taking part, but it also means neither knows about the other: both draw forward
   * into the same bin and come out overlapping. Their times cannot be moved without breaking the other participants'
   * schedules, so the own tour in between has no time left that satisfies both, and the schedule stays out of order.
   * </p>
   * <p>
   * Until this is solved the ActivitySim reader drops the schedules of the persons this happens to, which it reports,
   * so the conversion never emits a schedule that runs backwards. This test asserts the outcome we want rather than
   * the one we accept, and therefore fails
   * </p>
   */
  @Test
  void testPersonInTwoSharedToursMeetingAtABinBoundaryIsOrdered() {
    var accompanying = registerPerson();

    /* the first shared tour closes in the noon bin holding three of its points, so its end is drawn in the last of
     * three sub-bins and therefore lands in the final third of the bin */
    var firstPrimary = registerPerson();
    Tour firstSharedTour = registerTourFor(firstPrimary, LocalTime.of(11, 30), NOON, NOON, NOON);
    addAccompanyingParticipant(firstSharedTour, accompanying);

    /* their own tour, wholly within that same bin */
    Tour ownTour = registerTourFor(accompanying, NOON, NOON, NOON, NOON);

    /* the second shared tour opens in that bin with two of its points, so its first trip is drawn in the first of
     * two sub-bins and lands in the first half, i.e. before the first shared tour has ended */
    var secondPrimary = registerPerson();
    Tour secondSharedTour = registerTourFor(secondPrimary, NOON, LocalTime.of(13, 0), NOON, NOON);
    addAccompanyingParticipant(secondSharedTour, accompanying);

    applySubBinJitterToAll();

    assertFalse(firstSharedTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("first shared tour ends (%s) after the own tour that follows it starts (%s)",
            firstSharedTour.getEndTime(), ownTour.getStartTime()));
    assertFalse(ownTour.getEndTime().isAfter(secondSharedTour.getStartTime()),
        String.format("own tour ends (%s) after the second shared tour that follows it starts (%s)",
            ownTour.getEndTime(), secondSharedTour.getStartTime()));
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
   * A tour belongs to exactly one person, so a second primary participant is invalid rather than merely unusual
   */
  @Test
  void testTourRejectsASecondPrimaryParticipant() {
    var primary = registerPerson();
    Tour tour = registerTourFor(primary, NOON, LocalTime.of(13, 0), NOON, LocalTime.of(12, 30));

    var second = registerPerson();
    assertThrows(PlanItRunTimeException.class,
        () -> tour.addParticipant(second, TourParticipantRole.PRIMARY),
        "a second primary participant was accepted");

    assertEquals(primary, tour.getPrimaryParticipant(), "primary participant changed by the rejected attempt");
    assertTrue(tour.hasPrimaryParticipant(), "tour lost its primary participant");
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
