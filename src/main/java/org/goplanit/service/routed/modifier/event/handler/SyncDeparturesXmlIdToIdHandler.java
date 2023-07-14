package org.goplanit.service.routed.modifier.event.handler;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierListener;
import org.goplanit.zoning.modifier.event.ModifiedTripScheduleDepartureIdsEvent;

import java.util.logging.Logger;

/**
 * Whenever routed services' departures' managed Ids with an external id are changed in terms of their internal id, their XML ids remain the same and might no longer be unique.
 * When this is not desirable and the user wants to keep the XML ids unique, for example when the network is persisted to disk afterwards, in which case the XML ids must be unique, then
 * this handler can be used to sync the XML ids to the newly assigned unique internal ids.
 *
 * Class supports {@link org.goplanit.zoning.modifier.event.ModifiedTripScheduleDepartureIdsEvent}.EVENT_TYPE to apply its syncing functionality upon notification
 *
 * @author markr
 */
public class SyncDeparturesXmlIdToIdHandler extends SyncXmlIdToIdHandler implements RoutedServicesModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncDeparturesXmlIdToIdHandler.class.getCanonicalName());

  /**
   * Default constructor
   */
  public SyncDeparturesXmlIdToIdHandler() {
    super(ModifiedTripScheduleDepartureIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRoutedServicesModifierEvent(RoutedServicesModificationEvent event) {
    super.onRoutedServicesModifierEvent(event);

    /* visit all departures in the layer and sync their XML ids to their internal id */
    var routedServicesLayer = ((ModifiedTripScheduleDepartureIdsEvent)event).getModifiedRoutedServicesLayer();
    routedServicesLayer.forEach( rsm -> rsm.forEach( rs -> rs.getTripInfo().getScheduleBasedTrips().forEach( routedTripSchedule ->
        routedTripSchedule.getDepartures().forEach( departure -> syncXmlIdToInternalId(departure)))));
  }

}
