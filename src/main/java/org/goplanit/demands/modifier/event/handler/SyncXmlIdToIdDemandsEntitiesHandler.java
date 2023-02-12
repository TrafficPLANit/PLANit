package org.goplanit.demands.modifier.event.handler;

import org.goplanit.demands.modifier.event.DemandsModificationEvent;
import org.goplanit.demands.modifier.event.DemandsModifierListener;
import org.goplanit.demands.modifier.event.RecreatedDemandsEntitiesManagedIdsEvent;
import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.graph.modifier.event.RecreatedGraphEntitiesManagedIdsEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;

import java.util.logging.Logger;

/**
 * Sync the Demands entities' XML id in the container to the internal id. Listens to #RecreatedDemandsEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdDemandsEntitiesHandler extends SyncXmlIdToIdHandler implements DemandsModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdDemandsEntitiesHandler.class.getCanonicalName());

  /**
   * Allow super classes to add additional event types to support
   *
   * @param superEventType to also support
   */
  protected SyncXmlIdToIdDemandsEntitiesHandler(EventType superEventType) {
    super(RecreatedGraphEntitiesManagedIdsEvent.EVENT_TYPE, superEventType);
  }

  /**
   * Default constructor
   *
   */
  public SyncXmlIdToIdDemandsEntitiesHandler() {
    super(RecreatedDemandsEntitiesManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDemandsModificationEvent(DemandsModificationEvent event) {
    if (!event.getType().equals(RecreatedDemandsEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedDemandsEntitiesManagedIdsEvent events", SyncXmlIdToIdDemandsEntitiesHandler.class.getName()));
      return;
    }

    RecreatedGraphEntitiesManagedIdsEvent theEvent = RecreatedGraphEntitiesManagedIdsEvent.class.cast(event);
    theEvent.getManagedIdEntities().forEach( e -> super.syncXmlIdToInternalId((GraphEntity) e));
  }
}
