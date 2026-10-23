package org.goplanit.network.layer.macroscopic;


import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovementFactory;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovements;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Factory to create spatial movements and register them on their container
 *
 * @author markr
 */
public class SpatialMovementFactoryImpl extends ManagedIdEntityFactoryImpl<SpatialMovement>
    implements SpatialMovementFactory {

  /** container to register on */
  private final SpatialMovements spatialMovements;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids
   * @param spatialMovements to register on
   */
  protected SpatialMovementFactoryImpl(
      final IdGroupingToken groupId, final SpatialMovements spatialMovements) {
    super(groupId);
    this.spatialMovements = spatialMovements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovement create(
      LinkSegment from, LinkSegment to, double lengthKm) {
    return new SpatialMovementImpl(getIdGroupingToken(), from, to, lengthKm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SpatialMovement registerNew(
      LinkSegment from, LinkSegment to, double lengthKm) {
    var movement = create(from, to, lengthKm);
    spatialMovements.register(movement);
    return movement;
  }
}
