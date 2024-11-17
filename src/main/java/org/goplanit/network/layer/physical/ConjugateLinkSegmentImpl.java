package org.goplanit.network.layer.physical;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.*;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Conjugate Link segment object representing conjugate of original network's adjacent link segment pair, i.e. turn
 *
 * @author markr
 *
 */
public class ConjugateLinkSegmentImpl extends LinkSegmentImpl implements ConjugateLinkSegment {

  private static final Logger LOGGER = Logger.getLogger(ConjugateLinkSegmentImpl.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   */
  protected ConjugateLinkSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    this(groupId, null, directionAB);
  }

  /**
   * Constructor
   *
   * @param groupId,    contiguous id generation within this group for instances of this class
   * @param parent      parent link of segment
   * @param directionAb direction of travel
   */
  protected ConjugateLinkSegmentImpl(
      final IdGroupingToken groupId, final ConjugateLink parent, final boolean directionAb) {
    super(groupId, parent, directionAb);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   */
  protected ConjugateLinkSegmentImpl(ConjugateLinkSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  @Override
  public ConjugateLinkSegmentImpl getOppositeDirectionSegment() {
    return (ConjugateLinkSegmentImpl) ConjugateLinkSegment.super.getOppositeDirectionSegment();
  }

  @Override
  public ConjugateLinkImpl getParent() {
    return (ConjugateLinkImpl) super.getParent();
  }

  @Override
  public ConjugateNode getUpstreamNode() {
    return (ConjugateNode) super.getUpstreamNode();
  }

  @Override
  public ConjugateNode getDownstreamNode() {
    return (ConjugateNode) super.getDownstreamNode();
  }

  @Override
  public ConjugateNode getUpstreamVertex() {
    return getUpstreamNode();
  }

  @Override
  public ConjugateNode getDownstreamVertex() {
    return getDownstreamNode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentImpl shallowClone() {
    return new ConjugateLinkSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateLinkSegmentImpl deepClone() {
    return new ConjugateLinkSegmentImpl(this, true);
  }

  // DISALLOWED METHOD CALLS

  @Override
  public boolean isModeAllowed(Mode mode) {
    LOGGER.warning("Allowed modes of conjugate link segment are not available, use non-conjugate counterpart");
    return false;
  }

  @Override
  public Set<Mode> getAllowedModes() {
    LOGGER.warning("Allowed modes of conjugate link segment are not available, use non-conjugate counterpart");
    return null;
  }

  @Override
  public Set<Mode> getAllowedModesFrom(Collection<Mode> modes) {
    LOGGER.warning("Allowed modes of conjugate link segment are not available, use non-conjugate counterpart");
    return null;
  }

  @Override
  public int getNumberOfLanes() {
    LOGGER.warning("Number of lanes on conjugate link segment not available, use non-conjugate counterpart");
    return -1;
  }

  @Override
  public LinkSegment setNumberOfLanes(int numberOfLanes) {
    LOGGER.warning("Number of lanes on conjugate link segment cannot be set, use non-conjugate counterpart");
    return this;
  }

  @Override
  public LinkSegment setPhysicalSpeedLimitKmH(double maximumSpeedKmH) {
    LOGGER.warning("Speed limit on conjugate link segment cannot be set, use non-conjugate counterpart");
    return this;
  }

  @Override
  public double getPhysicalSpeedLimitKmH() {
    LOGGER.warning("Speed limit on conjugate link segment not available, use non-conjugate counterpart");
    return -1;
  }

  @Override
  public double getLengthKm() {
    LOGGER.warning("Length on conjugate link segment not available, use non-conjugate counterpart");
    return -1;
  }

  @Override
  public boolean hasGeometry() {
    LOGGER.warning("Geometry on conjugate link segment not available, use non-conjugate counterpart");
    return false;
  }

}
