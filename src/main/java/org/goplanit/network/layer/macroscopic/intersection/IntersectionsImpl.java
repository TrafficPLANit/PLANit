package org.goplanit.network.layer.macroscopic.intersection;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionFactory;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Container for the intersections of a macroscopic network layer, with a lookup of the intersection a node is a member
 * of, and of the intersection a link segment belongs to. Registering an intersection indexes what it refers to at that
 * moment; a later direct edit of it is seen once it is reindexed
 * <p>
 * The layer modifier keeps the intersections in step with every change to the graph, so for each node or link segment
 * it removes or breaks it needs the intersection concerned at once. For example, removing a member node of a spatial
 * intersection finds that intersection by the node, and breaking one of its approach segments finds it by the segment,
 * without going over every intersection of the layer.
 * </p>
 *
 * @author markr
 */
public class IntersectionsImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<Intersection>
    implements Intersections {

  /** factory to create intersections with */
  private final IntersectionFactory intersectionFactory;

  /** member node to the intersections it belongs to, the first indexed returned; by identity */
  private final EntityIndex<Node, Intersection> byMemberNode = addForeignKeyIndex(Intersection::getMemberNodes);

  /**
   * link segment to the intersections referring to it as approach or internal segment, the first indexed returned; by
   * identity. A valid layer has at most one per segment, since a node is a member of at most one
   */
  private final EntityIndex<LinkSegment, Intersection> bySegment = addForeignKeyIndex(
      intersection -> Stream.concat(intersection.getApproachSegments().stream(),
          intersection instanceof SpatialIntersection ?
              ((SpatialIntersection) intersection).getInternalSegments().stream() : Stream.empty())
          .collect(Collectors.toList()));

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public IntersectionsImpl(final IdGroupingToken groupId) {
    super(Intersection::getId, Intersection.INTERSECTION_ID_CLASS);
    this.intersectionFactory = new IntersectionFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public IntersectionsImpl(
      final IntersectionsImpl other, boolean deepCopy, BiConsumer<Intersection, Intersection> mapper) {
    super(other, deepCopy, mapper);
    this.intersectionFactory = new IntersectionFactoryImpl(other.intersectionFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionFactory getFactory() {
    return intersectionFactory;
  }

  /**
   * Update the member nodes of every intersection based on the mapping provided, e.g. to their copies after a deep copy
   * of the layer. An intersection left without member nodes is removed; the index is rebuilt to match
   *
   * @param nodeToNodeMapping should contain the member node as currently used and then the value is the new node to
   *                          replace it
   * @param removeMissingMappings when true a member node without a mapping is removed, otherwise it is left in-tact
   */
  public void updateMemberNodeMapping(Function<Node, Node> nodeToNodeMapping, boolean removeMissingMappings) {
    forEach(intersection ->
        ((IntersectionImpl) intersection).updateMemberNodeMapping(nodeToNodeMapping, removeMissingMappings));
    removeIf(intersection -> intersection.getMemberNodes().isEmpty());
    reindex();
  }

  /**
   * Update the movements of every spatial intersection based on the mapping provided, e.g. to their copies after a
   * deep copy of the layer
   *
   * @param movementToMovementMapping should contain the movement as currently used and then the value is the new
   *                                  movement to replace it
   */
  public void updateMovementMapping(Function<SpatialMovement, SpatialMovement> movementToMovementMapping) {
    forEach(intersection -> {
      if (intersection instanceof SpatialIntersectionImpl) {
        ((SpatialIntersectionImpl) intersection).updateMovementMapping(movementToMovementMapping);
      }
    });
  }

  /**
   * Update the segments of every intersection based on the mapping provided, e.g. to their copies after a deep copy of
   * the layer; the index is rebuilt to match
   *
   * @param <S> type of link segment the mapping takes, for example of the layer when it maps a deep copy
   * @param segmentToSegmentMapping should contain the segment as currently used and then the value is the new segment
   *                                to replace it
   * @param removeMissingMappings when true a segment without a mapping is removed, otherwise it is left in-tact
   */
  public <S extends LinkSegment> void updateSegmentMapping(
      Function<S, ? extends LinkSegment> segmentToSegmentMapping, boolean removeMissingMappings) {
    forEach(intersection ->
        ((IntersectionImpl) intersection).updateSegmentMapping(segmentToSegmentMapping, removeMissingMappings));
    reindex();
  }

  /**
   * Collect the registered intersection referring to the link segment as approach or internal segment, the first
   * indexed when several do
   *
   * @param segment to look up
   * @return intersection, null when none
   */
  public Intersection getBySegment(LinkSegment segment) {
    return bySegment.getFirst(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Intersection getByMemberNode(Node node) {
    return byMemberNode.getFirst(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the ids of each kind, kept next to the intersection id */
    IdGenerator.reset(getFactory().getIdGroupingToken(), NonSpatialIntersection.NON_SPATIAL_INTERSECTION_ID_CLASS);
    IdGenerator.reset(getFactory().getIdGroupingToken(), SpatialIntersection.SPATIAL_INTERSECTION_ID_CLASS);
    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionsImpl shallowClone() {
    return new IntersectionsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionsImpl deepClone() {
    return new IntersectionsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionsImpl deepCloneWithMapping(BiConsumer<Intersection, Intersection> mapper) {
    return new IntersectionsImpl(this, true, mapper);
  }
}
