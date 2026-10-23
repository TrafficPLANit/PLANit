package org.goplanit.network.layer.macroscopic.intersection;

import java.util.List;

import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Utilities for a list of intersection segments, the approaches or the internal segments an intersection holds. They
 * match a link segment by identity, the very link segment rather than one equal to it, since recreating ids changes
 * ids in place
 *
 * @author markr
 */
public class IntersectionSegmentUtils {

  /** static utilities, not to be instantiated */
  private IntersectionSegmentUtils() {
  }

  /**
   * Find the intersection segment of a link segment in a list
   *
   * @param <T> type of intersection segment
   * @param intersectionSegments to look in
   * @param linkSegment to find it for
   * @return the intersection segment, null when none has the link segment
   */
  public static <T extends IntersectionSegmentImpl> T findByLinkSegment(
      List<T> intersectionSegments, LinkSegment linkSegment) {
    for (var intersectionSegment : intersectionSegments) {
      if (intersectionSegment.getLinkSegment() == linkSegment) {
        return intersectionSegment;
      }
    }
    return null;
  }

  /**
   * Remove the intersection segment of a link segment from a list
   *
   * @param <T> type of intersection segment
   * @param intersectionSegments to remove it from
   * @param linkSegment to remove it for
   * @return true when removed, false when none has the link segment
   */
  public static <T extends IntersectionSegmentImpl> boolean removeByLinkSegment(
      List<T> intersectionSegments, LinkSegment linkSegment) {
    return intersectionSegments.removeIf(intersectionSegment -> intersectionSegment.getLinkSegment() == linkSegment);
  }

  /**
   * Swap the intersection segment of a link segment in a list for a signal segment or an unsignalised one. When it is
   * of that kind already it stays; otherwise a deep clone of the other kind takes its place, see
   * {@link IntersectionSegmentImpl#deepCloneAsSignalOrUnsignalised(boolean)}
   *
   * @param intersectionSegments to look in
   * @param linkSegment to swap it for
   * @param hasSignal true for a signal segment, false for an unsignalised one
   * @return the intersection segment after the swap, null when no intersection segment in the list has the link
   * segment
   */
  public static IntersectionSegmentImpl swapToSignalOrUnsignalised(
      List<IntersectionSegmentImpl> intersectionSegments, LinkSegment linkSegment, boolean hasSignal) {
    for (int index = 0; index < intersectionSegments.size(); ++index) {
      var intersectionSegment = intersectionSegments.get(index);
      if (intersectionSegment.getLinkSegment() == linkSegment) {
        if (intersectionSegment.hasSignal() != hasSignal) {
          intersectionSegment = intersectionSegment.deepCloneAsSignalOrUnsignalised(hasSignal);
          intersectionSegments.set(index, intersectionSegment);
        }
        return intersectionSegment;
      }
    }
    return null;
  }
}
