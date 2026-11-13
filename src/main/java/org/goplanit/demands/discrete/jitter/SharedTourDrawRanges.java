package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.misc.Pair;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Used only by the shared tour draw, see {@link SharedTourJitterUtils}: finds, bin by bin, the draw range of a shared
 * tour's time points. That is the part of the bin all its participants leave them, found from what each participant
 * has in the bin around those time points, see {@link ParticipantSlotBoundsInSharedTour}.
 *
 * @author markr
 */
final class SharedTourDrawRanges {

  /** the shared tour being drawn */
  private final Tour tour;

  /** the points of each participant, in their schedule order. They are read live from the schedules, so they hold
   * the final times of the shared tours drawn before this one */
  private final List<List<JitterPoint>> participantsPoints;

  /** start of the allowed window as a time of day */
  private final LocalTime windowStartTime;

  /** the minimum duration and the tours given it */
  private final MinTourDuration minTourDuration;

  /** to record the tours whose end moved and the tours kept in order at one time */
  private final SharedTourRoomAdjustments roomAdjustments;

  /**
   * Constructor, collecting the points of each participant of the tour
   *
   * @param tour the shared tour being drawn
   * @param windowStartTime start of the allowed window as a time of day
   * @param minTourDuration the minimum duration and the tours given it
   * @param roomAdjustments to record the tours whose end moved and the tours kept in order at one time
   */
  public SharedTourDrawRanges(
      Tour tour, LocalTime windowStartTime, MinTourDuration minTourDuration,
      SharedTourRoomAdjustments roomAdjustments) {
    this.tour = tour;
    this.windowStartTime = windowStartTime;
    this.minTourDuration = minTourDuration;
    this.roomAdjustments = roomAdjustments;
    this.participantsPoints = new ArrayList<>(tour.getParticipantTours().size());
    for (var participation : tour.getParticipantTours()) {
      var schedule = participation.getPerson() != null ? participation.getPerson().getSchedule() : null;
      if (schedule != null) {
        var points = new ArrayList<JitterPoint>(schedule.sizeUnrolled(false) + 1);
        JitterPoint.collect(schedule, points, false);
        participantsPoints.add(points);
      }
    }
  }

  /**
   * Find the draw range of the tour's time points in a bin, from the slot each participant leaves them, see
   * {@link ParticipantSlotBoundsInSharedTour#find(List, List, long, long, LocalTime, MinTourDuration)}. The draw
   * range runs from the latest of the participants' starts to the earliest of their ends.
   * <p>
   * When the participants' needs conflict there is no such range. The participant starting latest then moves the end
   * of their earlier shared tour at their anchor earlier, see
   * {@link ParticipantSlotBoundsInSharedTour#moveAnchorToItsShareBefore(double, int)}, and so on while the range
   * stays empty. When an anchor cannot move early enough, no end is moved and the order is kept at the cost of room:
   * the draw range starts and ends at the latest anchor, so the tour's time points in the bin all take that time,
   * never before anything a participant already has before them
   * </p>
   *
   * @param timePointsInBin the tour's time points in the bin, in order
   * @param binStartSeconds start of the bin as seconds from the window start
   * @param binEndSeconds end of the bin as seconds from the window start
   * @return start and end of the draw range, as seconds from the window start
   */
  public Pair<Double, Double> findInBin(List<JitterPoint> timePointsInBin, long binStartSeconds, long binEndSeconds) {
    var slots = new ArrayList<ParticipantSlotBoundsInSharedTour>(participantsPoints.size());
    for (var points : participantsPoints) {
      var slot = ParticipantSlotBoundsInSharedTour.find(
          timePointsInBin, points, binStartSeconds, binEndSeconds, windowStartTime, minTourDuration);
      if (slot != null) {
        slots.add(slot);
      }
    }
    Comparator<ParticipantSlotBoundsInSharedTour> byStart =
        Comparator.comparingDouble(ParticipantSlotBoundsInSharedTour::getStartSeconds);
    double drawRangeEndSeconds =
        slots.stream().mapToDouble(ParticipantSlotBoundsInSharedTour::getEndSeconds).min().orElse(binEndSeconds);
    long latestAnchorSeconds =
        slots.stream().mapToLong(ParticipantSlotBoundsInSharedTour::getAnchorSeconds).max().orElse(binStartSeconds);

    var latestStartSlot = slots.stream().max(byStart).orElse(null);
    while (latestStartSlot != null && latestStartSlot.getStartSeconds() > drawRangeEndSeconds) {
      if (latestStartSlot.isAnchorMovePlanned() || !latestStartSlot.canMoveAnchorBefore(drawRangeEndSeconds)) {
        roomAdjustments.addOrderKeptTour(tour);
        return Pair.of((double) latestAnchorSeconds, (double) latestAnchorSeconds);
      }
      latestStartSlot.moveAnchorToItsShareBefore(drawRangeEndSeconds, timePointsInBin.size());
      latestStartSlot = slots.stream().max(byStart).orElse(null);
    }

    for (var slot : slots) {
      var movedEndTour = slot.applyMovedAnchor(windowStartTime);
      if (movedEndTour != null) {
        roomAdjustments.addMovedEndTour(movedEndTour);
      }
    }
    double drawRangeStartSeconds = latestStartSlot != null ? latestStartSlot.getStartSeconds() : binStartSeconds;
    return Pair.of(drawRangeStartSeconds, drawRangeEndSeconds);
  }
}
