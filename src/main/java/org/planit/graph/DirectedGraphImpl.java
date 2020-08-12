package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class DirectedGraphImpl<V extends DirectedVertex, E extends Edge, ES extends EdgeSegment> implements DirectedGraph<V,E,ES>{

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  /**
   * The id of this graph
   */
  private final long id;

  // Protected

  /**
   * Graph builder responsible for constructing all graph related (derived) instances
   */
  protected final GraphBuilder<V, E, ES> graphBuilder;

  // PUBLIC

  /**
   * class instance containing all edges
   */
  protected final Edges<E> edges;

  /**
   * class instance containing all edge segments
   */
  protected final EdgeSegments<ES> edgeSegments;

  /**
   * class instance containing all vertices
   */
  protected final Vertices<V> vertices;

  /**
   * Graph Constructor
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public DirectedGraphImpl(final IdGroupingToken groupId, final GraphBuilder<V, E, ES> graphBuilder) {
    this.id = IdGenerator.generateId(groupId, DirectedGraphImpl.class);
    this.graphBuilder = graphBuilder;
    this.graphBuilder.setIdGroupingToken(IdGenerator.createIdGroupingToken(this, this.getId()));

    this.edges = new EdgesImpl<E>(graphBuilder);
    this.vertices = new VerticesImpl<V>(graphBuilder);
    this.edgeSegments = new EdgeSegmentsImpl<ES>(graphBuilder);
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertices<V> getVertices() {
    return vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edges<E> getEdges() {
    return edges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegments<ES> getEdgeSegments() {
    return edgeSegments;
  }
  
  /**
   * Collect the id grouping token used for all entities registered on the graph, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the graph id grouping token
   */
  public IdGroupingToken getGraphIdGroupingToken() {
    return this.graphBuilder.getIdGroupingToken();
  }  

}
