package org.goplanit.test.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.ScheduleJitterType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScheduleJitterType#SUB_BIN}, which splits each time bin into as many sub-bins as it holds
 * points and draws each point within its own sub-bin.
 */
class SubBinScheduleJitterTest {

  private DiscreteDemands discreteDemands;

  /** ActivitySim style window, 3AM to 3AM the next day */
  private static final int WINDOW_START = 3 * 3600;
  private static final int WINDOW_END = WINDOW_START + 24 * 3600;

  /** spread width, a 30 minute bin at ratio 1 */
  private static final int BIN_SECONDS = 1800;

  @BeforeEach
  void setUp() {
    IdGenerator.reset(IdGroupingToken.collectGlobalToken());
    discreteDemands = new DiscreteDemands(IdGroupingToken.collectGlobalToken());
  }

  /**
   * Register a tour on a new person with an outbound and inbound trip at the given times
   */
  private Tour registerTour(LocalTime tourStart, LocalTime tourEnd, LocalTime outbound, LocalTime inbound) {
    var person = discreteDemands.getPersons().getFactory().registerNew();
    return registerTourFor(person, tourStart, tourEnd, outbound, inbound);
  }

  private Tour registerTourFor(
      Person person, LocalTime tourStart, LocalTime tourEnd, LocalTime outbound, LocalTime inbound) {
    Tour tour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, tourStart, tourEnd, true);

    Trip outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
        tour, DirectionBound.OUTBOUND, true);
    outboundTrip.setStartTime(outbound);

    if (inbound != null) {
      Trip inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour, DirectionBound.INBOUND, true);
      inboundTrip.setStartTime(inbound);
    }
    return tour;
  }

  private void applySubBinJitter(Person person) {
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(person, BIN_SECONDS, WINDOW_START, WINDOW_END);
  }

  private static void assertWithin(LocalTime actual, LocalTime lowInclusive, LocalTime highExclusive, String what) {
    assertFalse(actual.isBefore(lowInclusive), what + " before " + lowInclusive + ": " + actual);
    assertTrue(actual.isBefore(highExclusive), what + " at or after " + highExclusive + ": " + actual);
  }

  /**
   * A tour wholly inside one bin holds three points, so it splits into three sub-bins and the tour gains a
   * positive duration where it had exactly zero
   */
  @Test
  void testSameBinTourGainsPositiveDurationAndDistinctTimes() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);
    var person = tour.getPrimaryParticipant();
    var outbound = (Trip) tour.getSchedule().get(0);
    var inbound = (Trip) tour.getSchedule().get(1);

    applySubBinJitter(person);

    assertWithin(outbound.getStartTime(), noon, noon.plusMinutes(10), "outbound trip");
    assertWithin(inbound.getStartTime(), noon.plusMinutes(10), noon.plusMinutes(20), "inbound trip");
    assertWithin(tour.getEndTime(), noon.plusMinutes(20), noon.plusMinutes(30), "tour end");

    assertTrue(outbound.getStartTime().isBefore(inbound.getStartTime()), "legs not strictly ordered");
    assertTrue(inbound.getStartTime().isBefore(tour.getEndTime()), "tour end not after inbound trip");
    assertTrue(tour.getStartTime().isBefore(tour.getEndTime()), "tour duration not positive");
  }

  /**
   * A tour starts precisely when its outbound trip departs, so its start is aliased rather than drawn
   */
  @Test
  void testTourStartIsAliasedToOutboundTrip() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);
    var outbound = (Trip) tour.getSchedule().get(0);

    applySubBinJitter(tour.getPrimaryParticipant());

    assertEquals(outbound.getStartTime(), tour.getStartTime(), "tour start not aliased to outbound trip");
  }

  /**
   * A tour end is the arrival back at the anchor, a point of its own in its own bin. It must not be dragged back
   * to the inbound trip's bin
   */
  @Test
  void testTourEndIsNotAliasedToInboundTrip() {
    var tourStart = LocalTime.of(8, 0);
    var tourEnd = LocalTime.of(18, 0);
    var inboundTime = LocalTime.of(17, 0);
    Tour tour = registerTour(tourStart, tourEnd, tourStart, inboundTime);
    var inbound = (Trip) tour.getSchedule().get(1);

    applySubBinJitter(tour.getPrimaryParticipant());

    assertWithin(tour.getEndTime(), tourEnd, tourEnd.plusMinutes(30), "tour end");
    assertWithin(inbound.getStartTime(), inboundTime, inboundTime.plusMinutes(30), "inbound trip");
    assertTrue(inbound.getStartTime().isBefore(tour.getEndTime()), "tour end not after inbound trip");
  }

  /**
   * A parent tour's end closes off everything it contains, including a sub-tour sharing its bin. All six points
   * must come out strictly increasing, with the parent end last
   */
  @Test
  void testNestedSubTourInSameBinDoesNotInvert() {
    var noon = LocalTime.of(12, 0);
    var person = discreteDemands.getPersons().getFactory().registerNew();

    Tour parentTour = discreteDemands.getTours().getFactory().registerNew(
        person, null, null, noon, noon, true);

    Trip parentOutbound = discreteDemands.getTrips().getFactory().registerNew(
        parentTour, DirectionBound.OUTBOUND, true);
    parentOutbound.setStartTime(noon);

    Tour subTour = discreteDemands.getTours().getFactory().registerNew(
        parentTour, null, null, noon, noon, true);
    Trip subOutbound = discreteDemands.getTrips().getFactory().registerNew(
        subTour, DirectionBound.OUTBOUND, true);
    subOutbound.setStartTime(noon);
    Trip subInbound = discreteDemands.getTrips().getFactory().registerNew(
        subTour, DirectionBound.INBOUND, true);
    subInbound.setStartTime(noon);

    Trip parentInbound = discreteDemands.getTrips().getFactory().registerNew(
        parentTour, DirectionBound.INBOUND, true);
    parentInbound.setStartTime(noon);

    applySubBinJitter(person);

    List<LocalTime> ordered = new ArrayList<>(List.of(
        parentOutbound.getStartTime(),
        subOutbound.getStartTime(),
        subInbound.getStartTime(),
        subTour.getEndTime(),
        parentInbound.getStartTime(),
        parentTour.getEndTime()));

    for (int index = 1; index < ordered.size(); ++index) {
      assertTrue(ordered.get(index - 1).isBefore(ordered.get(index)),
          "point " + index + " not strictly after its predecessor: " + ordered);
    }
    assertEquals(parentOutbound.getStartTime(), parentTour.getStartTime(), "parent start not aliased");
    assertEquals(subOutbound.getStartTime(), subTour.getStartTime(), "sub tour start not aliased");
  }

  /**
   * A point alone in its bin is still drawn across the whole width, which is what preserves the aggregate
   * spreading a rigid per person offset provides
   */
  @Test
  void testLonePointInBinStillMoves() {
    var morning = LocalTime.of(9, 0);
    int moved = 0;
    for (int index = 0; index < 50; ++index) {
      Tour tour = registerTour(morning, LocalTime.of(10, 0), morning, null);
      applySubBinJitter(tour.getPrimaryParticipant());
      if (!tour.getStartTime().equals(morning)) {
        ++moved;
      }
    }
    assertTrue(moved > 40, "lone points in a bin barely moved, only " + moved + " of 50");
  }

  /**
   * Points in different bins stay within their own bin, so their order is preserved without any extra work
   */
  @Test
  void testPointsInAdjacentBinsStayInTheirOwnBin() {
    var firstBin = LocalTime.of(8, 0);
    var secondBin = LocalTime.of(8, 30);
    Tour tour = registerTour(firstBin, LocalTime.of(9, 0), firstBin, secondBin);
    var outbound = (Trip) tour.getSchedule().get(0);
    var inbound = (Trip) tour.getSchedule().get(1);

    applySubBinJitter(tour.getPrimaryParticipant());

    assertWithin(outbound.getStartTime(), firstBin, secondBin, "outbound trip");
    assertWithin(inbound.getStartTime(), secondBin, LocalTime.of(9, 0), "inbound trip");
    assertTrue(outbound.getStartTime().isBefore(inbound.getStartTime()), "order not preserved across bins");
  }

  /**
   * Aggregated over many persons a lone point remains uniformly spread across its bin, i.e. the smoothing the
   * rigid per person offset already delivered is not regressed
   */
  @Test
  void testAggregateRemainsSpreadAcrossBin() {
    var noon = LocalTime.of(12, 0);
    final int personCount = 600;
    final int bucketCount = 6;
    int[] buckets = new int[bucketCount];

    for (int index = 0; index < personCount; ++index) {
      Tour tour = registerTour(noon, LocalTime.of(14, 0), noon, null);
      applySubBinJitter(tour.getPrimaryParticipant());

      long secondsIntoBin = Duration.between(noon, tour.getStartTime()).getSeconds();
      int bucket = (int) (secondsIntoBin * bucketCount / BIN_SECONDS);
      buckets[Math.min(Math.max(bucket, 0), bucketCount - 1)]++;
    }

    int expectedPerBucket = personCount / bucketCount;
    for (int index = 0; index < bucketCount; ++index) {
      assertTrue(buckets[index] > expectedPerBucket / 2,
          "bucket " + index + " underfilled with " + buckets[index] + ", expected around " + expectedPerBucket);
    }
  }

  /**
   * Same person, same seed, same outcome
   */
  @Test
  void testDeterministicReproducibility() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);
    var person = tour.getPrimaryParticipant();
    var outbound = (Trip) tour.getSchedule().get(0);
    var inbound = (Trip) tour.getSchedule().get(1);

    applySubBinJitter(person);
    var firstOutbound = outbound.getStartTime();
    var firstInbound = inbound.getStartTime();
    var firstEnd = tour.getEndTime();

    // restore and repeat
    tour.setStartEndTime(noon, noon);
    outbound.setStartTime(noon);
    inbound.setStartTime(noon);
    applySubBinJitter(person);

    assertEquals(firstOutbound, outbound.getStartTime(), "outbound trip not reproducible");
    assertEquals(firstInbound, inbound.getStartTime(), "inbound trip not reproducible");
    assertEquals(firstEnd, tour.getEndTime(), "tour end not reproducible");
  }

  /**
   * Close to the window end the spread shrinks to what remains, so no point crosses the window's end
   */
  @Test
  void testPointsNearWindowEndDoNotCrossIt() {
    var lateNight = LocalTime.of(2, 50);
    var windowEnd = LocalTime.of(3, 0);
    Tour tour = registerTour(LocalTime.of(2, 50), lateNight, lateNight, null);

    applySubBinJitter(tour.getPrimaryParticipant());

    assertWithin(tour.getStartTime(), lateNight, windowEnd, "tour start near window end");
    assertWithin(tour.getEndTime(), lateNight, windowEnd, "tour end near window end");
  }

  /**
   * A tour handed in as needing a minimum duration gets it by having its end time moved later, while the trips it
   * contains stay where the draws put them
   */
  @Test
  void testMinDurationIsAppliedToSuppliedTour() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);
    var inbound = (Trip) tour.getSchedule().get(1);

    // a third of a bin, so it always bites but stays well inside the bin
    final int minDurationSeconds = BIN_SECONDS / 3;
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
        tour.getPrimaryParticipant(), BIN_SECONDS, WINDOW_START, WINDOW_END, Set.of(tour), minDurationSeconds);

    assertTrue(Duration.between(tour.getStartTime(), tour.getEndTime()).getSeconds() >= minDurationSeconds,
        "minimum tour duration not applied");
    assertWithin(inbound.getStartTime(), noon, noon.plusSeconds(BIN_SECONDS), "inbound trip");
  }

  /**
   * The minimum duration is a preference, it may never take a tour end outside of its own bin
   */
  @Test
  void testMinDurationIsTruncatedAtTheBinEnd() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);

    // far longer than a bin, so truncation is the only possible outcome
    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
        tour.getPrimaryParticipant(), BIN_SECONDS, WINDOW_START, WINDOW_END, Set.of(tour), 4 * BIN_SECONDS);

    assertEquals(noon.plusSeconds(BIN_SECONDS), tour.getEndTime(), "tour end moved outside of its own bin");
  }

  /**
   * The minimum duration may not push a tour's end past whatever follows it, so it is met only as far as there is
   * room
   */
  @Test
  void testMinDurationDoesNotOvertakeTheNextTour() {
    var noon = LocalTime.of(12, 0);
    var person = discreteDemands.getPersons().getFactory().registerNew();
    Tour withinBinTour = registerTourFor(person, noon, noon, noon, noon);
    Tour nextTour = registerTourFor(
        person, LocalTime.of(12, 30), LocalTime.of(14, 0), LocalTime.of(12, 30), LocalTime.of(13, 30));

    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
        person, BIN_SECONDS, WINDOW_START, WINDOW_END, Set.of(withinBinTour), 7200);

    assertFalse(withinBinTour.getEndTime().isAfter(nextTour.getStartTime()),
        "minimum duration pushed the tour end past the tour that follows it");
    assertTrue(withinBinTour.getStartTime().isBefore(withinBinTour.getEndTime()),
        "tour still has no duration");
  }

  /**
   * A tour not handed in is left as the draws placed it, even when it starts and ends in the same bin
   */
  @Test
  void testTourNotSuppliedKeepsItsDrawnDuration() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);

    discreteDemands.getDiscreteDemandsModifier().adjustPersonScheduleSubBinJitter(
        tour.getPrimaryParticipant(), BIN_SECONDS, WINDOW_START, WINDOW_END, Set.of(), 2400);

    assertWithin(tour.getEndTime(), noon.plusMinutes(20), noon.plusMinutes(30), "tour end");
  }

  /**
   * Spreading backwards would take points out of their own bin, so a symmetric request is spread forward only
   */
  @Test
  void testSymmetricRequestStillSpreadsForwardOnly() {
    var noon = LocalTime.of(12, 0);
    Tour tour = registerTour(noon, noon, noon, noon);
    var outbound = (Trip) tour.getSchedule().get(0);

    discreteDemands.getDiscreteDemandsModifier().adjustPersonsScheduleSubBinJitter(BIN_SECONDS, WINDOW_START, WINDOW_END);

    assertFalse(outbound.getStartTime().isBefore(noon), "outbound trip moved backwards");
    assertFalse(tour.getEndTime().isBefore(noon), "tour end moved backwards");
  }
}
