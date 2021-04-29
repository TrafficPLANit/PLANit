package org.planit.graph.listener;

import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.id.IdAbleImpl;

/**
 * Whenever edges are broken, these edges' xml ids remain the same and are no longer unique. It is likely the user wants to keep the xml ids unique despite using internal ids in the memory model.
 * For example when the network is persisted to disk afterwards in which case the xml ids can be used to map ids. In this situation the xml ids need to remian unique.
 * 
 * If it is known that the XML ids are initially synced with the internal ids, then this listener can be used to sync all broken links' xml id to the internal id of these links ensuring uniqueness
 * after performing a break link action.
 * 
 * Class specifically designed to be used in tandem with {@link breakEdges}. 
 * 
 * @author markr
 *
 * @param <V> type of vertex
 * @param <E> type of edge
 */
public class SyncEdgeXmlIdsToInternalIdOnBreakEdge<V extends Vertex, E extends Edge> extends IdAbleImpl implements BreakEdgeListener<V, E> {

  /**
   * Default constructor
   */
  public SyncEdgeXmlIdsToInternalIdOnBreakEdge() {
    super(BreakEdgeListener.generateId());
  }

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(SyncEdgeXmlIdsToInternalIdOnBreakEdge.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  public void onBreakEdge(V vertex, E aToBreak, E breakToB) {
    aToBreak.setXmlId(String.valueOf(aToBreak.getId()));
    breakToB.setXmlId(String.valueOf(breakToB.getId()));
  }



}
