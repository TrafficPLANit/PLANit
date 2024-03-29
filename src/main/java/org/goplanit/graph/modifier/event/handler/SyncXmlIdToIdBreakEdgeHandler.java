package org.goplanit.graph.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Edges;
import org.goplanit.utils.graph.modifier.GraphModifier;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;

/**
 * Whenever edges are broken, these edges' XML ids remain the same and are no longer unique. It is likely the user wants to keep the XML ids unique despite using internal ids in
 * the memory model. For example when the network is persisted to disk afterwards in which case the XML ids can be used to map ids. In this situation the XML ids need to remain
 * unique.
 * 
 * If it is known that the XML ids are initially synced with the internal ids, then this listener can be used to sync all broken links' XML id to the internal id of these links
 * ensuring uniqueness after performing a break link action.
 * 
 * Class supports BreakEdgeEvent.EVENT_TYPE to apply its syncing functionality upon notification
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
      LOGGER.warning(String.format("%s only supports break edge events", SyncXmlIdToIdBreakEdgeHandler.class.getName()));
      return;
    }

    BreakEdgeEvent breakEdgeEvent = BreakEdgeEvent.class.cast(event);
    onBreakEdge(breakEdgeEvent.getEdgeFromVertexAToBreak(), breakEdgeEvent.getEdgeFromBreakToVertexB());
  }

}
