package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.event.discrete.DiscreteDemandsModificationEvent;
import org.goplanit.demands.event.discrete.DiscreteDemandsModifierListener;
import org.goplanit.demands.event.discrete.RemoveHouseholdEvent;
import org.goplanit.demands.event.discrete.RemovePersonEvent;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourParticipantRole;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.logging.Logging;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.goplanit.test.demands.DiscreteDemandsTestUtils.addAccompanyingParticipant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for tours shared by more than one participant, i.e. joint tours. Such a tour is held once and appears on
 * each participant's schedule through its own {@link ParticipantTour}, so the times within it are common to all of
 * them and must be drawn exactly once.
 */
class JointTourScheduleTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** ActivitySim style window, 3AM to 3AM the next day */
  private static final int WINDOW_START = 3 * 3600;
  private static final int WINDOW_END = WINDOW_START + 24 * 3600;

  /** spread width, a 30 minute bin at ratio 1 */
  private static final int BIN_SECONDS = 1800;

  private static final LocalTime NOON = LocalTime.of(12, 0);

  /**
   * Assert a tour of one outbound and one inbound trip carries the given times, its start being that of its outbound
   * trip
   */
  private static void assertTourTimes(Tour tour, LocalTime outbound, LocalTime inbound, LocalTime end) {
    assertEquals(outbound, tour.getStartTime(), "tour start drawn differently");
    assertEquals(outbound, tour.getSchedule().getFirst().getStartTime(), "outbound trip drawn differently");
    assertEquals(inbound, tour.getSchedule().getLast(false).getStartTime(), "inbound trip drawn differently");
    assertEquals(end, tour.getEndTime(), "tour end drawn differently");
  }

  private DiscreteDemands discreteDemands;

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

  private Person registerPerson() {
    return discreteDemands.getPersons().getFactory().registerNew();
  }

  private List<Person> applySubBinJitterToAll() {
    return discreteDemands.getDiscreteDemandsModifier().adjustPersonsScheduleSubBinJitter(
        BIN_SECONDS, WINDOW_START, WINDOW_END);
  }

  /**
   * Register a shared tour wholly within the noon bin, accompanied by the given person, whose new primary participant
   * has two tours of their own after it in that bin
   */
  private Tour registerNoonSharedTourWhosePrimaryHasTwoOwnToursAfterIt(Person accompanying) {
    var busyPrimary = registerPerson();
    Tour sharedTour = registerTourFor(busyPrimary, NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(sharedTour, accompanying);
    registerTourFor(busyPrimary, NOON, NOON, NOON, NOON);
    registerTourFor(busyPrimary, NOON, NOON, NOON, NOON);
    return sharedTour;
  }

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
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
   * be drawn past it. The joint tour is drawn once for all its participants and leaves this participant room in the
   * bin for the own tour before it, so the own tour keeps a duration and ends no later than the joint tour starts
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

    assertTrue(ownTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("own tour has no duration, it starts and ends at %s", ownTour.getStartTime()));
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
   * A person sits between two shared tours that meet in one bin, the first ending where the next begins, with a tour
   * of their own in between. Each shared tour is drawn once for all of its participants, so it cannot move for this
   * person afterwards. The first is drawn first and the second after its end, leaving room between them for the own
   * tour, which keeps a duration
   */
  @Test
  void testPersonInTwoSharedToursMeetingAtABinBoundaryIsOrdered() {
    var accompanying = registerPerson();

    /* the first shared tour closes in the noon bin with three of its points */
    var firstPrimary = registerPerson();
    Tour firstSharedTour = registerTourFor(firstPrimary, LocalTime.of(11, 30), NOON, NOON, NOON);
    addAccompanyingParticipant(firstSharedTour, accompanying);

    /* their own tour, wholly within that same bin */
    Tour ownTour = registerTourFor(accompanying, NOON, NOON, NOON, NOON);

    /* the second shared tour opens in that bin with two of its points */
    var secondPrimary = registerPerson();
    Tour secondSharedTour = registerTourFor(secondPrimary, NOON, LocalTime.of(13, 0), NOON, NOON);
    addAccompanyingParticipant(secondSharedTour, accompanying);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertFalse(firstSharedTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("first shared tour ends (%s) after the own tour that follows it starts (%s)",
            firstSharedTour.getEndTime(), ownTour.getStartTime()));
    assertFalse(ownTour.getEndTime().isAfter(secondSharedTour.getStartTime()),
        String.format("own tour ends (%s) after the second shared tour that follows it starts (%s)",
            ownTour.getEndTime(), secondSharedTour.getStartTime()));
    assertTrue(ownTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("own tour between the shared tours has no duration, it starts and ends at %s",
            ownTour.getStartTime()));
  }

  /**
   * A person takes part in two shared tours with a different participant on each, the first coming home in the
   * 16:30 bin and the next leaving in it. The first tour's last trip and its end share that bin, as do the next
   * tour's two trips. Drawn on their own, the first ends in the second half of the bin and the next leaves in the
   * first half. Drawn in order, the next leaves no earlier than the first has ended
   */
  @Test
  void testTwoSharedToursMeetingInAPeriodAreOrdered() {
    var common = registerPerson();
    var halfPastFour = LocalTime.of(16, 30);

    Tour firstSharedTour = registerTourFor(
        registerPerson(), LocalTime.of(15, 0), halfPastFour, LocalTime.of(15, 0), halfPastFour);
    addAccompanyingParticipant(firstSharedTour, common);

    Tour nextSharedTour = registerTourFor(
        registerPerson(), halfPastFour, LocalTime.of(17, 30), halfPastFour, halfPastFour);
    addAccompanyingParticipant(nextSharedTour, common);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertFalse(nextSharedTour.getStartTime().isBefore(firstSharedTour.getEndTime()),
        String.format("next shared tour starts (%s) before the first one ends (%s)",
            nextSharedTour.getStartTime(), firstSharedTour.getEndTime()));
  }

  /**
   * A person takes part in two shared tours that both start and end in the noon bin, so the tours tie on both and
   * only that person's schedule says which comes first. The tour first on their schedule is registered last, so its
   * id does not put it first either. After the jitter the later tour starts no earlier than the earlier one ends
   */
  @Test
  void testTwoSharedToursWithinOnePeriodAreOrdered() {
    var common = registerPerson();
    Tour laterSharedTour = registerTourFor(registerPerson(), NOON, NOON, NOON, NOON);
    Tour earlierSharedTour = registerTourFor(registerPerson(), NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(earlierSharedTour, common);
    addAccompanyingParticipant(laterSharedTour, common);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertFalse(laterSharedTour.getStartTime().isBefore(earlierSharedTour.getEndTime()),
        String.format("later shared tour starts (%s) before the earlier one ends (%s)",
            laterSharedTour.getStartTime(), earlierSharedTour.getEndTime()));
  }

  /**
   * A person takes part in two shared tours wholly within the noon bin. The second tour's other participant has two
   * tours of their own after it in that bin, so needs it done early in the bin, while the first tour, drawn first,
   * left the person room for the second tour only and so ends too late for that. The first tour's end is moved
   * earlier to make room, staying in its bin and after its last trip, and the second tour keeps a duration
   */
  @Test
  void testEarlierSharedTourEndMovesToMakeRoom() {
    var common = registerPerson();
    Tour earlierSharedTour = registerTourFor(registerPerson(), NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(earlierSharedTour, common);
    Tour laterSharedTour = registerNoonSharedTourWhosePrimaryHasTwoOwnToursAfterIt(common);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    var earlierEnd = earlierSharedTour.getEndTime();
    var earlierLastTripStart = earlierSharedTour.getSchedule().getLast(false).getStartTime();
    assertTrue(!earlierEnd.isBefore(NOON) && earlierEnd.isBefore(NOON.plusSeconds(BIN_SECONDS)),
        String.format("earlier shared tour end (%s) moved out of its bin", earlierEnd));
    assertFalse(earlierEnd.isBefore(earlierLastTripStart),
        String.format("earlier shared tour ends (%s) before its last trip departs (%s)",
            earlierEnd, earlierLastTripStart));
    assertFalse(laterSharedTour.getStartTime().isBefore(earlierEnd),
        String.format("later shared tour starts (%s) before the earlier one ends (%s)",
            laterSharedTour.getStartTime(), earlierEnd));
    assertTrue(laterSharedTour.getEndTime().isAfter(laterSharedTour.getStartTime()),
        String.format("later shared tour has no duration, it starts and ends at %s",
            laterSharedTour.getStartTime()));
  }

  /**
   * As the previous case, but the person also has a tour of their own before the first shared tour, so the first
   * tour lies in the middle of the bin and its last trip already departs after the second tour's other participant
   * needs that tour done. Its end cannot move far enough, so the order is kept at the cost of room: the second tour's
   * points take one time, no earlier than the first tour's end, and no day is out of order
   */
  @Test
  void testOrderIsKeptWhenNoEndCanMoveFarEnough() {
    var common = registerPerson();
    registerTourFor(common, NOON, NOON, NOON, NOON);
    Tour earlierSharedTour = registerTourFor(registerPerson(), NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(earlierSharedTour, common);
    Tour laterSharedTour = registerNoonSharedTourWhosePrimaryHasTwoOwnToursAfterIt(common);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertFalse(laterSharedTour.getStartTime().isBefore(earlierSharedTour.getEndTime()),
        String.format("later shared tour starts (%s) before the earlier one ends (%s)",
            laterSharedTour.getStartTime(), earlierSharedTour.getEndTime()));
    for (var element : laterSharedTour.getSchedule()) {
      assertEquals(laterSharedTour.getStartTime(), element.getStartTime(),
          "trip of the later shared tour does not take the tour's one time");
    }
    assertEquals(laterSharedTour.getStartTime(), laterSharedTour.getEndTime(),
        "end of the later shared tour does not take the tour's one time");
  }

  /**
   * A shared tour wholly within the noon bin is given a minimum duration as long as the bin. Its accompanying
   * participant has a tour of their own after it in that bin and then a shared tour leaving in it. The minimum
   * duration is applied in the shared tour's own draw, within the room it leaves that participant, so it pushes
   * neither the own tour nor the later shared tour: the participant's day stays in order and the own tour keeps a
   * duration
   */
  @Test
  void testSharedTourMinDurationTakesNoRoomOfLaterPoints() {
    var common = registerPerson();
    Tour minDurationTour = registerTourFor(registerPerson(), NOON, NOON, NOON, NOON);
    addAccompanyingParticipant(minDurationTour, common);
    Tour ownTour = registerTourFor(common, NOON, NOON, NOON, NOON);
    var onePm = LocalTime.of(13, 0);
    Tour laterSharedTour = registerTourFor(registerPerson(), NOON, onePm, NOON, onePm);
    addAccompanyingParticipant(laterSharedTour, common);

    var outOfOrder = discreteDemands.getDiscreteDemandsModifier().adjustPersonsScheduleSubBinJitter(
        BIN_SECONDS, WINDOW_START, WINDOW_END, Set.of(minDurationTour), BIN_SECONDS);

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertFalse(minDurationTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("shared tour with a minimum duration ends (%s) after the own tour that follows it starts (%s)",
            minDurationTour.getEndTime(), ownTour.getStartTime()));
    assertTrue(ownTour.getEndTime().isAfter(ownTour.getStartTime()),
        String.format("own tour has no duration, it starts and ends at %s", ownTour.getStartTime()));
    assertFalse(ownTour.getEndTime().isAfter(laterSharedTour.getStartTime()),
        String.format("own tour ends (%s) after the later shared tour starts (%s)",
            ownTour.getEndTime(), laterSharedTour.getStartTime()));
  }

  /**
   * A person's two shared tours end and start in different bins and their own tour lies in a bin of its own, so
   * nothing of theirs meets another tour in a bin. Each is then drawn over its whole bin, as a tour on its own is,
   * and the expected times are those draws
   */
  @Test
  void testSharedToursInDifferentPeriodsAreDrawnAsBefore() {
    var common = registerPerson();
    Tour morningSharedTour = registerTourFor(
        registerPerson(), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
    addAccompanyingParticipant(morningSharedTour, common);
    Tour afternoonSharedTour = registerTourFor(
        registerPerson(), LocalTime.of(14, 0), LocalTime.of(15, 30), LocalTime.of(14, 0), LocalTime.of(15, 30));
    addAccompanyingParticipant(afternoonSharedTour, common);
    var fivePm = LocalTime.of(17, 0);
    Tour ownTour = registerTourFor(common, fivePm, fivePm, fivePm, fivePm);

    var outOfOrder = applySubBinJitterToAll();

    assertTrue(outOfOrder.isEmpty(), "a day is out of order");
    assertTourTimes(morningSharedTour, LocalTime.of(10, 4, 50), LocalTime.of(11, 9, 17), LocalTime.of(11, 18, 34));
    assertTourTimes(afternoonSharedTour, LocalTime.of(14, 28, 35), LocalTime.of(15, 43, 22), LocalTime.of(15, 47, 56));
    assertTourTimes(ownTour, LocalTime.of(17, 1, 44), LocalTime.of(17, 17, 43), LocalTime.of(17, 22, 27));
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

  /**
   * A person takes part in two shared tours that overlap, leading the first and accompanying the second, so their day
   * cannot be put in order. Clearing their schedule with their shared tours removes both tours, with their trips, for
   * everyone taking part, while the person stays and the other participants keep their own tours
   */
  @Test
  void testClearingAPersonWithTheirSharedToursRemovesThemForAll() {
    var person = registerPerson();
    var firstOther = registerPerson();
    var secondOther = registerPerson();

    Tour firstSharedTour = registerTourFor(person, NOON, LocalTime.of(14, 0), NOON, LocalTime.of(13, 30));
    addAccompanyingParticipant(firstSharedTour, firstOther);
    Tour secondSharedTour = registerTourFor(
        secondOther, LocalTime.of(13, 0), LocalTime.of(15, 0), LocalTime.of(13, 0), LocalTime.of(14, 30));
    addAccompanyingParticipant(secondSharedTour, person);

    var fivePm = LocalTime.of(17, 0);
    Tour firstOtherOwnTour = registerTourFor(firstOther, fivePm, LocalTime.of(18, 0), fivePm, LocalTime.of(17, 30));
    Tour secondOtherOwnTour = registerTourFor(secondOther, fivePm, LocalTime.of(18, 0), fivePm, LocalTime.of(17, 30));

    int removedSharedTours =
        discreteDemands.getDiscreteDemandsModifier().clearPersonScheduleAndSharedTours(person);

    assertEquals(2, removedSharedTours, "number of shared tours removed");
    assertTrue(person.getSchedule().isEmpty(), "person's schedule not cleared");
    assertTrue(discreteDemands.getPersons().containsKey(person.getId()), "person removed");
    for (var sharedTour : List.of(firstSharedTour, secondSharedTour)) {
      assertFalse(discreteDemands.getTours().containsKey(sharedTour.getId()), "shared tour kept in the demands");
      assertTrue(sharedTour.getParticipantTours().isEmpty(), "shared tour kept a participant");
    }
    assertEquals(2, discreteDemands.getTours().size(), "tours other than the shared ones removed");
    assertEquals(4, discreteDemands.getTrips().size(), "trips of the shared tours kept, or others removed");
    assertEquals(1, firstOther.getSchedule().size(), "first other participant's schedule not as expected");
    assertEquals(firstOtherOwnTour.getPrimaryParticipation(), firstOther.getSchedule().getFirst(),
        "first other participant lost their own tour");
    assertEquals(1, secondOther.getSchedule().size(), "second other participant's schedule not as expected");
    assertEquals(secondOtherOwnTour.getPrimaryParticipation(), secondOther.getSchedule().getFirst(),
        "second other participant lost their own tour");
  }

  /**
   * Removing a household removes each of its persons with everything they do: a household of two sharing a joint tour,
   * each with a tour of their own, leaves no person, tour or trip of theirs, while another household keeps its person
   * and their tour. A listener hears each of the two persons removed and the household
   */
  @Test
  void testRemovingAHouseholdRemovesItsPersonsToursAndTrips() {
    var removedHousehold = discreteDemands.getHouseholds().getFactory().registerNew();
    var keptHousehold = discreteDemands.getHouseholds().getFactory().registerNew();
    var first = discreteDemands.getPersons().getFactory().registerNew(removedHousehold);
    var second = discreteDemands.getPersons().getFactory().registerNew(removedHousehold);
    var other = discreteDemands.getPersons().getFactory().registerNew(keptHousehold);

    var fivePm = LocalTime.of(17, 0);
    Tour jointTour = registerTourFor(first, NOON, LocalTime.of(14, 0), NOON, LocalTime.of(13, 30));
    addAccompanyingParticipant(jointTour, second);
    registerTourFor(first, fivePm, LocalTime.of(18, 0), fivePm, LocalTime.of(17, 30));
    registerTourFor(second, fivePm, LocalTime.of(18, 0), fivePm, LocalTime.of(17, 30));
    Tour otherTour = registerTourFor(other, fivePm, LocalTime.of(18, 0), fivePm, LocalTime.of(17, 30));

    var modifier = discreteDemands.getDiscreteDemandsModifier();
    var heard = new ArrayList<DiscreteDemandsModificationEvent>();
    DiscreteDemandsModifierListener listener = heard::add;
    modifier.addListener(listener, RemovePersonEvent.EVENT_TYPE);
    modifier.addListener(listener, RemoveHouseholdEvent.EVENT_TYPE);

    modifier.removeHousehold(removedHousehold, discreteDemands.getPersons().createGroupByIndex(Person::getHousehold));

    assertFalse(discreteDemands.getHouseholds().containsKey(removedHousehold.getId()), "removed household kept");
    assertFalse(discreteDemands.getPersons().containsKey(first.getId()), "first person of the household kept");
    assertFalse(discreteDemands.getPersons().containsKey(second.getId()), "second person of the household kept");
    assertEquals(1, discreteDemands.getTours().size(), "tours of the household kept, or others removed");
    assertTrue(discreteDemands.getTours().containsKey(otherTour.getId()), "other household's tour removed");
    assertEquals(2, discreteDemands.getTrips().size(), "trips of the household kept, or others removed");
    assertTrue(discreteDemands.getHouseholds().containsKey(keptHousehold.getId()), "other household removed");
    assertTrue(discreteDemands.getPersons().containsKey(other.getId()), "other household's person removed");
    assertEquals(1, other.getSchedule().size(), "other household's person lost their tour");
    assertEquals(2, heard.stream().filter(event -> event instanceof RemovePersonEvent).count(),
        "persons removed not heard each");
    assertEquals(1, heard.stream().filter(event -> event instanceof RemoveHouseholdEvent).count(),
        "household removed not heard once");
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUpLogger() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(JointTourScheduleTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDownLogger() {
    Logging.closeLogger(LOGGER);
  }
}
