package org.goplanit.network.layer.macroscopic.intersection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;
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

  /** internal segments, one for each link segment lying inside the intersection between member nodes, in the order
   * added */
  private final List<IntersectionSegmentImpl> internalSegments;

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
    this.movements = new ArrayList<>();
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own collections and their own copies of the approaches
   * and internal segments, referring to the same nodes, segments and movements; re-pointing those to copied ones is up
   * to the copy of the layer
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected SpatialIntersectionImpl(final SpatialIntersectionImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.spatialIntersectionId = other.spatialIntersectionId;
    this.internalSegments = new ArrayList<>(other.internalSegments.size());
    other.internalSegments.forEach(internal -> internalSegments.add(internal.deepClone()));
    this.movements = new ArrayList<>(other.movements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void removeReferencesOfRemovedMemberNode(Node node) {
    internalSegments.removeIf(internal -> internal.getLinkSegment().getUpstreamVertex() == node ||
        internal.getLinkSegment().getDownstreamVertex() == node);
    movements.removeIf(movement -> getApproach(movement.getLinkSegmentFrom()) == null);
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
  public List<IntersectionSegment> getIntersectionInternalSegments() {
    return Collections.unmodifiableList(internalSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSegment getIntersectionInternalSegment(MacroscopicLinkSegment segment) {
    return IntersectionSegmentUtils.findByLinkSegment(internalSegments, segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addNewInternalSegment(MacroscopicLinkSegment segment) {
    if (getIntersectionInternalSegment(segment) != null || !hasMemberNode(segment.getUpstreamVertex())
        || !hasMemberNode(segment.getDownstreamVertex())) {
      return false;
    }
    return internalSegments.add(IntersectionSegmentFactory.create(segment, false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeInternalSegment(MacroscopicLinkSegment segment) {
    return IntersectionSegmentUtils.removeByLinkSegment(internalSegments, segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSegment swapApproachToNewSignalOrUnsignalised(
      MacroscopicLinkSegment approachSegment, boolean hasOwnSignal) {
    var approach = super.swapApproachToNewSignalOrUnsignalised(approachSegment, hasOwnSignal);
    if (approach != null && !hasOwnSignal) {
      getMovementsFrom(approach).forEach(SpatialMovement::removeGreenRatio);
    }
    return approach;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addNewInternalSegmentWithSignal(MacroscopicLinkSegment segment) {
    var internal = getIntersectionInternalSegment(segment);
    if ((internal != null && internal.hasSignal()) || (internal == null && !addNewInternalSegment(segment))) {
      return false;
    }
    return IntersectionSegmentUtils.swapToSignalOrUnsignalised(internalSegments, segment, true) != null;
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
    if (ListUtils.containsByIdentity(movements, movement) || getApproach(movement.getLinkSegmentFrom()) == null) {
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
    // each internal segment updates its own link segment; one without a mapping is removed when asked
    internalSegments.removeIf(
        internal -> !internal.updateLinkSegmentMapping(segmentToSegmentMapping) && removeMissingMappings);
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
