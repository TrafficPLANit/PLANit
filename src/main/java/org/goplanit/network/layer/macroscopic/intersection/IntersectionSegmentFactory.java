package org.goplanit.network.layer.macroscopic.intersection;

import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Creates the intersection segments an intersection holds as its approach and internal segment objects: an
 * {@link IntersectionSignalSegmentImpl} for one with a signal, an {@link IntersectionUnsignalisedSegmentImpl} for one
 * without
 *
 * @author markr
 */
public class IntersectionSegmentFactory {

  /** static factory, not to be instantiated */
  private IntersectionSegmentFactory() {
  }

  /**
   * Create an intersection segment for a link segment, without values
   *
   * @param linkSegment the link segment
   * @param hasSignal true for a signal segment, false for an unsignalised one
   * @return the intersection segment
   */
  public static IntersectionSegmentImpl create(LinkSegment linkSegment, boolean hasSignal) {
    return hasSignal ?
        new IntersectionSignalSegmentImpl(linkSegment) : new IntersectionUnsignalisedSegmentImpl(linkSegment);
  }
}
