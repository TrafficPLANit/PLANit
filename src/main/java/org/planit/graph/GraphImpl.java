package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Graph;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
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
public class GraphImpl<V extends Vertex, E extends Edge> implements Graph<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(GraphImpl.class.getCanonicalName());

  /**
   * The id of this graph
   */
  private final long id;

  // Protected

  /**
   * Graph builder responsible for constructing all graph related (derived) instances
   */
  protected final GraphBuilder<V, E> graphBuilder;

  // PUBLIC

  /**
   * class instance containing all edges
   */
  protected final Edges<E> edges;

  /**
   * class instance containing all vertices
   */
  protected final Vertices<V> vertices;

  /**
   * Graph Constructor
   *
   * @param groupId      contiguous id generation within this group for instances of this class
   * @param graphBuilder the builder to be used to create this network
   */
  public GraphImpl(final IdGroupingToken groupId, final GraphBuilder<V, E> graphBuilder) {
    this.id = IdGenerator.generateId(groupId, GraphImpl.class);
    this.graphBuilder = graphBuilder;

    this.edges = new EdgesImpl<E>(graphBuilder);
    this.vertices = new VerticesImpl<V>(graphBuilder);
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
   * Collect the id grouping token used for all entities registered on the graph, i.e., this network's specific identifier for generating ids unique and contiguous within this
   * network and this network only
   * 
   * @return the graph id grouping token
   */
  public IdGroupingToken getGraphIdGroupingToken() {
    return this.graphBuilder.getIdGroupingToken();
  }

}
