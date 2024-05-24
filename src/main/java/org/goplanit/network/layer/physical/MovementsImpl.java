package org.goplanit.network.layer.physical;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.layer.physical.MovementFactory;
import org.goplanit.utils.network.layer.physical.Movements;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.function.BiConsumer;

/**
 * 
 * Movements primary managed container implementation
 * 
 * @author markr
 *
 */
public class MovementsImpl extends ManagedIdEntitiesImpl<Movement> implements Movements {

  /** factory to use */
  private final MovementFactory movementFactory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public MovementsImpl(final IdGroupingToken groupId) {
    super(Movement::getId, Movement.MOVEMENT_ID_CLASS);
    this.movementFactory = new MovementFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId     to use for creating ids for instances
   * @param factory the factory to use
   */
  public MovementsImpl(final IdGroupingToken groupId, MovementFactory factory) {
    super(Movement::getId, Movement.MOVEMENT_ID_CLASS);
    this.movementFactory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public MovementsImpl(MovementsImpl other, boolean deepCopy, BiConsumer<Movement,Movement> mapper) {
    super(other, deepCopy, mapper);
    this.movementFactory = new MovementFactoryImpl(other.movementFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MovementFactory getFactory() {
    return movementFactory;
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
  public MovementsImpl shallowClone() {
    return new MovementsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MovementsImpl deepClone() {
    return new MovementsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MovementsImpl deepCloneWithMapping(BiConsumer<Movement, Movement> mapper) {
    return new MovementsImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    IdGenerator.reset(getFactory().getIdGroupingToken(), Movement.MOVEMENT_ID_CLASS);
    super.reset();
  }
}
