package org.planit.graph.modifier.event.handler;

import java.util.logging.Logger;

import org.planit.graph.modifier.event.BreakEdgeSegmentEvent;
import org.planit.utils.event.EventType;
import org.planit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.planit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.planit.utils.graph.modifier.event.GraphModificationEvent;

/**
 * Whenever edge segments are broken, their edge segments' xml ids remain the same and are no longer unique. It is likely the user wants to keep the XML ids unique despite using
 * internal ids in the memory model. For example when the network is persisted to disk afterwards in which case the XML ids can be used to map ids. In this situation the XML ids
 * need to remain unique.
 * 
 * If it is known that the XML ids are initially synced with the internal ids, then this listener can be used to sync all broken edge segments' xml id to the internal id of these
 * edge seegments ensuring uniqueness after performing a break edge segment action.
 * 
 * Class supports BreakEdgeSegmentEvent.EVENT_TYPE to apply its syncing functionality upon notification
 * 
 * @author markr
 */
public class SyncXmlIdToIdBreakEdgeSegmentHandler implements DirectedGraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdBreakEdgeSegmentHandler.class.getCanonicalName());

  /**
   * Default constructor
   */
  public SyncXmlIdToIdBreakEdgeSegmentHandler() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { BreakEdgeSegmentEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports break edge segment events", SyncXmlIdToIdBreakEdgeSegmentHandler.class.getName()));
  }

  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge segment events", SyncXmlIdToIdBreakEdgeSegmentHandler.class.getName()));
      return;
    }

    BreakEdgeSegmentEvent breakEdgeSegmentEvent = BreakEdgeSegmentEvent.class.cast(event);
    breakEdgeSegmentEvent.getNewlyBrokenEdgeSegment().setXmlId(String.valueOf(breakEdgeSegmentEvent.getNewlyBrokenEdgeSegment().getId()));
  }

}
