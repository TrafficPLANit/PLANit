package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Used only by the schedule jitter, see {@link ScheduleJitterExecutor} and {@link SharedTourJitterUtils}: the steps of
 * a sub-bin draw over the points of one schedule, the same for a person's own points and for a shared tour's. The
 * points are drawn within their bins, then kept from running backwards, and then tours given a minimum duration get it
 * as far as there is room.
 *
 * @author markr
 */
final class SubBinDrawUtils {

  private SubBinDrawUtils() {
  }

  /**
   * Give each of the provided tours a minimum duration by moving its end time later, leaving the trips it contains
   * where they are. The end is never moved past the end of the draw range it was drawn in, nor past the point that
   * follows it, so the minimum is a preference that is met only as far as there is room for it. Points marked as
   * fixed are left alone, a shared tour being given its minimum duration in its own draw
   *
   * @param jitterPoints all points of the schedule, in order, as drawn
   * @param minTourDuration the minimum duration and the tours to give it
   * @param windowStartTime start of the allowed window as a time of day
   * @param windowDurationSeconds length of the allowed window
   */
  public static void enforceMinTourDuration(
      List<JitterPoint> jitterPoints, MinTourDuration minTourDuration, LocalTime windowStartTime,
      long windowDurationSeconds) {

    if (!minTourDuration.appliesToAnyTour()) {
      return;
    }
    for (int index = 0; index < jitterPoints.size(); ++index) {
      var point = jitterPoints.get(index);
      if (!point.isTourEndTime() || point.isFixed()) {
        continue;
      }
      /* the point belongs to a participation, the minimum duration is a property of the tour behind it */
      Long minDurationEndSeconds = minTourDuration.findEndSeconds(
          ((ParticipantTour) point.getElement()).getTour(), windowStartTime);
      if (minDurationEndSeconds == null) {
        continue;
      }

      long endElapsedSeconds = point.getElapsedSeconds(windowStartTime);

      long latestAllowedSeconds = index + 1 < jitterPoints.size()
          ? jitterPoints.get(index + 1).getElapsedSeconds(windowStartTime)
          : windowDurationSeconds;
      latestAllowedSeconds = Math.min(latestAllowedSeconds, point.getDrawRangeEndElapsedSeconds());

      long newEndElapsedSeconds = Math.min(minDurationEndSeconds, latestAllowedSeconds);
      if (newEndElapsedSeconds > endElapsedSeconds) {
        point.setElapsedSeconds(newEndElapsedSeconds, windowStartTime);
      }
    }
  }

  /**
   * Draw the provided points within their bins. Consecutive points carrying an identical time are the time points
   * in one bin. They are drawn within their draw range in that bin, which is split into as many sub-bins as there are
   * time points in the bin, each point being drawn within its own sub-bin in order. The draw range is the whole bin
   * for a person's own points, and the part of the bin its participants leave them for a shared tour's points.
   * Points marked as fixed keep their time, they were drawn as part of the tour they belong to, but they still
   * occupy their place in the order
   *
   * @param jitterPoints points to draw, in schedule order
   * @param spreadWidthSeconds width to spread the time points in a bin over
   * @param windowStartTime start of the allowed window as a time of day
   * @param windowDurationSeconds length of the allowed window
   * @param rng to draw with
   * @param sharedTourDrawRanges gives the draw ranges when the points are a shared tour's, null when they are a
   *                             person's own
   */
  public static void drawSubBins(
      List<JitterPoint> jitterPoints, int spreadWidthSeconds, LocalTime windowStartTime,
      long windowDurationSeconds, SplittableRandom rng, SharedTourDrawRanges sharedTourDrawRanges) {

    int firstIndexInBin = 0;
    while (firstIndexInBin < jitterPoints.size()) {

      // the time points in a bin are a maximal stretch of points carrying the same time
      final var binTime = jitterPoints.get(firstIndexInBin).getTime();
      int lastIndexInBin = firstIndexInBin;
      while (lastIndexInBin + 1 < jitterPoints.size()
          && binTime.equals(jitterPoints.get(lastIndexInBin + 1).getTime())) {
        ++lastIndexInBin;
      }
      final int timePointsInBinCount = lastIndexInBin - firstIndexInBin + 1;

      // near the window end there may be less room than a full width, in which case spread over what remains
      // rather than clamping the draws, which would pile them onto the boundary
      long binElapsedSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, binTime);
      long remainingSeconds = windowDurationSeconds - binElapsedSeconds;
      double availableSeconds = Math.min(spreadWidthSeconds, Math.max(remainingSeconds, 0));

      long binEndElapsedSeconds = binElapsedSeconds + (long) availableSeconds;
      long drawRangeEndElapsedSeconds = binEndElapsedSeconds;
      if (availableSeconds > 0) {
        var drawRange = sharedTourDrawRanges == null
            ? Pair.of((double) binElapsedSeconds, (double) binEndElapsedSeconds)
            : sharedTourDrawRanges.findInBin(
                jitterPoints.subList(firstIndexInBin, lastIndexInBin + 1), binElapsedSeconds, binEndElapsedSeconds);
        drawRangeEndElapsedSeconds = (long) drawRange.second().doubleValue();
        final double drawRangeOffsetSeconds = drawRange.first() - binElapsedSeconds;
        final double subBinSeconds = (drawRange.second() - drawRange.first()) / timePointsInBinCount;
        for (int index = 0; index < timePointsInBinCount; ++index) {
          // draw regardless, so the sequence of draws does not depend on which points happen to be fixed
          long offsetSeconds = Math.round(drawRangeOffsetSeconds + (index + rng.nextDouble()) * subBinSeconds);
          var point = jitterPoints.get(firstIndexInBin + index);
          if (!point.isFixed()) {
            point.setTime(binTime.plusSeconds(offsetSeconds));
          }
        }
      }

      // remember where the draw range ends, nothing may be moved beyond it afterwards
      for (int index = 0; index < timePointsInBinCount; ++index) {
        jitterPoints.get(firstIndexInBin + index).setDrawRangeEndElapsedSeconds(drawRangeEndElapsedSeconds);
      }

      firstIndexInBin = lastIndexInBin + 1;
    }
  }

  /**
   * Ensure the points do not run backwards in time. Draws within a single bin cannot invert, but a point belonging
   * to a shared tour was drawn elsewhere and no longer carries its bin's time, so the points around it can land on
   * the wrong side of it. Since such a point is fixed, it is the free points that give way, in both directions: a
   * free point following it is moved up to it, a free point preceding it is moved back to it. Where the bin leaves
   * no room the two end up equal, which is in order rather than inverted
   *
   * @param jitterPoints points in schedule order
   * @param windowStartTime start of the allowed window as a time of day
   */
  public static void enforceNonDecreasingOrder(List<JitterPoint> jitterPoints, LocalTime windowStartTime) {

    /* a fixed point cannot be moved, so a free point placed before it must instead not exceed it. Walk back from
     * the end carrying the earliest fixed time seen as an upper bound, before the forward pass below fills any
     * remaining gaps */
    long upperBoundElapsedSeconds = Long.MAX_VALUE;
    for (int index = jitterPoints.size() - 1; index >= 0; --index) {
      var point = jitterPoints.get(index);
      long elapsedSeconds = point.getElapsedSeconds(windowStartTime);
      if (point.isFixed()) {
        upperBoundElapsedSeconds = Math.min(upperBoundElapsedSeconds, elapsedSeconds);
      } else if (elapsedSeconds > upperBoundElapsedSeconds) {
        point.setElapsedSeconds(upperBoundElapsedSeconds, windowStartTime);
      }
    }

    long previousElapsedSeconds = -1;
    for (var point : jitterPoints) {
      long elapsedSeconds = point.getElapsedSeconds(windowStartTime);
      if (elapsedSeconds < previousElapsedSeconds) {
        if (point.isFixed()) {
          /* authoritative, it was drawn as part of its own tour, leave it and continue from here */
          previousElapsedSeconds = elapsedSeconds;
          continue;
        }
        point.setElapsedSeconds(previousElapsedSeconds, windowStartTime);
        elapsedSeconds = previousElapsedSeconds;
      }
      previousElapsedSeconds = elapsedSeconds;
    }
  }

  /**
   * Verify the drawn points do not run backwards in time. The clamps resolve every conflict a person's own draws can
   * create, and each shared tour is drawn after the shared tours before it on its participants' schedules, in the
   * room they leave it. A schedule failing here therefore holds shared tours that already overlap in the input, which
   * no draw within a bin can put in order
   *
   * @param jitterPoints points in schedule order
   * @param windowStartTime start of the allowed window as a time of day
   * @return true when the points are in order, false when they are not
   */
  public static boolean isNonDecreasing(List<JitterPoint> jitterPoints, LocalTime windowStartTime) {
    long previousElapsedSeconds = -1;
    for (var point : jitterPoints) {
      long elapsedSeconds = point.getElapsedSeconds(windowStartTime);
      if (elapsedSeconds < previousElapsedSeconds) {
        return false;
      }
      previousElapsedSeconds = elapsedSeconds;
    }
    return true;
  }
}
