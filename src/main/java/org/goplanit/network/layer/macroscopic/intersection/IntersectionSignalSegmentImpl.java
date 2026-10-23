package org.goplanit.network.layer.macroscopic.intersection;

import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * A link segment of an intersection with a signal: the link segment and, when set, its saturation flow factor and green
 * ratio, each with how it got its value. Created and held by its intersection
 *
 * @author markr
 */
public class IntersectionSignalSegmentImpl extends IntersectionSegmentImpl implements IntersectionSignalSegment {

  /** generated UID */
  private static final long serialVersionUID = -4378096154231779952L;

  /** the green ratio, null when not set */
  private Double greenRatio;

  /** how the green ratio got its value, null when not set */
  private CapacityFactorOrigin greenRatioOrigin;

  /**
   * Constructor, without values
   *
   * @param linkSegment the link segment
   */
  protected IntersectionSignalSegmentImpl(LinkSegment linkSegment) {
    super(linkSegment);
  }

  /**
   * Copy constructor, for the same link segment and with the same saturation flow factor, and the same green ratio when
   * the other has a signal too
   *
   * @param other to copy, with a signal or not
   * @param deepCopy when true, create a deep copy, shallow copy otherwise, see
   *                 {@link IntersectionSegmentImpl#IntersectionSegmentImpl(IntersectionSegmentImpl, boolean)}
   */
  protected IntersectionSignalSegmentImpl(IntersectionSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
    if (other instanceof IntersectionSignalSegmentImpl) {
      this.greenRatio = ((IntersectionSignalSegmentImpl) other).greenRatio;
      this.greenRatioOrigin = ((IntersectionSignalSegmentImpl) other).greenRatioOrigin;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected IntersectionSegmentImpl deepCloneAsSignalOrUnsignalised(boolean hasSignal) {
    return hasSignal ? deepClone() : new IntersectionUnsignalisedSegmentImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getGreenRatio() {
    return greenRatio == null ? 1 : greenRatio;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CapacityFactorOrigin getGreenRatioOrigin() {
    return greenRatioOrigin;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean setGreenRatio(double greenRatio, CapacityFactorOrigin origin) {
    if (!IntersectionUtils.isValidCapacityFactor(greenRatio)) {
      return false;
    }
    this.greenRatio = greenRatio;
    this.greenRatioOrigin = origin;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeGreenRatio() {
    if (greenRatio == null) {
      return false;
    }
    this.greenRatio = null;
    this.greenRatioOrigin = null;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSignalSegmentImpl shallowClone() {
    return new IntersectionSignalSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSignalSegmentImpl deepClone() {
    return new IntersectionSignalSegmentImpl(this, true);
  }
}
