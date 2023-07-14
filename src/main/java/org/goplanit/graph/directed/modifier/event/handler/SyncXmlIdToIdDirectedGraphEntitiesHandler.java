package org.goplanit.graph.directed.modifier.event.handler;

import org.goplanit.graph.directed.modifier.event.RecreatedDirectedGraphEntitiesManagedIdsEvent;
import org.goplanit.graph.modifier.event.RecreatedGraphEntitiesManagedIdsEvent;
import org.goplanit.graph.modifier.event.handler.SyncXmlIdToIdGraphEntitiesHandler;
import org.goplanit.utils.graph.GraphEntity;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;

import java.util.logging.Logger;

/**
 * Sync the directed graph entities' (including edges and vertices) XML id in the container to the internal id. Listens to #RecreatedGraphEntitiesManagedIdsEvent and
 * #RecreatedDirectedGraphEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdDirectedGraphEntitiesHandler extends SyncXmlIdToIdGraphEntitiesHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdDirectedGraphEntitiesHandler.class.getCanonicalName());

  /**
   * Default constructor
   *
   */
  public SyncXmlIdToIdDirectedGraphEntitiesHandler() {
    super(RecreatedDirectedGraphEntitiesManagedIdsEvent.EVENT_TYPE);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (!event.getType().equals(RecreatedGraphEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedGraphEntitiesManagedIdsEvent events", SyncXmlIdToIdDirectedGraphEntitiesHandler.class.getName()));
      return;
    }

    RecreatedGraphEntitiesManagedIdsEvent theEvent = RecreatedGraphEntitiesManagedIdsEvent.class.cast(event);
    theEvent.getManagedIdEntities().forEach( e -> super.syncXmlIdToInternalId((GraphEntity) e));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(RecreatedDirectedGraphEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedDirectedGraphEntitiesManagedIdsEvent events", SyncXmlIdToIdDirectedGraphEntitiesHandler.class.getName()));
      return;
    }

    RecreatedDirectedGraphEntitiesManagedIdsEvent theEvent = RecreatedDirectedGraphEntitiesManagedIdsEvent.class.cast(event);
    theEvent.getManagedIdEntities().forEach( e -> super.syncXmlIdToInternalId((GraphEntity) e));
  }
}
