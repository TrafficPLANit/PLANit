package org.goplanit.network.layer.macroscopic.intersection;

import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUnsignalisedSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * A link segment of an intersection without a signal: the link segment and, when set, its saturation flow factor with
 * how it got its value. Created and held by its intersection
 *
 * @author markr
 */
public class IntersectionUnsignalisedSegmentImpl extends IntersectionSegmentImpl
    implements IntersectionUnsignalisedSegment {

  /** generated UID */
  private static final long serialVersionUID = 7709256114925614307L;

  /**
   * Constructor, without values
   *
   * @param linkSegment the link segment
   */
  protected IntersectionUnsignalisedSegmentImpl(LinkSegment linkSegment) {
    super(linkSegment);
  }

  /**
   * Copy constructor, for the same link segment and with the same saturation flow factor; a green ratio of the other is
   * not kept
   *
   * @param other to copy, with a signal or not
   * @param deepCopy when true, create a deep copy, shallow copy otherwise, see
   *                 {@link IntersectionSegmentImpl#IntersectionSegmentImpl(IntersectionSegmentImpl, boolean)}
   */
  protected IntersectionUnsignalisedSegmentImpl(IntersectionSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected IntersectionSegmentImpl deepCloneAsSignalOrUnsignalised(boolean hasSignal) {
    return hasSignal ? new IntersectionSignalSegmentImpl(this, true) : deepClone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionUnsignalisedSegmentImpl shallowClone() {
    return new IntersectionUnsignalisedSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionUnsignalisedSegmentImpl deepClone() {
    return new IntersectionUnsignalisedSegmentImpl(this, true);
  }
}
