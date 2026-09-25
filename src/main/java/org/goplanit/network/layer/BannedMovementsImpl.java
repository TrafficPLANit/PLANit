package org.goplanit.network.layer;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.containers.IdentityEntityIndex;
import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovementFactory;
import org.goplanit.utils.graph.directed.BannedMovements;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.MovementUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 
 * Movements primary managed container implementation, with a lookup of the banned movements starting or ending at a
 * link segment. Registering a banned movement indexes the segments it refers to at that moment; later raw edits of its
 * segments are not seen by the lookup, see {@link #update(BannedMovement, Consumer)}
 *
 * @author markr
 *
 */
public class BannedMovementsImpl extends ManagedGraphEntitiesImpl<BannedMovement> implements BannedMovements {

  /** factory to use */
  private final BannedMovementFactory bannedMovementFactory;

  /** link segment to the banned movements starting or ending at it, by identity */
  private final EntityIndex<EdgeSegment, BannedMovement> bySegment = new IdentityEntityIndex<>(
      bannedMovement -> Arrays.asList(bannedMovement.getSegmentFrom(), bannedMovement.getSegmentTo()));

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public BannedMovementsImpl(final IdGroupingToken groupId) {
    super(BannedMovement::getId, BannedMovement.BANNED_MOVEMENT_ID_CLASS);
    this.bannedMovementFactory = new BannedMovementFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId     to use for creating ids for instances
   * @param factory the factory to use
   */
  public BannedMovementsImpl(final IdGroupingToken groupId, BannedMovementFactory factory) {
    super(BannedMovement::getId, BannedMovement.BANNED_MOVEMENT_ID_CLASS);
    this.bannedMovementFactory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy (may be null)
   */
  public BannedMovementsImpl(
      BannedMovementsImpl other, boolean deepCopy, BiConsumer<BannedMovement, BannedMovement> mapper) {
    super(other, deepCopy, mapper);
    this.bannedMovementFactory =
        new BannedMovementFactoryImpl(other.bannedMovementFactory.getIdGroupingToken(), this);
    bySegment.reindex(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementFactory getFactory() {
    return bannedMovementFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovement register(final BannedMovement bannedMovement) {
    var previous = super.register(bannedMovement);
    bySegment.replace(previous, bannedMovement);
    return previous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovement remove(final BannedMovement bannedMovement) {
    var removed = super.remove(bannedMovement);
    bySegment.unindex(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovement remove(final long key) {
    var removed = super.remove(key);
    bySegment.unindex(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeIf(Predicate<BannedMovement> condition) {
    var toRemove = new ArrayList<BannedMovement>();
    forEach(bannedMovement -> {
      if (condition.test(bannedMovement)) {
        toRemove.add(bannedMovement);
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
    bySegment.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BannedMovement> getBySegment(EdgeSegment segment) {
    return bySegment.get(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(BannedMovement bannedMovement, Consumer<BannedMovement> change) {
    bySegment.update(bannedMovement, change);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends EdgeSegment> void updateSegmentMapping(
      Function<T, T> segmentToSegmentMapping, boolean removeMissingMappings) {
    MovementUtils.updateMovementSegmentMapping(this, segmentToSegmentMapping, removeMissingMappings);
    bySegment.reindex(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementsImpl shallowClone() {
    return new BannedMovementsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementsImpl deepClone() {
    return new BannedMovementsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovementsImpl deepCloneWithMapping(BiConsumer<BannedMovement, BannedMovement> mapper) {
    return new BannedMovementsImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), BannedMovement.BANNED_MOVEMENT_ID_CLASS);
    super.reset();
  }
}
