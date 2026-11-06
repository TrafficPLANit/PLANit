package org.goplanit.network.layer.macroscopic.intersection.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;

/**
 * Event fired when an intersection has been removed from its layer
 *
 * @author markr
 */
public class RemoveIntersectionEvent extends EventImpl implements IntersectionModificationEvent {

  /** event type fired off when an intersection has been removed */
  public static final IntersectionModifierEventType EVENT_TYPE =
      new IntersectionModifierEventType("INTERSECTIONMODIFIER.INTERSECTION.REMOVE");

  /**
   * Constructor
   *
   * @param source layer modifier firing the event
   * @param removedIntersection the intersection removed
   */
  public RemoveIntersectionEvent(final MacroscopicNetworkLayerModifier source, final Intersection removedIntersection) {
    super(EVENT_TYPE, source, new Object[] {removedIntersection});
  }

  /**
   * The removed intersection
   *
   * @return removed intersection
   */
  public Intersection getRemovedIntersection() {
    return (Intersection) getContent()[0];
  }
}
