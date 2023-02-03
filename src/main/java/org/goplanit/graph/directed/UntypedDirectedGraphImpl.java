package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.UntypedGraphImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A directed graph implementation consisting of directed vertices and directed edges
 * 
 * @author markr
 *
 */
public class UntypedDirectedGraphImpl<V extends DirectedVertex, E extends DirectedEdge, ES extends EdgeSegment> extends UntypedGraphImpl<V, E>
    implements UntypedDirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedDirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * class instance containing all edge segments
   */
  protected final GraphEntities<ES> edgeSegments;

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken   contiguous id generation within this group for instances of this class
   * @param vertices     to use
   * @param edges        to use
   * @param edgeSegments to use
   */
  public UntypedDirectedGraphImpl(final IdGroupingToken groupToken, GraphEntities<V> vertices, GraphEntities<E> edges, GraphEntities<ES> edgeSegments) {
    super(groupToken, vertices, edges);
    this.edgeSegments = edgeSegments;
  }

  /**
   * Copy constructor
   * 
   * @param directedGraphImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public UntypedDirectedGraphImpl(final UntypedDirectedGraphImpl<V, E, ES> directedGraphImpl, boolean deepCopy) {
    super(directedGraphImpl, deepCopy);

    // container class, so clone upon shallow copy
    this.edgeSegments =
            deepCopy ? directedGraphImpl.getEdgeSegments().deepClone() : directedGraphImpl.getEdgeSegments().shallowClone();
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<ES> getEdgeSegments() {
    return edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedDirectedGraphImpl<V, E, ES> shallowClone() {
    return new UntypedDirectedGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedDirectedGraphImpl<V, E, ES> deepClone() {
    return new UntypedDirectedGraphImpl<>(this, true);
  }

}
