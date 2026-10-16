package org.goplanit.network.layer.macroscopic.intersection.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovements;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;

/**
 * Event fired when the ids of the intersections of a layer, and of its spatial movements, have been recreated
 *
 * @author markr
 */
public class RecreatedIntersectionsManagedIdsEvent extends EventImpl implements IntersectionModificationEvent {

  /** event type fired off when the ids of the intersections have been recreated */
  public static final IntersectionModifierEventType EVENT_TYPE =
      new IntersectionModifierEventType("INTERSECTIONMODIFIER.MANAGEDIDENTITIES.RECREATED");

  /**
   * Constructor
   *
   * @param source layer modifier firing the event
   * @param intersections whose ids have been recreated
   * @param spatialMovements whose ids have been recreated, those of spatial intersections and independent ones alike
   */
  public RecreatedIntersectionsManagedIdsEvent(
      final MacroscopicNetworkLayerModifier source, final Intersections intersections,
      final SpatialMovements spatialMovements) {
    super(EVENT_TYPE, source, new Object[] {intersections, spatialMovements});
  }

  /**
   * The intersections whose ids have been recreated
   *
   * @return intersections
   */
  public Intersections getIntersections() {
    return (Intersections) getContent()[0];
  }

  /**
   * The spatial movements whose ids have been recreated
   *
   * @return spatial movements
   */
  public SpatialMovements getSpatialMovements() {
    return (SpatialMovements) getContent()[1];
  }
}
