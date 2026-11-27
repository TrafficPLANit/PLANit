package org.goplanit.network.layer.macroscopic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovementFactory;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovements;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Container for the spatial movements of a macroscopic network layer, with a lookup of the movements starting or
 * ending on a link segment. Registering a movement indexes its segments at that moment; later raw edits on it are not
 * seen by the lookup, see {@link #update(SpatialMovement, Consumer)}
 *
 * @author markr
 */
public class SpatialMovementsImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<SpatialMovement>
    implements SpatialMovements {

  /** factory to create movements with */
  private final SpatialMovementFactory spatialMovementFactory;

  /** link segment to the movements entering or leaving on it; by identity */
  private final EntityIndex<LinkSegment, SpatialMovement> bySegment = addForeignKeyIndex(
      movement -> Arrays.asList(movement.getLinkSegmentFrom(), movement.getLinkSegmentTo()));

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public SpatialMovementsImpl(final IdGroupingToken groupId) {
    super(SpatialMovement::getId, SpatialMovement.SPATIAL_MOVEMENT_ID_CLASS);
    this.spatialMovementFactory = new SpatialMovementFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public SpatialMovementsImpl(final SpatialMovementsImpl other, boolean deepCopy,
      BiConsumer<SpatialMovement, SpatialMovement> mapper) {
    super(other, deepCopy, mapper);
    this.spatialMovementFactory =
        new SpatialMovementFactoryImpl(other.spatialMovementFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementFactory getFactory() {
    return spatialMovementFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SpatialMovement> getBySegment(LinkSegment segment) {
    return bySegment.get(segment);
  }

  /**
   * Replace the segments of every movement based on the mapping provided, e.g. to their copies after a deep copy of
   * the layer, keeping the lookup by segment in step. A movement with a segment without a mapping is removed
   *
   * @param <S> type of link segment the mapping takes, for example of the layer when it maps a deep copy
   * @param segmentToSegmentMapping should contain the segment as currently used and then the value is the new segment
   *                                to replace it
   */
  @SuppressWarnings("unchecked")
  public <S extends LinkSegment> void updateSegmentMapping(Function<S, ? extends LinkSegment> segmentToSegmentMapping) {
    var unmapped = new ArrayList<SpatialMovement>();
    for (var movement : this) {
      var from = segmentToSegmentMapping.apply((S) movement.getLinkSegmentFrom());
      var to = segmentToSegmentMapping.apply((S) movement.getLinkSegmentTo());
      if (from == null || to == null) {
        unmapped.add(movement);
        continue;
      }
      movement.setLinkSegmentFrom(from);
      movement.setLinkSegmentTo(to);
    }
    unmapped.forEach(this::remove);
    reindex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementsImpl shallowClone() {
    return new SpatialMovementsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementsImpl deepClone() {
    return new SpatialMovementsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovementsImpl deepCloneWithMapping(
      BiConsumer<SpatialMovement, SpatialMovement> mapper) {
    return new SpatialMovementsImpl(this, true, mapper);
  }
}
