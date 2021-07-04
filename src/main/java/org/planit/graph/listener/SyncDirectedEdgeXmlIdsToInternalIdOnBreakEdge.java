package org.planit.graph.listener;

import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.modifier.BreakEdgeSegmentListener;

/**
 * Whenever directed edges are broken, these edges and their edge segments' xml ids remain the same and are no longer unique. It is likely the user wants to keep the xml ids unique
 * despite using internal ids in the memory model. For example when the network is persisted to disk afterwards in which case the xml ids can be used to map ids. In this situation
 * the xml ids need to remain unique.
 * 
 * If it is known that the XML ids are initially synced with the internal ids, then this listener can be used to sync all broken links' and link segments' xml id to the internal id
 * of these links ensuring uniqueness after performing a break link action.
 * 
 * Class specifically designed to be used in tandem with breakEdges on graph modifier.
 * 
 * @author markr
 */
public class SyncDirectedEdgeXmlIdsToInternalIdOnBreakEdge extends SyncEdgeXmlIdsToInternalIdOnBreakEdge implements BreakEdgeSegmentListener {

  /**
   * Default constructor
   */
  public SyncDirectedEdgeXmlIdsToInternalIdOnBreakEdge() {
    super();
  }

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(SyncDirectedEdgeXmlIdsToInternalIdOnBreakEdge.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  public void onBreakEdgeSegment(DirectedVertex vertex, DirectedEdge brokenEdge, EdgeSegment brokenEdgeSegment) {
    /* syncing of edge already taken care of in super listener, only sync segment here */
    brokenEdgeSegment.setXmlId(String.valueOf(brokenEdgeSegment.getId()));
  }

}
