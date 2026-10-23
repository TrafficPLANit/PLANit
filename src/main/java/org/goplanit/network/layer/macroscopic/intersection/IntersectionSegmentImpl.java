package org.goplanit.network.layer.macroscopic.intersection;

import java.io.Serializable;
import java.util.function.Function;

import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * What every link segment of an intersection holds, with a signal or not: the link segment and, when set, its
 * saturation flow factor with how it got its value. An intersection holds them in a list, see
 * {@link IntersectionSegmentFactory} to create them and {@link IntersectionSegmentUtils} to find them in the list
 *
 * @author markr
 */
public abstract class IntersectionSegmentImpl implements IntersectionSegment, Serializable {

  /** generated UID */
  private static final long serialVersionUID = 2915708385526283416L;

  /** the link segment */
  private LinkSegment linkSegment;

  /** the saturation flow factor, null when not set */
  private Double saturationFlowFactor;

  /** how the saturation flow factor got its value, null when not set */
  private CapacityFactorOrigin saturationFlowFactorOrigin;

  /**
   * Constructor, without values
   *
   * @param linkSegment the link segment
   */
  protected IntersectionSegmentImpl(LinkSegment linkSegment) {
    this.linkSegment = linkSegment;
  }

  /**
   * Copy constructor, for the same link segment and with the same saturation flow factor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise. Both are the same: the link segment is
   *                 shared either way and the values are plain numbers
   */
  protected IntersectionSegmentImpl(IntersectionSegmentImpl other, boolean deepCopy) {
    this.linkSegment = other.linkSegment;
    this.saturationFlowFactor = other.saturationFlowFactor;
    this.saturationFlowFactorOrigin = other.saturationFlowFactorOrigin;
  }

  /**
   * Set the link segment, for example the new link segment an approach enters on once its link is broken at a node
   *
   * @param linkSegment to set
   */
  protected void setLinkSegment(LinkSegment linkSegment) {
    this.linkSegment = linkSegment;
  }

  /**
   * Update the link segment based on the mapping provided, e.g. to its copy after a deep copy of the layer
   *
   * @param <S> type of link segment the mapping takes, for example of the layer when it maps a deep copy
   * @param segmentToSegmentMapping should contain the link segment as currently used and then the value is the new
   *                                link segment to replace it
   * @return true when the link segment has a mapping and is replaced, false when it has none and is left as it was
   */
  @SuppressWarnings("unchecked")
  protected <S extends LinkSegment> boolean updateLinkSegmentMapping(
      Function<S, ? extends LinkSegment> segmentToSegmentMapping) {
    var mapped = segmentToSegmentMapping.apply((S) linkSegment);
    if (mapped == null) {
      return false;
    }
    this.linkSegment = mapped;
    return true;
  }

  /**
   * Deep clone this instance as a signal segment or an unsignalised one: on the same link segment, with the same
   * saturation flow factor, and the same green ratio when both have a signal. Of its own kind it is
   * {@link #deepClone()}
   *
   * @param hasSignal true for a signal segment, false for an unsignalised one
   * @return the deep copy
   */
  protected abstract IntersectionSegmentImpl deepCloneAsSignalOrUnsignalised(boolean hasSignal);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IntersectionSegmentImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IntersectionSegmentImpl deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment getLinkSegment() {
    return linkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getSaturationFlowFactor() {
    return saturationFlowFactor == null ? 1 : saturationFlowFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CapacityFactorOrigin getSaturationFlowFactorOrigin() {
    return saturationFlowFactorOrigin;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean setSaturationFlowFactor(double saturationFlowFactor, CapacityFactorOrigin origin) {
    if (!IntersectionUtils.isValidCapacityFactor(saturationFlowFactor)) {
      return false;
    }
    this.saturationFlowFactor = saturationFlowFactor;
    this.saturationFlowFactorOrigin = origin;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeSaturationFlowFactor() {
    if (saturationFlowFactor == null) {
      return false;
    }
    this.saturationFlowFactor = null;
    this.saturationFlowFactorOrigin = null;
    return true;
  }
}
