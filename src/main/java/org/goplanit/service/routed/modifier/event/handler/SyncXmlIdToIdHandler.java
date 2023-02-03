package org.goplanit.graph.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierEventType;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierListener;

/**
 * Whenever routed services managed Ids with an external id are changed in terms of their internal id, their XML ids remain the same and might no longer be unique.
 * When this is not desirbale and the user wants to keep the XML ids unique, for example when the network is persisted to disk afterwards, in which case the XML ids must be unique, then
 * this handler can be used to sync the XML ids to the newly assigned unique internal ids.
 *
 * @author markr
 */
public abstract class SyncXmlIdToIdHandler<T extends ExternalIdAble> implements RoutedServicesModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdHandler.class.getCanonicalName());

  /** supported event type */
  private final RoutedServicesModifierEventType eventType;

  /**
   * Perform action by syncing XML ids to ids
   *
   * @param entity entity to sync XML id to internal id
   */
  protected void syncXmlIdToInternalId(ExternalIdAble entity) {
    entity.setXmlId(String.valueOf(entity.getId()));
  }

  /**
   * Default constructor
   */
  public SyncXmlIdToIdHandler(RoutedServicesModifierEventType eventType) {
    super();
    this.eventType = eventType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] {eventType };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRoutedServicesModifierEvent(RoutedServicesModificationEvent event) {
    if (!event.getType().equals(eventType)) {
      LOGGER.warning(String.format("%s does not support event type %s", SyncXmlIdToIdHandler.class.getName(), event.getType()));
      return;
    }
  }

}
