package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.demands.discrete.tour.ScheduleElement;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A schedule holds participations in tours and trips, nothing else. Every place that walks a schedule therefore
 * rejects anything else rather than quietly passing over it, so that introducing a third kind of schedule element -
 * a participation in a single trip, say - forces each of those places to be revisited instead of silently doing
 * nothing there.
 * <p>
 * These tests plant such an unknown element and assert each of those places rejects it. They are not about the
 * element itself, they are about the absence of silent gaps
 * </p>
 */
class UnsupportedScheduleElementTest {

  private DiscreteDemands discreteDemands;

  private static final LocalTime NOON = LocalTime.of(12, 0);

  /** ActivitySim style anchor, the day runs from 3AM to 3AM the next day */
  private static final LocalTime DAY_ANCHOR = LocalTime.of(3, 0);

  /**
   * A schedule element that is neither a participation in a tour nor a trip
   */
  private static final class UnknownScheduleElement implements ScheduleElement {

    @Override
    public LocalTime getStartTime() {
      return NOON;
    }

    @Override
    public String getPurpose() {
      return "unknown";
    }

    @Override
    public ActivitySchedule getSchedule() {
      return null;
    }

    @Override
    public Mode getOutboundMode() {
      return null;
    }

    @Override
    public Mode getInboundMode() {
      return null;
    }
  }

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * Register a person with a tour holding an outbound and inbound trip
   */
  private Tour registerTour() {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, NOON, LocalTime.of(13, 0), true);

    Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        tour, DirectionBound.OUTBOUND, true);
    outboundTrip.setStartTime(NOON);

    Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        tour, DirectionBound.INBOUND, true);
    inboundTrip.setStartTime(LocalTime.of(12, 30));
    return tour;
  }

  @Test
  void testShiftRejectsUnknownScheduleElement() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();
    person.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class, () -> person.getSchedule().shift(600),
        "shifting a schedule accepted an unknown element");
  }

  @Test
  void testSyncTourStartTimesRejectsUnknownScheduleElement() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();
    person.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class, () -> person.getSchedule().syncTourStartTimesToFirstElement(),
        "syncing tour start times accepted an unknown element");
  }

  @Test
  void testChronologyCheckRejectsUnknownScheduleElement() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();
    person.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class, () -> person.getSchedule().isChronological(DAY_ANCHOR),
        "chronology check accepted an unknown element");
  }

  @Test
  void testJitterRejectsUnknownScheduleElement() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();
    person.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class,
        () -> discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
            person, 1800, 3 * 3600, 27 * 3600),
        "jitter accepted an unknown element");
  }

  @Test
  void testRemovePersonRejectsUnknownScheduleElement() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();
    person.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class,
        () -> discreteDemands.getDiscreteDemandsModifier().removePerson(person),
        "removing a person accepted an unknown element on their schedule");
  }

  @Test
  void testRemoveTourRejectsUnknownScheduleElementWithinIt() {
    var tour = registerTour();
    tour.getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class,
        () -> discreteDemands.getDiscreteDemandsModifier().removeTour(tour, false),
        "removing a tour accepted an unknown element within it");
  }

  @Test
  void testDeepCloneRejectsUnknownScheduleElement() {
    var tour = registerTour();
    tour.getPrimaryParticipant().getSchedule().add(new UnknownScheduleElement());

    assertThrows(PlanItRunTimeException.class, () -> discreteDemands.deepClone(),
        "deep cloning accepted an unknown element, which would silently drop it from the copy");
  }

  /**
   * Control: with only the supported elements present none of these operations reject anything, so the tests above
   * fail for the reason intended rather than because the operations throw regardless
   */
  @Test
  void testSupportedScheduleElementsAreAccepted() {
    var tour = registerTour();
    var person = tour.getPrimaryParticipant();

    assertDoesNotThrow(() -> person.getSchedule().shift(600), "shift rejected a supported schedule");
    assertDoesNotThrow(() -> person.getSchedule().syncTourStartTimesToFirstElement(),
        "sync rejected a supported schedule");
    assertDoesNotThrow(() -> person.getSchedule().isChronological(DAY_ANCHOR),
        "chronology check rejected a supported schedule");
    assertDoesNotThrow(() -> discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
        person, 1800, 3 * 3600, 27 * 3600), "jitter rejected a supported schedule");
    assertDoesNotThrow(() -> discreteDemands.deepClone(), "deep clone rejected a supported schedule");
    assertDoesNotThrow(() -> discreteDemands.getDiscreteDemandsModifier().removePerson(person),
        "removing a person rejected a supported schedule");
  }
}
