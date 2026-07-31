package org.goplanit.test.demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
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

    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleUniformJitter(
        person, 600, false, MIN_TIME, MAX_TIME);

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
        discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleUniformJitter(
            p, maxDeviationSeconds, true, minAllowed, maxAllowed)
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

        if (element instanceof Tour) {
          Tour tour = (Tour) element;
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

  @Test
  void testDeterministicReproducibility() {
    var person0 = discreteDemands.getPersons().getFactory().registerNew();
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        person0, null, null, LocalTime.of(10, 0),
        LocalTime.of(12, 0), true);

    LocalTime beforeShift = tour.getStartTime();
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleUniformJitter(
        person0, 1200,false, MIN_TIME, MAX_TIME);
    LocalTime firstShifted = tour.getStartTime();

    tour.setStartTime(beforeShift);
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleUniformJitter(
        person0, 1200, false, MIN_TIME, MAX_TIME);

    assertEquals(firstShifted, tour.getStartTime());
  }
}