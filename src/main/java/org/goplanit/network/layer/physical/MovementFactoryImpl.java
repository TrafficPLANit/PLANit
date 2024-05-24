package org.goplanit.network.layer.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.*;
import org.locationtech.jts.geom.Point;

/**
 * Factory for creating movements on movement container
 * 
 * @author markr
 */
public class MovementFactoryImpl extends ManagedIdEntityFactoryImpl<Movement> implements MovementFactory {

  /** container */
  private final Movements movements;

  /**
   * Constructor
   *
   * @param groupId  to use
   * @param movements to use
   */
  protected MovementFactoryImpl(final IdGroupingToken groupId, final Movements movements) {
    super(groupId);
    this.movements = movements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Movement create(final EdgeSegment from, final EdgeSegment to) {
    return new MovementImpl(getIdGroupingToken(), from, to);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Movement registerNew(final EdgeSegment from, final EdgeSegment to) {
    final Movement newMovement = create(from, to);
    movements.register(newMovement);
    return newMovement;
  }

}
