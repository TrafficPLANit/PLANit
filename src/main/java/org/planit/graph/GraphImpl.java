package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
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
public class GraphImpl<V extends Vertex, E extends Edge, ES extends EdgeSegment> {

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
  protected final GraphBuilder<V, E, ES> graphBuilder;

  // PUBLIC

  /**
   * class instance containing all edges
   */
  public final Edges<E> edges;

  /**
   * class instance containing all edge segments
   */
  public final EdgeSegments<ES> edgeSegments;

  /**
   * class instance containing all vertices
   */
  public final Vertices<V> vertices;

  /**
   * Graph Constructor
   *
   * @param groupId        contiguous id generation within this group for instances of this class
   * @param networkBuilder the builder to be used to create this network
   */
  public GraphImpl(final IdGroupingToken groupId, final GraphBuilder<V, E, ES> graphBuilder) {
    this.id = IdGenerator.generateId(groupId, GraphImpl.class);
    this.graphBuilder = graphBuilder;
    this.graphBuilder.setIdGroupingToken(IdGenerator.createIdGroupingToken(this, this.getId()));

    this.edges = new EdgesImpl<E>(graphBuilder);
    this.vertices = new VerticesImpl<V>(graphBuilder);
    this.edgeSegments = new EdgeSegmentsImpl<ES>(graphBuilder);
  }

  // Getters - Setters

  /**
   * collect the id of this graph
   * 
   * @return graph id
   */
  public long getId() {
    return this.id;
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
