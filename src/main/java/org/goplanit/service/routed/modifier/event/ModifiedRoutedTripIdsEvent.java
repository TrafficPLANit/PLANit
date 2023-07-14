package org.goplanit.service.routed.modifier.event;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.modifier.RoutedServicesLayerModifier;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierEventType;

/**
 * Event fired when routed trip ids of a routed services layer have been changed (potentially)
 *
 * @author markr
 *
 */
public class ModifiedRoutedTripIdsEvent extends ModifiedRoutedServicesLayerEventImpl implements RoutedServicesModificationEvent {

  /** event type fired off when zone ids have been modified */
  public static final RoutedServicesModifierEventType EVENT_TYPE = new RoutedServicesModifierEventType("ROUTEDSERVICESEVENT.MODIFIED_ROUTED_TRIP_IDS");

  /**
   * Wrapper for a modified routed trips ids event indicating that one or more managed internal ids of the routed services layer have been changed
   *
   * @param source zoning modifier firing the event
   * @param routedServicesLayer to use
   */
  public ModifiedRoutedTripIdsEvent(RoutedServicesLayerModifier source, RoutedServicesLayer routedServicesLayer) {
    super(EVENT_TYPE, source, routedServicesLayer);
  }

}