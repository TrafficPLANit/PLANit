package org.goplanit.network.layer;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.*;

/**
 * Factory for creating movements on movement container
 * 
 * @author markr
 */
public class MovementFactoryImpl extends ManagedIdEntityFactoryImpl<BannedMovement> implements MovementFactory {

  /** container */
  private final BannedMovements bannedMovements;

  /**
   * Constructor
   *
   * @param groupId  to use
   * @param bannedMovements to use
   */
  protected MovementFactoryImpl(final IdGroupingToken groupId, final BannedMovements bannedMovements) {
    super(groupId);
    this.bannedMovements = bannedMovements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovement create(final EdgeSegment from, final EdgeSegment to) {
    return new BannedMovementImpl(getIdGroupingToken(), from, to);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BannedMovement registerNew(final EdgeSegment from, final EdgeSegment to) {
    final BannedMovement newBannedMovement = create(from, to);
    bannedMovements.register(newBannedMovement);
    return newBannedMovement;
  }

}
