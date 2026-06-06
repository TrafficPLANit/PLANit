package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovementFactory;
import org.goplanit.utils.graph.directed.BannedMovements;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.*;

import java.util.function.BiConsumer;

/**
 * 
 * Movements primary managed container implementation
 * 
 * @author markr
 *
 */
public class BannedMovementsImpl extends ManagedIdEntitiesImpl<BannedMovement> implements BannedMovements {

  /** factory to use */
  private final BannedMovementFactory bannedMovementFactory;

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
  public BannedMovementsImpl(BannedMovementsImpl other, boolean deepCopy, BiConsumer<BannedMovement, BannedMovement> mapper) {
    super(other, deepCopy, mapper);
    this.bannedMovementFactory = new BannedMovementFactoryImpl(other.bannedMovementFactory.getIdGroupingToken(), this);
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
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional node id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), Node.NODE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
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
