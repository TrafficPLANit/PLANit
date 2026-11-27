package org.goplanit.graph.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.utils.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Edges;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;

/**
 * When edges are broken, their XML ids stay the same and are no longer unique. The user likely wants to keep the XML
 * ids unique even though the memory model uses internal ids, for example when the network is persisted to disk
 * afterwards and the XML ids are used to map ids.
 *
 * If the XML ids are known to be synced with the internal ids initially, this listener keeps them unique after a break
 * link action, by syncing the XML id of each broken link to its internal id.
 *
 * Supports BreakEdgeEvent.EVENT_TYPE, syncing upon notification
 * 
 * @author markr
 */
public class SyncXmlIdToIdBreakEdgeHandler implements GraphModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdBreakEdgeHandler.class.getCanonicalName());

  /**
   * Perform action by syncing XML ids to ids
   * 
   * @param aToBreak edge running from original a node to break location (new node)
   * @param breakToB aToBreak edge running from break location (new node) to original node b
   */
  protected void onBreakEdge(Edge aToBreak, Edge breakToB) {
    aToBreak.setXmlId(String.valueOf(aToBreak.getId()));
    breakToB.setXmlId(String.valueOf(breakToB.getId()));
  }

  /**
   * Default constructor
   */
  public SyncXmlIdToIdBreakEdgeHandler() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { BreakEdgeEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge events",
          SyncXmlIdToIdBreakEdgeHandler.class.getName()));
      return;
    }

    BreakEdgeEvent breakEdgeEvent = (BreakEdgeEvent) event;
    onBreakEdge(breakEdgeEvent.getEdgeFromVertexAToBreak(), breakEdgeEvent.getEdgeFromBreakToVertexB());
  }

}
