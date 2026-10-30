package org.goplanit.network.layer;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.containers.IdentityEntityIndex;
import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.graph.directed.BannedTurnFactory;
import org.goplanit.utils.graph.directed.BannedTurns;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 
 * Movements primary managed container implementation, with a lookup of the banned turns starting or ending at a
 * link segment. Registering a banned turn indexes the segments it refers to at that moment; later raw edits of its
 * segments are not seen by the lookup, see {@link #update(BannedTurn, Consumer)}
 *
 * @author markr
 *
 */
public class BannedTurnsImpl extends ManagedGraphEntitiesImpl<BannedTurn> implements BannedTurns {

  /** creates the factory of a container, kept so that a copy gets a factory of the same kind */
  private final BiFunction<IdGroupingToken, BannedTurns, BannedTurnFactory> factoryCreator;

  /** factory to use */
  private final BannedTurnFactory bannedTurnFactory;

  /** link segment to the banned turns starting or ending at it, by identity */
  private final EntityIndex<EdgeSegment, BannedTurn> bySegment = new IdentityEntityIndex<>(
      bannedTurn -> Arrays.asList(bannedTurn.getSegmentFrom(), bannedTurn.getSegmentTo()));

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public BannedTurnsImpl(final IdGroupingToken groupId) {
    this(groupId, BannedTurnFactoryImpl::new);
  }

  /**
   * Constructor for a container whose factory creates a particular kind of banned turn, such as the banned turns of
   * a macroscopic layer, which name the modes they ban
   *
   * @param groupId     to use for creating ids for instances
   * @param factoryCreator creates the factory for a container from the id grouping token and the container itself
   */
  protected BannedTurnsImpl(
      final IdGroupingToken groupId, BiFunction<IdGroupingToken, BannedTurns, BannedTurnFactory> factoryCreator) {
    super(BannedTurn::getId, BannedTurn.BANNED_TURN_ID_CLASS);
    this.factoryCreator = factoryCreator;
    this.bannedTurnFactory = factoryCreator.apply(groupId, this);
  }

  /**
   * Copy constructor, also creates new factory of the same kind with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy (may be null)
   */
  public BannedTurnsImpl(
      BannedTurnsImpl other, boolean deepCopy, BiConsumer<BannedTurn, BannedTurn> mapper) {
    super(other, deepCopy, mapper);
    this.factoryCreator = other.factoryCreator;
    this.bannedTurnFactory = factoryCreator.apply(other.bannedTurnFactory.getIdGroupingToken(), this);
    bySegment.reindex(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurnFactory getFactory() {
    return bannedTurnFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurn register(final BannedTurn bannedTurn) {
    var previous = super.register(bannedTurn);
    bySegment.replace(previous, bannedTurn);
    return previous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurn remove(final BannedTurn bannedTurn) {
    var removed = super.remove(bannedTurn);
    bySegment.unindex(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurn remove(final long key) {
    var removed = super.remove(key);
    bySegment.unindex(removed);
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeIf(Predicate<BannedTurn> condition) {
    var toRemove = new ArrayList<BannedTurn>();
    forEach(bannedTurn -> {
      if (condition.test(bannedTurn)) {
        toRemove.add(bannedTurn);
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
  public List<BannedTurn> getBySegment(EdgeSegment segment) {
    return bySegment.get(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(BannedTurn bannedTurn, Consumer<BannedTurn> change) {
    bySegment.update(bannedTurn, change);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends EdgeSegment> void updateSegmentMapping(
      Function<T, T> segmentToSegmentMapping, boolean removeMissingMappings) {
    Set<BannedTurn> unmapped = new TreeSet<>();
    for (var bannedTurn : this) {
      if (bannedTurn.hasSegmentFrom()) {
        var from = segmentToSegmentMapping.apply((T) bannedTurn.getSegmentFrom());
        if (from != null) {
          bannedTurn.setSegmentFrom(from);
        } else {
          unmapped.add(bannedTurn);
        }
      }
      if (bannedTurn.hasSegmentTo()) {
        var to = segmentToSegmentMapping.apply((T) bannedTurn.getSegmentTo());
        if (to != null) {
          bannedTurn.setSegmentTo(to);
        } else {
          unmapped.add(bannedTurn);
        }
      }
    }
    if (removeMissingMappings) {
      unmapped.forEach(this::remove);
    }
    bySegment.reindex(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurnsImpl shallowClone() {
    return new BannedTurnsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurnsImpl deepClone() {
    return new BannedTurnsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedTurnsImpl deepCloneWithMapping(BiConsumer<BannedTurn, BannedTurn> mapper) {
    return new BannedTurnsImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), BannedTurn.BANNED_TURN_ID_CLASS);
    super.reset();
  }
}
