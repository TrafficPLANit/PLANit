package org.planit.graph.listener;

import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.id.IdAble;
import org.planit.utils.id.IdAbleImpl;

/**
 * Whenever edges are broken, these edges' XML ids remain the same and are no longer unique. It is likely the user wants to keep the XML ids unique despite using internal ids in
 * the memory model. For example when the network is persisted to disk afterwards in which case the XML ids can be used to map ids. In this situation the XML ids need to remain
 * unique.
 * 
 * If it is known that the XML ids are initially synced with the internal ids, then this listener can be used to sync all broken links' XML id to the internal id of these links
 * ensuring uniqueness after performing a break link action.
 * 
 * Class specifically designed to be used in tandem with breakEdges method on graph modifier.
 * 
 * @author markr
 */
public class SyncEdgeXmlIdsToInternalIdOnBreakEdge extends IdAbleImpl implements BreakEdgeListener {

  /**
   * Default constructor
   */
  public SyncEdgeXmlIdsToInternalIdOnBreakEdge() {
    super(BreakEdgeListener.generateId());
  }

  /**
   * Copy constructor
   * 
   * @param syncEdgeXmlIdsToInternalIdOnBreakEdge to copy
   */
  public SyncEdgeXmlIdsToInternalIdOnBreakEdge(SyncEdgeXmlIdsToInternalIdOnBreakEdge syncEdgeXmlIdsToInternalIdOnBreakEdge) {
    super(syncEdgeXmlIdsToInternalIdOnBreakEdge);
  }

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(SyncEdgeXmlIdsToInternalIdOnBreakEdge.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  public void onBreakEdge(Vertex vertex, Edge aToBreak, Edge breakToB) {
    aToBreak.setXmlId(String.valueOf(aToBreak.getId()));
    breakToB.setXmlId(String.valueOf(breakToB.getId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdAble clone() {
    return new SyncEdgeXmlIdsToInternalIdOnBreakEdge(this);
  }

}
