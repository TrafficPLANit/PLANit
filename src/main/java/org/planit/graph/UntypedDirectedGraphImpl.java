package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.UntypedDirectedGraph;
import org.planit.utils.graph.directed.DirectedEdge;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;

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
   * @param graphBuilder the builder to be used to create this network
   */
  public UntypedDirectedGraphImpl(final IdGroupingToken groupToken, GraphEntities<V> vertices, GraphEntities<E> edges, GraphEntities<ES> edgeSegments) {
    super(groupToken, vertices, edges);
    this.edgeSegments = edgeSegments;
  }

  /**
   * Copy constructor
   * 
   * @param directedGraphImpl to copy
   */
  public UntypedDirectedGraphImpl(final UntypedDirectedGraphImpl<V, E, ES> directedGraphImpl) {
    super(directedGraphImpl);
    this.edgeSegments = (GraphEntities<ES>) directedGraphImpl.getEdgeSegments().clone();
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
  public UntypedDirectedGraphImpl<V, E, ES> clone() {
    return new UntypedDirectedGraphImpl<V, E, ES>(this);
  }

}
