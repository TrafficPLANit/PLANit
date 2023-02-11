package org.goplanit.service.routed.modifier.event.handler;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.service.routed.modifier.event.ModifiedRoutedTripIdsEvent;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierListener;

import java.util.logging.Logger;

/**
 * Whenever routed services' trips managed Ids with an external id are changed in terms of their internal id, their XML ids remain the same and might no longer be unique.
 * When this is not desirable and the user wants to keep the XML ids unique, for example when the network is persisted to disk afterwards, in which case the XML ids must be unique, then
 * this handler can be used to sync the XML ids to the newly assigned unique internal ids.
 *
 * Class supports {@link ModifiedRoutedTripIdsEvent}.EVENT_TYPE to apply its syncing functionality upon notification
 *
 * @author markr
 */
public class SyncRoutedTripsXmlIdToIdHandler extends SyncXmlIdToIdHandler implements RoutedServicesModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncRoutedTripsXmlIdToIdHandler.class.getCanonicalName());

  /**
   * Default constructor
   */
  public SyncRoutedTripsXmlIdToIdHandler() {
    super(ModifiedRoutedTripIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRoutedServicesModifierEvent(RoutedServicesModificationEvent event) {
    super.onRoutedServicesModifierEvent(event);

    /* visit all routed services' trips in the layer and sync their XML ids to their internal id */
    var routedServicesLayer = ((ModifiedRoutedTripIdsEvent)event).getModifiedRoutedServicesLayer();
    routedServicesLayer.forEach( rsm -> rsm.forEach( routedService ->
      {
        routedService.getTripInfo().getScheduleBasedTrips().forEach( routedTripsSchedule -> syncXmlIdToInternalId(routedTripsSchedule));
        routedService.getTripInfo().getFrequencyBasedTrips().forEach( routedTripsFrequency -> syncXmlIdToInternalId(routedTripsFrequency));
      }));
  }

}
