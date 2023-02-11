package org.goplanit.graph.modifier.event.handler;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.graph.modifier.event.RecreatedGraphEntitiesManagedIdsEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;

import java.util.logging.Logger;

/**
 * Sync the graph entities' XML id in the container to the internal id. Listens to #RecreatedGraphEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdGraphEntitiesHandler extends SyncXmlIdToIdHandler implements GraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdGraphEntitiesHandler.class.getCanonicalName());

  /**
   * Allow super classes to add additional event types to support
   *
   * @param superEventType to also support
   */
  protected SyncXmlIdToIdGraphEntitiesHandler(EventType superEventType) {
    super(RecreatedGraphEntitiesManagedIdsEvent.EVENT_TYPE, superEventType);
  }

  /**
   * Default constructor
   *
   */
  public SyncXmlIdToIdGraphEntitiesHandler() {
    super(RecreatedGraphEntitiesManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (!event.getType().equals(RecreatedGraphEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedGraphEntitiesManagedIdsEvent events", SyncXmlIdToIdGraphEntitiesHandler.class.getName()));
      return;
    }

    RecreatedGraphEntitiesManagedIdsEvent theEvent = RecreatedGraphEntitiesManagedIdsEvent.class.cast(event);
    theEvent.getManagedIdEntities().forEach( e -> super.syncXmlIdToInternalId((GraphEntity) e));
  }
}
