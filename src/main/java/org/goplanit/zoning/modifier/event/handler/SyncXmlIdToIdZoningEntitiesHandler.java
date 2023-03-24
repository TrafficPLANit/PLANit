package org.goplanit.zoning.modifier.event.handler;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;
import org.goplanit.zoning.modifier.event.RecreatedZoningEntitiesManagedIdsEvent;

import java.util.logging.Logger;

/**
 * Sync the graph entities' XML id in the container to the internal id. Listens to #RecreatedGraphEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdZoningEntitiesHandler extends SyncXmlIdToIdHandler implements ZoningModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdZoningEntitiesHandler.class.getCanonicalName());

  /**
   * Default constructor
   *
   */
  public SyncXmlIdToIdZoningEntitiesHandler() {
    super(RecreatedZoningEntitiesManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onZoningModifierEvent(ZoningModificationEvent event) {
    if (!event.getType().equals(RecreatedZoningEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedZoningEntitiesManagedIdsEvent events", SyncXmlIdToIdZoningEntitiesHandler.class.getName()));
      return;
    }

    RecreatedZoningEntitiesManagedIdsEvent theEvent = RecreatedZoningEntitiesManagedIdsEvent.class.cast(event);
    theEvent.getManagedIdEntities().forEach( e -> super.syncXmlIdToInternalId((ExternalIdAble) e));
  }
}
