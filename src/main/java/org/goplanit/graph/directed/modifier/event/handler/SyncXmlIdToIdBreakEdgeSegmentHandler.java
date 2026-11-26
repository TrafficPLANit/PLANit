package org.goplanit.graph.directed.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;

/**
 * When edge segments are broken, their XML ids stay the same and are no longer unique. The user likely wants to keep
 * the XML ids unique even though the memory model uses internal ids, for example when the network is persisted to disk
 * afterwards and the XML ids are used to map ids.
 *
 * If the XML ids are known to be synced with the internal ids initially, this listener keeps them unique after a break
 * edge segment action, by syncing the XML id of each broken edge segment to its internal id.
 *
 * Supports BreakEdgeSegmentEvent.EVENT_TYPE, syncing upon notification
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
    LOGGER.warning(String.format("%s only supports break edge segment events",
        SyncXmlIdToIdBreakEdgeSegmentHandler.class.getName()));
  }

  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge segment events",
          SyncXmlIdToIdBreakEdgeSegmentHandler.class.getName()));
      return;
    }

    BreakEdgeSegmentEvent breakEdgeSegmentEvent = BreakEdgeSegmentEvent.class.cast(event);
    breakEdgeSegmentEvent.getNewlyBrokenEdgeSegment().setXmlId(
        String.valueOf(breakEdgeSegmentEvent.getNewlyBrokenEdgeSegment().getId()));
  }

}
