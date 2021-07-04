package org.planit.graph.directed;

import java.util.logging.Logger;

import org.planit.graph.UntypedDirectedGraphImpl;
import org.planit.utils.graph.directed.DirectedEdges;
import org.planit.utils.graph.directed.DirectedGraph;
import org.planit.utils.graph.directed.DirectedVertices;
import org.planit.utils.graph.directed.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A directed graph implementation consisting of directed vertices, directed edges and edge segments
 * 
 * @author markr
 *
 */
public class DirectedGraphImpl<V extends DirectedVertices, E extends DirectedEdges, ES extends EdgeSegments> extends UntypedDirectedGraphImpl<V, E, ES>
    implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken   contiguous id generation within this group for instances of this class
   * @param graphBuilder the builder to be used to create this network
   */
  public DirectedGraphImpl(final IdGroupingToken groupToken, V vertices, E edges, ES edgeSegments) {
    super(groupToken, vertices, edges, edgeSegments);
  }

  /**
   * Copy constructor
   * 
   * @param directedGraphImpl to copy
   */
  public DirectedGraphImpl(final DirectedGraphImpl<V, E, ES> directedGraphImpl) {
    super(directedGraphImpl);
  }

}
