package org.goplanit.network.layer.physical;

import org.goplanit.graph.directed.ConjugateEdgeSegmentImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.ConjugateLink;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.goplanit.utils.network.layer.physical.ConjugateNode;
import org.goplanit.utils.network.layer.physical.LinkSegment;

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
    return (ConjugateNode) super.getUpstreamVertex();
  }

  @Override
  public ConjugateNode getDownstreamVertex() {
    return (ConjugateNode) super.getDownstreamVertex();
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

  /**
   * Length is sum of length of its underlying two edge segments. Computed on-the-fly. If any edge is null, it is assumed
   * length may be set to 0km for that edge.
   *
   * @return on-the-fly length calculation
   */
  @Override
  public double getLengthKm() {
    return ConjugateEdgeSegmentImpl.getLengthKm(this);
  }

  /**
   * Geometry is to be derived from underlying non-conjugate counterpart. Currently, this entails simply
   * requiring an up and downstream node geometry.
   *
   * @return true when up and downstream conjugate node geometry is available to construct on-the-fly geometry,
   * false otherwise
   */
  @Override
  public boolean hasGeometry() {
    return ConjugateEdgeSegmentImpl.hasGeometry(this);
  }

}
