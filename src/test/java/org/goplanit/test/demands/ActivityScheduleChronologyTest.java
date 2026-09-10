package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the chronological validity of an activity schedule, which is a property of the demand input rather
 * than of any modification applied to it.
 */
class ActivityScheduleChronologyTest {

  private DiscreteDemands discreteDemands;

  /** ActivitySim style anchor, the day runs from 3AM to 3AM the next day */
  private static final LocalTime DAY_ANCHOR = LocalTime.of(3, 0);

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * Register a tour on a new person, optionally with an outbound and inbound trip
   */
  private Tour registerTour(LocalTime tourStart, LocalTime tourEnd, LocalTime outbound, LocalTime inbound) {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, tourStart, tourEnd, true);

    if (outbound != null) {
      Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour, DirectionBound.OUTBOUND, true);
      outboundTrip.setStartTime(outbound);
    }
    if (inbound != null) {
      Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour, DirectionBound.INBOUND, true);
      inboundTrip.setStartTime(inbound);
    }
    return tour;
  }

  /**
   * An ordinary daytime schedule is in order regardless of whether an anchor is supplied
   */
  @Test
  void testOrderedScheduleIsChronologicalWithAndWithoutAnchor() {
    Tour tour = registerTour(LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(17, 0));
    var schedule = tour.getPerson().getSchedule();

    assertTrue(schedule.isChronological(DAY_ANCHOR), "ordered schedule rejected with an anchor");
    assertTrue(schedule.isChronological(null), "ordered schedule rejected without an anchor");
  }

  /**
   * A schedule crossing midnight is in order when the anchor tells us where the day is cut, and out of order
   * without one, since a null anchor states no crossing occurs
   */
  @Test
  void testMidnightCrossingNeedsTheAnchor() {
    Tour tour = registerTour(LocalTime.of(23, 0), LocalTime.of(1, 30), LocalTime.of(23, 0), LocalTime.of(1, 0));
    var schedule = tour.getPerson().getSchedule();

    assertTrue(schedule.isChronological(DAY_ANCHOR), "midnight crossing rejected despite an anchor");
    assertFalse(schedule.isChronological(null), "midnight crossing accepted without an anchor");
  }

  /**
   * A schedule ending exactly on the anchor closes the day there rather than opening it, so it spans nearly a
   * full day and is in order
   */
  @Test
  void testScheduleEndingOnTheAnchorIsChronological() {
    Tour tour = registerTour(LocalTime.of(4, 0), LocalTime.of(3, 0), LocalTime.of(4, 0), LocalTime.of(2, 0));
    var schedule = tour.getPerson().getSchedule();

    assertTrue(schedule.isChronological(DAY_ANCHOR), "schedule ending on the anchor rejected");
    assertFalse(schedule.isChronological(null), "schedule ending on the anchor accepted without an anchor");
  }

  /**
   * A tour cannot end before the inbound trip that brings the traveller back to it departs
   */
  @Test
  void testTourEndingBeforeItsOwnInboundTripIsRejected() {
    Tour tour = registerTour(LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(19, 0));

    assertFalse(tour.getPerson().getSchedule().isChronological(DAY_ANCHOR),
        "tour ending before its own inbound trip accepted");
  }

  /**
   * A tour cannot start after the first trip it contains departs
   */
  @Test
  void testTourStartingAfterItsOwnOutboundTripIsRejected() {
    Tour tour = registerTour(LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(17, 0));

    assertFalse(tour.getPerson().getSchedule().isChronological(DAY_ANCHOR),
        "tour starting after its own outbound trip accepted");
  }

  /**
   * A nested sub tour must be enclosed by the tour holding it
   */
  @Test
  void testSubTourOutlivingItsParentIsRejected() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    Tour parentTour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, LocalTime.of(8, 0), LocalTime.of(18, 0), true);

    Trip parentOutbound = discreteDemands.getTrips().getFactory().registerNew(
        parentTour, DirectionBound.OUTBOUND, true);
    parentOutbound.setStartTime(LocalTime.of(8, 0));

    // sub tour returns after its parent has already ended
    discreteDemands.getTours().getFactory().registerNew(
        parentTour, null, null, LocalTime.of(12, 0), LocalTime.of(19, 0), true);

    assertFalse(person.getSchedule().isChronological(DAY_ANCHOR), "sub tour outliving its parent accepted");
  }

  /**
   * A nested sub tour that stays within its parent is in order
   */
  @Test
  void testEnclosedSubTourIsChronological() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    Tour parentTour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, LocalTime.of(8, 0), LocalTime.of(18, 0), true);

    Trip parentOutbound = discreteDemands.getTrips().getFactory().registerNew(
        parentTour, DirectionBound.OUTBOUND, true);
    parentOutbound.setStartTime(LocalTime.of(8, 0));

    discreteDemands.getTours().getFactory().registerNew(
        parentTour, null, null, LocalTime.of(12, 0), LocalTime.of(13, 0), true);

    Trip parentInbound = discreteDemands.getTrips().getFactory().registerNew(
        parentTour, DirectionBound.INBOUND, true);
    parentInbound.setStartTime(LocalTime.of(17, 0));

    assertTrue(person.getSchedule().isChronological(DAY_ANCHOR), "enclosed sub tour rejected");
  }

  /**
   * Elements sharing a time bin carry identical times, which is not out of order
   */
  @Test
  void testIdenticalTimesAreChronological() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);

    assertTrue(tour.getPerson().getSchedule().isChronological(DAY_ANCHOR), "identical times rejected");
  }

  /**
   * The container level check reports each offending person and leaves the schedules untouched, and tolerates a
   * null anchor throughout
   */
  @Test
  void testContainerLevelValidationCountsOffendersAndAcceptsNullAnchor() {
    registerTour(LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(17, 0));
    Tour brokenTour =
        registerTour(LocalTime.of(8, 0), LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(19, 0));

    var persons = discreteDemands.getPersons();

    assertEquals(1, PersonUtils.validateSchedulesChronological(persons, DAY_ANCHOR), "offender count incorrect");
    assertEquals(1, PersonUtils.validateSchedulesChronological(persons, null),
        "offender count incorrect for a null anchor");

    assertEquals(LocalTime.of(18, 0), brokenTour.getEndTime(), "validation altered the schedule");
  }

  /**
   * A person without any schedule is not reported, since there is nothing to be out of order
   */
  @Test
  void testPersonWithoutScheduleIsNotReported() {
    discreteDemands.getPersons().getFactory().registerNew();

    assertEquals(0,
        PersonUtils.validateSchedulesChronological(discreteDemands.getPersons(), DAY_ANCHOR),
        "person without a schedule reported as inconsistent");
  }
}
