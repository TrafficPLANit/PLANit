package org.goplanit.network.layer.macroscopic.intersection.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;

/**
 * Event fired when the ids of the intersections of a layer have been recreated
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
   */
  public RecreatedIntersectionsManagedIdsEvent(
      final MacroscopicNetworkLayerModifier source, final Intersections intersections) {
    super(EVENT_TYPE, source, new Object[] {intersections});
  }

  /**
   * The intersections whose ids have been recreated
   *
   * @return intersections
   */
  public Intersections getIntersections() {
    return (Intersections) getContent()[0];
  }
}
