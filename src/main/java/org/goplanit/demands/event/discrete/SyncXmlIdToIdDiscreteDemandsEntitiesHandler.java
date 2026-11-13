package org.goplanit.demands.event.discrete;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.id.ExternalIdAble;

import java.util.logging.Logger;

/**
 * Sync the Discrete Demands entities' XML id in the container to the internal id. Listens
 * to #RecreatedDiscreteDemandsEntitiesManagedIdsEvent
 */
public class SyncXmlIdToIdDiscreteDemandsEntitiesHandler extends SyncXmlIdToIdHandler
    implements DiscreteDemandsModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(
      SyncXmlIdToIdDiscreteDemandsEntitiesHandler.class.getCanonicalName());

  /**
   * Allow super classes to add additional event types to support
   *
   * @param superEventType to also support
   */
  protected SyncXmlIdToIdDiscreteDemandsEntitiesHandler(EventType superEventType) {
    super(RecreatedDiscreteDemandsEntitiesManagedIdsEvent.EVENT_TYPE, superEventType);
  }

  /**
   * Default constructor
   *
   */
  public SyncXmlIdToIdDiscreteDemandsEntitiesHandler() {
    super(RecreatedDiscreteDemandsEntitiesManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDiscreteDemandsModificationEvent(DiscreteDemandsModificationEvent event) {
    if (!event.getType().equals(RecreatedDiscreteDemandsEntitiesManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedDiscreteDemandsEntitiesManagedIdsEvent events",
          SyncXmlIdToIdDiscreteDemandsEntitiesHandler.class.getName()));
      return;
    }

    var theEvent = (RecreatedDiscreteDemandsEntitiesManagedIdsEvent) event;
    theEvent.getManagedIdEntities().forEach(e -> super.syncXmlIdToInternalId((ExternalIdAble) e));
  }
}
