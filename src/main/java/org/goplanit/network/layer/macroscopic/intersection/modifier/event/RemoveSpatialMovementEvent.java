package org.goplanit.network.layer.macroscopic.intersection.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;

/**
 * Event fired when a spatial movement, of a spatial intersection or independent, has been removed from its layer
 *
 * @author markr
 */
public class RemoveSpatialMovementEvent extends EventImpl implements IntersectionModificationEvent {

  /** event type fired off when a spatial movement has been removed */
  public static final IntersectionModifierEventType EVENT_TYPE =
      new IntersectionModifierEventType("INTERSECTIONMODIFIER.SPATIALMOVEMENT.REMOVE");

  /**
   * Constructor
   *
   * @param source layer modifier firing the event
   * @param removedSpatialMovement the movement removed
   */
  public RemoveSpatialMovementEvent(
      final MacroscopicNetworkLayerModifier source, final SpatialMovement removedSpatialMovement) {
    super(EVENT_TYPE, source, new Object[] {removedSpatialMovement});
  }

  /**
   * The removed movement
   *
   * @return removed spatial movement
   */
  public SpatialMovement getRemovedSpatialMovement() {
    return (SpatialMovement) getContent()[0];
  }
}
