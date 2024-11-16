package org.goplanit.network.layer.physical;

import org.goplanit.graph.directed.EdgeSegmentImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Conjugate Link segment object representing conjugate of original network's adjacent link segment pair, i.e. turn
 *
 * @author markr
 *
 */
public class ConjugateLinkSegmentImpl extends LinkSegmentBase<ConjugateLink> implements ConjugateLinkSegment {

  //todo link segment base is not consistently implemented --> to fix this make linksegmentBase not abstract and implement
  // all methods, the ones we do not have info on throw, since its derived classes (this and macroscopic link segment have their own
  // implementation

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
  public ConjugateLinkSegmentImpl getOppositeDirectionSegment() {
    return (ConjugateLinkSegmentImpl) ConjugateLinkSegment.super.getOppositeDirectionSegment();
  }

  @Override
  public boolean hasOppositeDirectionSegment() {
    return super.hasOppositeDirectionSegment();
  }

  @Override
  public boolean isParentGeometryInSegmentDirection(boolean allowSingleVertexWithoutGeometry) {
    return super.isParentGeometryInSegmentDirection(allowSingleVertexWithoutGeometry);
  }

  @Override
  public boolean isAdjacent(EdgeSegment other, boolean allowUTurn) {
    return super.isAdjacent(other, allowUTurn);
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

  @Override
  public ConjugateLinkImpl getParentLink() {
    return (ConjugateLinkImpl) super.getParentLink();
  }

  @Override
  public ConjugateNode getUpstreamNode() {
    return (ConjugateNode) super.getUpstreamNode();
  }

  @Override
  public ConjugateNode getDownstreamNode() {
    return (ConjugateNode) super.getDownstreamNode();
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

}
