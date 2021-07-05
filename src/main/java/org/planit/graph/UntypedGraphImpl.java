package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.UntypedGraph;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class UntypedGraphImpl<V extends GraphEntities<? extends Vertex>, E extends GraphEntities<? extends Edge>> extends IdAbleImpl implements UntypedGraph<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UntypedGraphImpl.class.getCanonicalName());

  /** the group id token used for this class instance */
  private final IdGroupingToken groupId;

  // Protected

  /**
   * class instance containing all edges
   */
  protected final E edges;

  /**
   * class instance containing all vertices
   */
  protected final V vertices;

  /**
   * Generate a graph id
   * 
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
  public UntypedGraphImpl(final IdGroupingToken groupId, final V vertices, final E edges) {
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
  @SuppressWarnings("unchecked")
  public UntypedGraphImpl(final UntypedGraphImpl<V, E> graphImpl) {
    super(graphImpl);
    this.edges = (E) graphImpl.getEdges().clone();
    this.vertices = (V) graphImpl.getVertices().clone();
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
  public V getVertices() {
    return vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E getEdges() {
    return edges;
  }

}
