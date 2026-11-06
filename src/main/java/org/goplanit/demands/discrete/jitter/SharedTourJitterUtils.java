package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.person.PersonUtils;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.logging.Logger;

/**
 * Used only by the schedule jitter, see {@link ScheduleJitterExecutor}: draws the tours shared by several
 * participants, each once for all its participants, in the order they take place and before any person is drawn.
 *
 * @author markr
 */
final class SharedTourJitterUtils {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SharedTourJitterUtils.class.getCanonicalName());

  private SharedTourJitterUtils() {
  }

  /**
   * The shared tours among the provided ones, in the order they take place, so each comes after every shared tour
   * that is before it on the schedule of one of its participants. The order matters because a shared tour is drawn
   * after the final times of the shared tours before it on its participants' schedules, see
   * {@link ParticipantSlotBoundsInSharedTour}. So those must be drawn first. Of the tours whose earlier shared tours
   * have all been placed, the one starting first comes next, then the one ending first, then the one with the lowest
   * id, all times as seconds from the window start. Tours equal on start and end therefore follow the schedule of a
   * person taking part in both, and the order is the same every time. Where schedules contradict each other, which only
   * input with overlapping shared tours does, the tours left over follow by start, end and id
   *
   * @param tours to order the shared ones of
   * @param persons whose schedules give the order of the shared tours they take part in
   * @param windowStartTime start of the allowed window as a time of day
   * @return the shared tours in the order they take place
   */
  public static List<Tour> collectSharedToursInOrder(
      Iterable<Tour> tours, Iterable<Person> persons, LocalTime windowStartTime) {

    /* per shared tour, the number of shared tours right before it on a participant's schedule not yet placed */
    var earlierCounts = new HashMap<Tour, Integer>();
    tours.forEach(tour -> {
      if (tour != null && tour.hasMultipleParticipants() && tour.hasSchedule()) {
        earlierCounts.put(tour, 0);
      }
    });
    var laterTours = new HashMap<Tour, List<Tour>>();
    for (var person : persons) {
      if (person.getSchedule() == null) {
        continue;
      }
      Tour previousSharedTour = null;
      for (var sharedTour : person.getSchedule().collectSharedTours()) {
        if (!earlierCounts.containsKey(sharedTour)) {
          continue;
        }
        if (previousSharedTour != null) {
          laterTours.computeIfAbsent(previousSharedTour, tour -> new ArrayList<>()).add(sharedTour);
          earlierCounts.merge(sharedTour, 1, Integer::sum);
        }
        previousSharedTour = sharedTour;
      }
    }

    Comparator<LocalTime> byElapsedSeconds = Comparator.nullsLast(Comparator.comparingLong(
        time -> LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, time)));
    Comparator<Tour> byStartEndAndId = Comparator.comparing(Tour::getStartTime, byElapsedSeconds)
        .thenComparing(Tour::getEndTime, byElapsedSeconds).thenComparing(IdAbleUtils.byId());

    var placeable = new PriorityQueue<Tour>(byStartEndAndId);
    earlierCounts.forEach((tour, count) -> {
      if (count == 0) {
        placeable.add(tour);
      }
    });
    var ordered = new ArrayList<Tour>(earlierCounts.size());
    while (!placeable.isEmpty()) {
      var tour = placeable.poll();
      ordered.add(tour);
      for (var laterTour : laterTours.getOrDefault(tour, List.of())) {
        if (earlierCounts.merge(laterTour, -1, Integer::sum) == 0) {
          placeable.add(laterTour);
        }
      }
    }
    if (ordered.size() < earlierCounts.size()) {
      var placed = new HashSet<>(ordered);
      earlierCounts.keySet().stream().filter(tour -> !placed.contains(tour)).sorted(byStartEndAndId)
          .forEach(ordered::add);
    }
    return ordered;
  }

  /**
   * Draw the times within a tour that is shared by more than one participant. Everything inside such a tour, its
   * trips as much as its end, is common to all participants and is therefore drawn once here rather than once per
   * participant, seeded by the primary participant so the result stays reproducible. Its time points in each bin are
   * drawn in the part of the bin its participants leave them, see
   * {@link SharedTourDrawRanges#findInBin(List, long, long)}.
   * A tour given a minimum duration gets it here, within the draw range of its last time points, so before any later
   * shared tour of its participants is placed after it
   *
   * @param tour to draw, ignored when it has a single participant
   * @param spreadWidthSeconds width to spread the time points in a bin over
   * @param minAllowedTimeSeconds the lower boundary in seconds
   * @param maxAllowedTimeSeconds the upper boundary in seconds
   * @param minDurationTours tours to give a minimum duration, may be null
   * @param minDurationSeconds minimum duration to give the tours provided
   * @param roomAdjustments to record the tours whose end moved and the tours kept in order at one time
   */
  public static void applySharedTourSubBinJitter(
      Tour tour, int spreadWidthSeconds, int minAllowedTimeSeconds, int maxAllowedTimeSeconds,
      Set<Tour> minDurationTours, int minDurationSeconds, SharedTourRoomAdjustments roomAdjustments) {

    if (spreadWidthSeconds <= 0 || tour == null || !tour.hasMultipleParticipants() || !tour.hasSchedule()) {
      return;
    }
    var primaryParticipant = tour.getPrimaryParticipant();
    if (primaryParticipant == null) {
      LOGGER.severe(String.format(
          "Tour (%s) shared by multiple participants has no primary participant, unable to apply jitter",
          tour.getIdsAsString()));
      return;
    }

    /* the tour's own points, which are not fixed here, this being the place they are drawn. Its end is reached
     * through the primary participation, though any participation gives the same end */
    var jitterPoints = new ArrayList<JitterPoint>(tour.getSchedule().sizeUnrolled(false) + 1);
    JitterPoint.collect(tour.getSchedule(), jitterPoints, false);
    var primaryParticipation = tour.getPrimaryParticipation();
    if (primaryParticipation != null && primaryParticipation.getEndTime() != null) {
      jitterPoints.add(new JitterPoint(primaryParticipation, true, false));
    }
    if (jitterPoints.isEmpty()) {
      return;
    }

    final var windowStartTime = LocalTimeUtils.ofSecondOfDayWrapped(minAllowedTimeSeconds);
    final long windowDurationSeconds = maxAllowedTimeSeconds - minAllowedTimeSeconds;
    var rng = new SplittableRandom(PersonUtils.generatePersonSeed(primaryParticipant));

    SubBinDrawUtils.drawSubBins(jitterPoints, spreadWidthSeconds, windowStartTime, windowDurationSeconds, rng,
        new SharedTourDrawRanges(tour, windowStartTime, minDurationTours, minDurationSeconds, roomAdjustments));
    SubBinDrawUtils.enforceNonDecreasingOrder(jitterPoints, minAllowedTimeSeconds, windowStartTime);

    // the tour's start follows the first trip that has now been placed
    tour.getSchedule().syncTourStartTimesToFirstElement();
    var firstElementStartTime = tour.getSchedule().getFirst() != null
        ? tour.getSchedule().getFirst().getStartTime() : null;
    if (firstElementStartTime != null) {
      tour.setStartTime(firstElementStartTime);
    }

    SubBinDrawUtils.enforceMinTourDuration(jitterPoints, minDurationTours, minDurationSeconds, minAllowedTimeSeconds,
        windowStartTime, windowDurationSeconds);
  }
}
