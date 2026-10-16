package org.goplanit.test.demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.ScheduleJitterType;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PersonScheduleTimeJitterTest {

  private DiscreteDemands discreteDemands;
  private Person person0;

  private static final int MIN_TIME = 0; // 00:00
  private static final int MAX_TIME = 24 * 3600 - 1; // 23:59:59

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  @Test
  void testNestedTourAndSyncedTripsShiftUniformly() {
    var household = discreteDemands.getHouseholds().getFactory().registerNew();
    var person = discreteDemands.getPersons().getFactory().registerNew(household);

    LocalTime originalTourStart = LocalTime.of(8, 0);
    LocalTime originalTourEnd = LocalTime.of(17, 30);

    Tour mainTour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, originalTourStart, originalTourEnd, true);

    Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        mainTour, DirectionBound.OUTBOUND, true);
    outboundTrip.syncStartTimeToTourStartTime();

    Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        mainTour, DirectionBound.INBOUND, true);
    inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(30, ChronoUnit.MINUTES));

    LocalTime originalOutboundStart = outboundTrip.getStartTime();
    LocalTime originalInboundStart = inboundTrip.getStartTime();

    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleRigidJitter(person, 600, false, MIN_TIME, MAX_TIME);

    if (!mainTour.getStartTime().equals(originalTourStart)) {
      long tourDelta = Duration.between(originalTourStart, mainTour.getStartTime()).getSeconds();
      long outboundDelta = Duration.between(originalOutboundStart, outboundTrip.getStartTime()).getSeconds();
      long inboundDelta = Duration.between(originalInboundStart, inboundTrip.getStartTime()).getSeconds();

      assertEquals(tourDelta, outboundDelta, "Outbound trip didn't shift in sync with tour start");
      assertEquals(tourDelta, inboundDelta, "Inbound trip didn't shift in sync with tour end");
    }
  }

  @Test
  void testBoundaryClampingOnComplexSchedule() {
    int numPersons = 100;

    // Create 100 persons, each with a complex schedule near the boundaries
    // (e.g., staggered close to 00:05 and 23:55)
    for (int i = 0; i < numPersons; i++) {
      var person = discreteDemands.getPersons().getFactory().registerNew();

      // Alternate between placing them near the lower boundary and upper boundary
      boolean nearStart = (i % 2 == 0);
      LocalTime start = nearStart ? LocalTime.of(0, 5) : LocalTime.of(23 - 3, 55);
      LocalTime end = nearStart ? LocalTime.of(3, 0) : LocalTime.of(23, 55);

      Tour mainTour = discreteDemands.getTours().getFactory().registerNew(
          person, null, null, start, end, true);

      // Add synced outbound trip
      Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          mainTour, DirectionBound.OUTBOUND, true);
      outboundTrip.syncStartTimeToTourStartTime();

      // Add a nested sub-tour inside the main tour's schedule
      discreteDemands.getTours().getFactory().registerNew(
          mainTour, null, null, start.plusHours(1), start.plusHours(2), true);

      // Add synced inbound trip
      Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          mainTour, DirectionBound.INBOUND, true);
      inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(5, ChronoUnit.MINUTES));

    }

    // Apply uniform jitter across all persons with a massive deviation window (e.g., +/- 2 hours)
    int minAllowed = 0;
    int maxAllowed = (24 * 3600) - 1;
    int maxDeviationSeconds = 7200;

    discreteDemands.getPersons().forEach(p ->
        discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleRigidJitter(p, maxDeviationSeconds, true, minAllowed, maxAllowed)
    );

    // Assert that every single element across all persons respects the simulation period boundaries
    for (Person p : discreteDemands.getPersons()) {
      p.getSchedule().testNested(element -> {
        if (element.getStartTime() != null) {
          int startSec = element.getStartTime().toSecondOfDay();
          assertTrue(startSec >= minAllowed,
              "Start time breached lower bound: " + element.getStartTime());
          assertTrue(startSec <= maxAllowed,
              "Start time breached upper bound: " + element.getStartTime());
        }

        if (element instanceof ParticipantTour) {
          Tour tour = ((ParticipantTour) element).getTour();
          if (tour.getEndTime() != null) {
            int endSec = tour.getEndTime().toSecondOfDay();
            assertTrue(endSec >= minAllowed,
                "Tour end time breached lower bound: " + tour.getEndTime());
            assertTrue(endSec <= maxAllowed,
                "Tour end time breached upper bound: " + tour.getEndTime());
          }
        }
        return true;
      });
    }
  }

  /** ActivitySim style window, 3AM to 3AM the next day, where seconds of day is not a valid measure */
  private static final int WRAP_WINDOW_START = 3 * 3600;
  private static final int WRAP_WINDOW_END = WRAP_WINDOW_START + 24 * 3600;
  private static final int HALF_HOUR = 1800;

  /**
   * Register a tour on a new person, spanning the given times
   */
  private Tour registerTour(LocalTime tourStart, LocalTime tourEnd) {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    return discreteDemands.getTours().getFactory().registerNew(
        person, null, null, tourStart, tourEnd, true);
  }

  private void applyRigidJitterOnWrapWindow(Tour tour) {
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleRigidJitter(
        tour.getPrimaryParticipant(), HALF_HOUR, false, WRAP_WINDOW_START, WRAP_WINDOW_END);
  }

  /**
   * A schedule running past midnight sits legitimately inside a 3AM to 3AM window. Measuring it against seconds
   * of day makes it look like it starts before the window, which used to displace it forwards by hours
   */
  @Test
  void testPostMidnightScheduleIsNotDisplaced() {
    var tourStart = LocalTime.of(23, 0);
    Tour tour = registerTour(tourStart, LocalTime.of(1, 30));

    applyRigidJitterOnWrapWindow(tour);

    assertFalse(tour.getStartTime().isBefore(tourStart), "schedule moved backwards");
    assertTrue(tour.getStartTime().isBefore(tourStart.plusSeconds(HALF_HOUR)),
        "post midnight schedule displaced beyond the allowed deviation: " + tour.getStartTime());
  }

  /**
   * Case C, a schedule ending on the far side of the anchor fills the window, leaving no room to move
   */
  @Test
  void testScheduleEndingAtAnchorFillsWindowAndDoesNotMove() {
    var tourStart = LocalTime.of(4, 0);
    Tour tour = registerTour(tourStart, LocalTime.of(3, 0));

    applyRigidJitterOnWrapWindow(tour);

    assertEquals(tourStart, tour.getStartTime(), "schedule filling the window was still shifted");
  }

  /**
   * Case D prime, a schedule that starts and ends at the anchor is read as spanning a full day, so it cannot move
   */
  @Test
  void testScheduleStartingAndEndingAtAnchorDoesNotMove() {
    var anchor = LocalTime.of(3, 0);
    Tour tour = registerTour(anchor, anchor);

    applyRigidJitterOnWrapWindow(tour);

    assertEquals(anchor, tour.getStartTime(), "schedule assumed to span a day was still shifted");
    assertEquals(anchor, tour.getEndTime(), "schedule assumed to span a day was still shifted");
  }

  /**
   * Case E, a same bin tour away from the anchor spans nothing at all and is free to move within the deviation
   */
  @Test
  void testSameBinTourAwayFromAnchorIsFreeToMove() {
    var noon = LocalTime.of(12, 0);
    int moved = 0;
    for (int index = 0; index < 50; ++index) {
      Tour tour = registerTour(noon, noon);
      applyRigidJitterOnWrapWindow(tour);
      assertFalse(tour.getStartTime().isBefore(noon), "same bin tour moved backwards");
      assertTrue(tour.getStartTime().isBefore(noon.plusSeconds(HALF_HOUR)), "same bin tour moved too far");
      if (!tour.getStartTime().equals(noon)) {
        ++moved;
      }
    }
    assertTrue(moved > 40, "same bin tours barely moved, only " + moved + " of 50");
  }

  /**
   * A schedule ending 10 minutes shy of the window end may only move by those 10 minutes, and the offsets drawn
   * must spread across that room rather than piling onto the boundary
   */
  @Test
  void testRoomNearWindowEndIsRespectedAndNotTruncatedOntoTheBoundary() {
    var tourStart = LocalTime.of(20, 0);
    var tourEnd = LocalTime.of(2, 50);
    var distinctOffsets = new java.util.HashSet<Long>();

    for (int index = 0; index < 50; ++index) {
      Tour tour = registerTour(tourStart, tourEnd);
      applyRigidJitterOnWrapWindow(tour);

      long offsetSeconds = Duration.between(tourStart, tour.getStartTime()).getSeconds();
      assertTrue(offsetSeconds >= 0, "schedule moved backwards");
      assertTrue(offsetSeconds <= 600, "schedule moved past the window end, by " + offsetSeconds + "s");
      distinctOffsets.add(offsetSeconds);
    }

    assertTrue(distinctOffsets.size() > 10,
        "offsets piled onto the boundary rather than spreading, only " + distinctOffsets.size() + " distinct");
  }

  /**
   * A schedule longer than the window cannot be placed, so it is reported and left alone
   */
  @Test
  void testScheduleLongerThanWindowIsLeftAlone() {
    var tourStart = LocalTime.of(4, 0);
    var tourEnd = LocalTime.of(3, 59);
    Tour tour = registerTour(tourStart, tourEnd);

    applyRigidJitterOnWrapWindow(tour);

    assertEquals(tourStart, tour.getStartTime(), "oversized schedule was shifted");
    assertEquals(tourEnd, tour.getEndTime(), "oversized schedule was shifted");
  }

  @Test
  void testDeterministicReproducibility() {
    var person0 = discreteDemands.getPersons().getFactory().registerNew();
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        person0, null, null, LocalTime.of(10, 0),
        LocalTime.of(12, 0), true);

    LocalTime beforeShift = tour.getStartTime();
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleRigidJitter(person0, 1200, false, MIN_TIME, MAX_TIME);
    LocalTime firstShifted = tour.getStartTime();

    tour.setStartTime(beforeShift);
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleRigidJitter(person0, 1200, false, MIN_TIME, MAX_TIME);

    assertEquals(firstShifted, tour.getStartTime());
  }
}