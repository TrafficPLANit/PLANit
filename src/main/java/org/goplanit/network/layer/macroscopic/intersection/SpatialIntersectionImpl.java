package org.goplanit.network.layer.macroscopic.intersection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * A spatial intersection: several member nodes, the internal segments between them, its approaches and every movement
 * through it.
 * <p>
 * It lists a movement whether or not a mode can make it. A movement can be banned for a mode in two ways:
 * </p>
 * <ul>
 * <li>explicitly, by a direct ban stored on the movement, see {@link SpatialMovement#hasDirectBan()};</li>
 * <li>implicitly, by the network inside the intersection: a turn banned for the mode at a member node, or an internal
 * segment the mode has no access to, can leave the mode with no connection from the approach to the exit.</li>
 * </ul>
 * <p>
 * An implicit ban is not stored, on the movement or here. It is worked out from the network when asked, see
 * {@link org.goplanit.network.layer.macroscopic.SpatialMovementUtils#isImplicitlyBanned}.
 * For example, buses cannot make a movement from the north approach to the east exit that has no direct ban, when
 * every connection between the two passes a turn banned for buses. Adding or removing that banned turn changes which
 * modes can make the movement, and leaves both the movement and this intersection unchanged.
 * </p>
 *
 * @author markr
 */
public class SpatialIntersectionImpl extends IntersectionImpl implements SpatialIntersection {

  /** generated UID */
  private static final long serialVersionUID = -6046317011436593287L;

  /** id among the spatial intersections of the layer */
  private long spatialIntersectionId;

  /** link segments lying inside the intersection, running between member nodes */
  private final List<MacroscopicLinkSegment> internalSegments;

  /** internal segments with a signal standing on them, each also one of the internal segments */
  private final List<MacroscopicLinkSegment> internalSegmentsWithSignal;

  /** movements through the intersection, each starting on one of its approaches */
  private final List<SpatialMovement> movements;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param memberNodes of the intersection
   * @param controlType of the intersection
   * @param types kinds of the intersection, at least one
   */
  protected SpatialIntersectionImpl(
      final IdGroupingToken groupId, final Collection<Node> memberNodes, final IntersectionControlType controlType,
      final Collection<IntersectionType> types) {
    super(groupId, memberNodes, controlType, types);
    this.spatialIntersectionId = generateSpatialIntersectionId(groupId);
    this.internalSegments = new ArrayList<>();
    this.internalSegmentsWithSignal = new ArrayList<>(0);
    this.movements = new ArrayList<>();
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own collections, referring to the same nodes, segments
   * and movements; re-pointing those to copied ones is up to the copy of the layer
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected SpatialIntersectionImpl(final SpatialIntersectionImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.spatialIntersectionId = other.spatialIntersectionId;
    this.internalSegments = new ArrayList<>(other.internalSegments);
    this.internalSegmentsWithSignal = new ArrayList<>(other.internalSegmentsWithSignal);
    this.movements = new ArrayList<>(other.movements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void removeReferencesOfRemovedMemberNode(Node node) {
    internalSegments.removeIf(segment -> segment.getUpstreamVertex() == node || segment.getDownstreamVertex() == node);
    internalSegmentsWithSignal.removeIf(segment -> !ListUtils.containsByIdentity(internalSegments, segment));
    movements.removeIf(movement -> !ListUtils.containsByIdentity(getApproachSegments(), movement.getLinkSegmentFrom()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void removeReferencesOfRemovedApproachSegment(MacroscopicLinkSegment approachSegment) {
    movements.removeIf(movement -> movement.getLinkSegmentFrom() == approachSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSpatialIntersectionId() {
    return spatialIntersectionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addMemberNode(Node node) {
    if (hasMemberNode(node) ||
        getApproachSegments().stream().anyMatch(segment -> segment.getUpstreamVertex() == node)) {
      return false;
    }
    return addToMemberNodes(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> getInternalSegments() {
    return Collections.unmodifiableList(internalSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment getInternalSegment(long segmentId) {
    return IdAbleUtils.findById(internalSegments, segmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addInternalSegment(MacroscopicLinkSegment segment) {
    if (ListUtils.containsByIdentity(internalSegments, segment) || !hasMemberNode(segment.getUpstreamVertex())
        || !hasMemberNode(segment.getDownstreamVertex())) {
      return false;
    }
    return internalSegments.add(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeInternalSegment(MacroscopicLinkSegment segment) {
    internalSegmentsWithSignal.removeIf(held -> held == segment);
    return internalSegments.removeIf(held -> held == segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> getInternalSegmentsWithSignal() {
    return Collections.unmodifiableList(internalSegmentsWithSignal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addInternalSegmentWithSignal(MacroscopicLinkSegment segment) {
    if (ListUtils.containsByIdentity(internalSegmentsWithSignal, segment) ||
        (!ListUtils.containsByIdentity(internalSegments, segment) && !addInternalSegment(segment))) {
      return false;
    }
    return internalSegmentsWithSignal.add(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SpatialMovement> getMovements() {
    return Collections.unmodifiableList(movements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addMovement(SpatialMovement movement) {
    if (ListUtils.containsByIdentity(movements, movement) ||
        !ListUtils.containsByIdentity(getApproachSegments(), movement.getLinkSegmentFrom())) {
      return false;
    }
    return movements.add(movement);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeMovement(SpatialMovement movement) {
    return movements.removeIf(held -> held == movement);
  }

  /**
   * Update the movements based on the mapping provided, e.g. to their copies after a deep copy of the layer
   *
   * @param movementToMovementMapping should contain the movement as currently used and then the value is the new
   *                                  movement to replace it
   */
  protected void updateMovementMapping(Function<SpatialMovement, SpatialMovement> movementToMovementMapping) {
    ListUtils.updateMapping(movements, movementToMovementMapping, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> segmentToSegmentMapping, boolean removeMissingMappings) {
    super.updateSegmentMapping(segmentToSegmentMapping, removeMissingMappings);
    ListUtils.updateMapping(internalSegments, segmentToSegmentMapping, removeMissingMappings);
    ListUtils.updateMapping(internalSegmentsWithSignal, segmentToSegmentMapping, removeMissingMappings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    this.spatialIntersectionId = generateSpatialIntersectionId(tokenId);
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialIntersectionImpl shallowClone() {
    return new SpatialIntersectionImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialIntersectionImpl deepClone() {
    return new SpatialIntersectionImpl(this, true);
  }
}
