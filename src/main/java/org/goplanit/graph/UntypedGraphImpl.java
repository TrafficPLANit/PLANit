package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.UntypedGraph;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class UntypedGraphImpl<V extends Vertex, E extends Edge> extends IdAbleImpl implements UntypedGraph<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedGraphImpl.class.getCanonicalName());

  /** the group id token used for this class instance */
  private final IdGroupingToken groupId;

  // Protected

  /**
   * class instance containing all edges
   */
  protected final GraphEntities<E> edges;

  /**
   * class instance containing all vertices
   */
  protected final GraphEntities<V> vertices;

  /**
   * Generate a graph id
   * 
   * @param groupId to use
   * @return the generated id
   */
  protected static long generatedGraphId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, GRAPH_ID_CLASS);
  }

  /**
   * Graph Constructor
   *
   * @param groupId  contiguous id generation within this group for instances of this class
   * @param vertices to use
   * @param edges    to use
   */
  public UntypedGraphImpl(final IdGroupingToken groupId, final GraphEntities<V> vertices, final GraphEntities<E> edges) {
    super(generatedGraphId(groupId));
    this.groupId = groupId;
    this.edges = edges;
    this.vertices = vertices;
  }

  // Getters - Setters

  /**
   * Copy constructor for shallow copy
   * 
   * @param graphImpl to copy
   */
  public UntypedGraphImpl(final UntypedGraphImpl<V, E> graphImpl) {
    super(graphImpl);
    this.edges = (GraphEntities<E>) graphImpl.getEdges().clone();
    this.vertices = (GraphEntities<V>) graphImpl.getVertices().clone();
    this.groupId = graphImpl.groupId;
  }

  /**
   * Collect the id grouping token used for all entities registered on the graph, i.e., this graph specific identifier for generating ids unique and contiguous within this network
   * and this network only
   * 
   * @return the graph id grouping token
   */
  public IdGroupingToken getGraphIdGroupingToken() {
    return groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generatedGraphId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Class<UntypedGraph> getIdClass() {
    return UntypedGraph.GRAPH_ID_CLASS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedGraph<V, E> clone() {
    return new UntypedGraphImpl<V, E>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<V> getVertices() {
    return vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntities<E> getEdges() {
    return edges;
  }

}
