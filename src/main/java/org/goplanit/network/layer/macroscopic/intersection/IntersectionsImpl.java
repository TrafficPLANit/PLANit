package org.goplanit.network.layer.macroscopic.intersection;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.containers.IdentityEntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionFactory;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Container for the intersections of a macroscopic network layer, with a lookup of the intersection a node is a member
 * of, and of the intersection a link segment belongs to. Registering an intersection indexes what it refers to at that
 * moment; later raw edits on it are not seen by the lookup
 *
 * @author markr
 */
public class IntersectionsImpl extends ManagedIdEntitiesImpl<Intersection> implements Intersections {

  /** factory to create intersections with */
  private final IntersectionFactory intersectionFactory;

  /** member node to the intersections it belongs to, the first indexed returned; by identity */
  private final EntityIndex<Node, Intersection> byMemberNode = new IdentityEntityIndex<>(Intersection::getMemberNodes);

  /**
   * link segment to the intersections referring to it as approach or internal segment, the first indexed returned; by
   * identity. A valid layer has at most one per segment, since a node is a member of at most one
   */
  private final EntityIndex<MacroscopicLinkSegment, Intersection> bySegment = new IdentityEntityIndex<>(
      intersection -> Stream.concat(intersection.getApproachSegments().stream(),
          intersection.getInternalSegments().stream()).collect(Collectors.toList()));

  /**
   * Index everything the intersection refers to at this moment
   *
   * @param intersection to index
   */
  private void index(Intersection intersection) {
    byMemberNode.index(intersection);
    bySegment.index(intersection);
  }

  /**
   * Remove what the intersection refers to at this moment from the index
   *
   * @param intersection to unindex
   */
  private void unindex(Intersection intersection) {
    byMemberNode.unindex(intersection);
    bySegment.unindex(intersection);
  }

  /**
   * Rebuild the lookups from what every registered intersection refers to at this moment, e.g. after raw edits of
   * registered intersections
   */
  public void reindex() {
    byMemberNode.reindex(this);
    bySegment.reindex(this);
  }

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
    forEach(this::index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionFactory getFactory() {
    return intersectionFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Intersection register(final Intersection intersection) {
    var previous = super.register(intersection);
    byMemberNode.replace(previous, intersection);
    bySegment.replace(previous, intersection);
    return previous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Intersection remove(final Intersection intersection) {
    var removed = super.remove(intersection);
    if (removed != null) {
      unindex(removed);
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Intersection remove(final long key) {
    var removed = super.remove(key);
    if (removed != null) {
      unindex(removed);
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeIf(Predicate<Intersection> condition) {
    var toRemove = new ArrayList<Intersection>();
    forEach(intersection -> {
      if (condition.test(intersection)) {
        toRemove.add(intersection);
      }
    });
    toRemove.forEach(this::remove);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    super.clear();
    byMemberNode.clear();
    bySegment.clear();
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
   * Update the approach and internal segments of every intersection based on the mapping provided, e.g. to their
   * copies after a deep copy of the layer; the index is rebuilt to match
   *
   * @param segmentToSegmentMapping should contain the segment as currently used and then the value is the new segment
   *                                to replace it
   * @param removeMissingMappings when true a segment without a mapping is removed, otherwise it is left in-tact
   */
  public void updateSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> segmentToSegmentMapping, boolean removeMissingMappings) {
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
  public Intersection getBySegment(MacroscopicLinkSegment segment) {
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
   * Apply a change to a registered intersection, keeping the lookups in step with what it refers to before and after.
   * For keeping the container consistent with changes made through the layer modifier
   *
   * @param intersection to change
   * @param change to apply to it
   */
  public void update(Intersection intersection, Consumer<Intersection> change) {
    unindex(intersection);
    change.accept(intersection);
    index(intersection);
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
