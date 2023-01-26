package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedGraph;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A directed graph implementation consisting of directed vertices, directed edges and edge segments
 * 
 * @author markr
 *
 */
public class DirectedGraphImpl<V extends DirectedVertex, E extends DirectedEdge, ES extends EdgeSegment> extends UntypedDirectedGraphImpl<V, E, ES>
    implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken   contiguous id generation within this group for instances of this class
   * @param vertices     to use
   * @param edges        to use
   * @param edgeSegments to use
   */
  public DirectedGraphImpl(final IdGroupingToken groupToken, GraphEntities<V> vertices, GraphEntities<E> edges, GraphEntities<ES> edgeSegments) {
    super(groupToken, vertices, edges, edgeSegments);
  }

  /**
   * Copy constructor
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public DirectedGraphImpl(final DirectedGraphImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedDirectedGraphImpl<V, E, ES> clone() {
    return new DirectedGraphImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedDirectedGraphImpl<V, E, ES> deepClone() {
    return new DirectedGraphImpl(this, true);
  }

}
