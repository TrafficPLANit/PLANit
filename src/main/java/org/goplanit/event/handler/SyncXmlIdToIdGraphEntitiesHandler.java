package org.goplanit.event.handler;

import org.goplanit.event.RecreatedGraphEntitiesManagedIdsEvent;
import org.goplanit.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.graph.modifier.event.handler.SyncXmlIdToIdBreakEdgeHandler;
import org.goplanit.graph.modifier.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ExternalIdAble;

import java.util.logging.Logger;

/**
 * Sync the graph entities' XML id in the container to the internal id. Listens to #RecreatedGraphEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdGraphEntitiesHandler extends SyncXmlIdToIdHandler<GraphEntity> implements GraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdBreakEdgeHandler.class.getCanonicalName());

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
