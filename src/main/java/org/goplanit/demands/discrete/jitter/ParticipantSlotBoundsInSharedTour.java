package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.ParticipantTour;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.time.LocalTimeUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Used only by the shared tour draw, see {@link SharedTourDrawRanges}: the bounds of the slot one participant of a
 * shared tour leaves the tour's time points in a bin, that is where they may start and must end. They follow from
 * what the participant has in the bin around those time points.
 * <p>
 * The participant's anchor is the latest time in the bin before the tour's time points that is already drawn,
 * usually the end of an earlier shared tour. Everything else of theirs in the bin still carries the bin's start, and
 * is drawn later. So the stretch from the anchor to the bin's end is shared out evenly, one part per point. For
 * example, in the bin from 12:00 to 12:15 a participant has the end of an earlier shared tour drawn at 12:04, then a
 * trip of their own, then the tour's two time points, then another trip of their own. The 11 minutes from 12:04 are
 * split into four parts of 2:45. The tour's time points get the second and third part. So for this participant they
 * may start at 12:06:45 and must end by 12:12:15.
 * </p>
 * <p>
 * The tour's draw range is where the slots of all its participants overlap. When they do not overlap, the anchor is
 * what gives way, as it is the only time already drawn. An anchor that is a shared tour's end may move earlier, down
 * to its lower limit: its bin's start, its tour's last trip, and its tour's start plus its minimum duration. An
 * earlier end puts nothing out of order, and gives that tour's other participants more room after it. A move is
 * planned first and made only once the draw range is open. So when the range cannot be opened, no end moves at all.
 * </p>
 *
 * @author markr
 */
final class ParticipantSlotBoundsInSharedTour {

  /**
   * Find the earliest time the anchor at the given point may move to: its bin's start, the last trip of the tour it
   * ends, and that tour's start plus its minimum duration when it is given one
   *
   * @param points the participant's points, in their schedule order
   * @param anchorIndex index of the anchor's point
   * @param binStartSeconds start of the bin as seconds from the window start
   * @param windowStartTime start of the allowed window as a time of day
   * @param minDurationTours tours given a minimum duration, may be null
   * @param minDurationSeconds minimum duration given to them
   * @return the earliest time as seconds from the window start
   */
  private static long findAnchorLowerLimit(
      List<JitterPoint> points, int anchorIndex, long binStartSeconds, LocalTime windowStartTime,
      Set<Tour> minDurationTours, int minDurationSeconds) {
    long lowerLimitSeconds = binStartSeconds;
    /* when the anchor is a tour's end, the point before it is that tour's last trip */
    if (anchorIndex > 0) {
      lowerLimitSeconds = Math.max(lowerLimitSeconds,
          LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, points.get(anchorIndex - 1).getTime()));
    }
    var anchorPoint = points.get(anchorIndex);
    if (anchorPoint.isTourEndTime()) {
      Long minDurationEndSeconds = SubBinDrawUtils.findMinDurationEndSeconds(
          ((ParticipantTour) anchorPoint.getElement()).getTour(), minDurationTours, minDurationSeconds,
          windowStartTime);
      if (minDurationEndSeconds != null) {
        lowerLimitSeconds = Math.max(lowerLimitSeconds, minDurationEndSeconds);
      }
    }
    return lowerLimitSeconds;
  }

  /** latest time already drawn before the tour's time points within the bin, as seconds from the window start */
  private long anchorSeconds;

  /** the point drawn at the anchor, null when the anchor is the bin's start */
  private JitterPoint anchorPoint;

  /** earliest time the anchor may move to, as seconds from the window start */
  private long anchorLowerLimitSeconds;

  /** number of points the participant has between the anchor and the tour's time points */
  private int pointsBefore;

  /** where the tour's time points may start for this participant, as seconds from the window start */
  private double startSeconds;

  /** where the tour's time points must end for this participant, as seconds from the window start */
  private double endSeconds;

  /** time the anchor is to move to so the tour's time points fit, null when no move is planned. The move is made
   * only once the draw range is open, see {@link #applyMovedAnchor(LocalTime)} */
  private Long movedAnchorSeconds;

  /**
   * Constructor, slot bounds are found with {@link #find(List, List, long, long, LocalTime, Set, int)}
   */
  private ParticipantSlotBoundsInSharedTour() {
  }

  /**
   * Find the slot a participant leaves a shared tour's time points in a bin. Their anchor is the latest time already
   * drawn before those time points on their schedule within the bin, the bin's start when there is none. The
   * stretch from the anchor to the bin's end is divided into equal parts, one for each point they have there: the
   * points between the anchor and the tour's time points, the tour's time points themselves and the points after
   * them. The tour's time points may start at the end of the parts before them and must end at the end of their own
   * parts
   *
   * @param timePointsInBin the shared tour's time points in the bin, in order
   * @param points the participant's points, in their schedule order
   * @param binStartSeconds start of the bin as seconds from the window start
   * @param binEndSeconds end of the bin as seconds from the window start
   * @param windowStartTime start of the allowed window as a time of day
   * @param minDurationTours tours given a minimum duration, may be null
   * @param minDurationSeconds minimum duration given to them
   * @return the slot bounds, null when the tour's time points are not on the participant's schedule
   */
  public static ParticipantSlotBoundsInSharedTour find(
      List<JitterPoint> timePointsInBin, List<JitterPoint> points, long binStartSeconds, long binEndSeconds,
      LocalTime windowStartTime, Set<Tour> minDurationTours, int minDurationSeconds) {
    int firstInBinIndex = IntStream.range(0, points.size())
        .filter(index -> points.get(index).isSameTimeAs(timePointsInBin.get(0))).findFirst().orElse(-1);
    if (firstInBinIndex < 0) {
      return null;
    }

    /* a point not drawn yet still carries its bin's start, so a later time within the bin is drawn */
    var slot = new ParticipantSlotBoundsInSharedTour();
    slot.anchorSeconds = binStartSeconds;
    slot.anchorLowerLimitSeconds = binStartSeconds;
    int pointsAfter = 0;
    for (int index = 0; index < points.size(); ++index) {
      if (index >= firstInBinIndex && index < firstInBinIndex + timePointsInBin.size()) {
        continue;
      }
      long seconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, points.get(index).getTime());
      if (seconds < binStartSeconds || seconds >= binEndSeconds) {
        continue;
      }
      if (index > firstInBinIndex) {
        ++pointsAfter;
      } else if (seconds > binStartSeconds) {
        if (seconds >= slot.anchorSeconds) {
          slot.anchorSeconds = seconds;
          slot.anchorPoint = points.get(index);
          slot.anchorLowerLimitSeconds = findAnchorLowerLimit(
              points, index, binStartSeconds, windowStartTime, minDurationTours, minDurationSeconds);
        }
        slot.pointsBefore = 0;
      } else {
        ++slot.pointsBefore;
      }
    }

    double partSeconds = (binEndSeconds - slot.anchorSeconds)
        / (double) (slot.pointsBefore + timePointsInBin.size() + pointsAfter);
    slot.startSeconds = slot.anchorSeconds + slot.pointsBefore * partSeconds;
    slot.endSeconds = slot.anchorSeconds + (slot.pointsBefore + timePointsInBin.size()) * partSeconds;
    return slot;
  }

  /**
   * Latest time already drawn before the tour's time points within the bin
   *
   * @return the anchor as seconds from the window start
   */
  public long getAnchorSeconds() {
    return anchorSeconds;
  }

  /**
   * Where the tour's time points may start for this participant
   *
   * @return the start as seconds from the window start
   */
  public double getStartSeconds() {
    return startSeconds;
  }

  /**
   * Where the tour's time points must end for this participant
   *
   * @return the end as seconds from the window start
   */
  public double getEndSeconds() {
    return endSeconds;
  }

  /**
   * Whether a move of the anchor is planned already
   *
   * @return true when it is, false otherwise
   */
  public boolean isAnchorMovePlanned() {
    return movedAnchorSeconds != null;
  }

  /**
   * Whether the anchor is the end of an earlier shared tour that may move early enough for the tour's time points to
   * start before the given end of their draw range
   *
   * @param drawRangeEndSeconds end of the draw range as seconds from the window start
   * @return true when it can, false otherwise
   */
  public boolean canMoveAnchorBefore(double drawRangeEndSeconds) {
    return anchorPoint != null && anchorPoint.isTourEndTime() && anchorLowerLimitSeconds < drawRangeEndSeconds;
  }

  /**
   * Plan a move of the anchor earlier, to make room for the tour's time points. The stretch from the anchor's lower
   * limit to the end of the draw range is split evenly over the anchor, the points before the tour's time points and
   * those time points, and the anchor is to move to the end of its own part, staying where it is when that is not
   * earlier. The tour's time points then start after the points before them
   *
   * @param drawRangeEndSeconds end of the draw range as seconds from the window start
   * @param timePointsInBinCount number of the tour's time points in the bin
   */
  public void moveAnchorToItsShareBefore(double drawRangeEndSeconds, int timePointsInBinCount) {
    double partSeconds = (drawRangeEndSeconds - anchorLowerLimitSeconds) / (1 + pointsBefore + timePointsInBinCount);
    movedAnchorSeconds = Math.min(anchorSeconds, Math.round(anchorLowerLimitSeconds + partSeconds));
    startSeconds = movedAnchorSeconds + pointsBefore * (drawRangeEndSeconds - movedAnchorSeconds)
        / (double) (pointsBefore + timePointsInBinCount);
  }

  /**
   * Give the end of the earlier shared tour at the anchor the time the anchor is to move to, when that is earlier
   * than it is now
   *
   * @param windowStartTime start of the allowed window as a time of day
   * @return the tour whose end moved, null when it did not move
   */
  public Tour applyMovedAnchor(LocalTime windowStartTime) {
    if (movedAnchorSeconds == null) {
      return null;
    }
    long currentSeconds = LocalTimeUtils.secondsFromWrapAroundDayAnchor(windowStartTime, anchorPoint.getTime());
    if (movedAnchorSeconds >= currentSeconds) {
      return null;
    }
    anchorPoint.setTime(LocalTimeUtils.ofSecondOfDayWrapped(windowStartTime.toSecondOfDay() + movedAnchorSeconds));
    return ((ParticipantTour) anchorPoint.getElement()).getTour();
  }
}
