package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class DirectedGraphImpl<V extends DirectedVertex, E extends DirectedEdge, ES extends EdgeSegment> extends GraphImpl<V, E> implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * class instance containing all edge segments
   */
  protected final EdgeSegments<ES> edgeSegments;

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken   contiguous id generation within this group for instances of this class
   * @param graphBuilder the builder to be used to create this network
   */
  public DirectedGraphImpl(final IdGroupingToken groupToken, final DirectedGraphBuilder<V, E, ES> graphBuilder) {
    super(groupToken, graphBuilder);
    this.edgeSegments = new EdgeSegmentsImpl<ES>(graphBuilder);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegments<ES> getEdgeSegments() {
    return edgeSegments;
  }

}
