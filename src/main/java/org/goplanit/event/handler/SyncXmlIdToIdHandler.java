package org.goplanit.event.handler;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.demands.modifier.event.DemandsModificationEvent;
import org.goplanit.demands.modifier.event.DemandsModifierListener;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModificationEvent;
import org.goplanit.utils.service.routed.modifier.RoutedServicesModifierListener;

/**
 * Whenever managed Ids containers with entities aso supporting an external id are changed in terms of their internal id, their XML ids remain the same and might no longer be unique.
 * When this is not desirable and the user wants to keep the XML ids unique, for example when the network is persisted to disk afterwards, in which case the XML ids must be unique, then
 * this handler can be used to sync the XML ids to the newly assigned unique internal ids.
 *
 * @author markr
 */
public abstract class SyncXmlIdToIdHandler implements RoutedServicesModifierListener, GraphModifierListener, DirectedGraphModifierListener, DemandsModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdHandler.class.getCanonicalName());

  /** supported event types */
  private final EventType[] eventTypes;

  /**
   * Perform action by syncing XML ids to ids
   *
   * @param <T> type of ExternalIdAble
   * @param entity entity to sync XML id to internal id
   */
  protected <T extends ExternalIdAble> void syncXmlIdToInternalId(T entity) {
    entity.setXmlId(String.valueOf(entity.getId()));
  }

  protected void onEvent(Event event){
    if (!Arrays.stream(eventTypes).anyMatch(et -> et.equals(event.getType()))) {
      LOGGER.warning(String.format("%s does not support event type %s", SyncXmlIdToIdHandler.class.getName(), event.getType()));
      return;
    }
  }

  /**
   * Default constructor
   *
   * @param eventTypes event types
   */
  public SyncXmlIdToIdHandler(EventType... eventTypes) {
    super();
    this.eventTypes = eventTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return eventTypes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRoutedServicesModifierEvent(RoutedServicesModificationEvent event) {
    onEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    onEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    onEvent(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDemandsModificationEvent(DemandsModificationEvent event) {
    onEvent(event);
  }
}
