package org.goplanit.graph;

import org.goplanit.utils.graph.*;
import org.goplanit.utils.id.IdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

import java.util.logging.Logger;

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

  /**
   * Copy constructor for shallow copy
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public UntypedGraphImpl(final UntypedGraphImpl<V, E> other, boolean deepCopy) {
    this(other, deepCopy, null, null);
  }

  /**
   * Copy constructor for shallow copy
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param vertexMapper to use for tracking mapping between original and copied vertices
   * @param edgesMapper to use for tracking mapping between original and copied edges
   */
  public UntypedGraphImpl(final UntypedGraphImpl<V, E> other, boolean deepCopy, GraphEntityDeepCopyMapper<V> vertexMapper, GraphEntityDeepCopyMapper<E> edgesMapper) {
    super(other);

    if(deepCopy){
      /* deep copy requires updating of deep copied interdependencies within graph */
      this.vertices = other.getVertices().deepCloneWithMapping(vertexMapper);
      this.edges    = other.getEdges().deepCloneWithMapping(edgesMapper);

      EdgeUtils.updateEdgeVertices(edges, (V originalVertex) -> vertexMapper.getMapping(originalVertex), true);
      VertexUtils.updateVertexEdges(vertices, (E originalEdge) -> edgesMapper.getMapping(originalEdge), true );
    }else{
      this.edges    = other.getEdges().shallowClone();
      this.vertices = other.getVertices(). shallowClone();
    }

    this.groupId  = other.groupId;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedGraph<V, E> shallowClone() {
    return new UntypedGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UntypedGraph<V, E> deepClone() {
    return new UntypedGraphImpl<>(this, true);
  }

  /**
   * A smart deep clone updates known interdependencies between vertices, edges, and edge segments utilising the graph entity deep copy mappers
   *
   * @param vertexMapper tracking original to copy mappings
   * @param edgeMapper tracking original to copy mappings
   * @return created copy
   */
  public UntypedGraphImpl<V, E> smartDeepClone(
      GraphEntityDeepCopyMapper<V> vertexMapper, GraphEntityDeepCopyMapper<E> edgeMapper) {
    return new UntypedGraphImpl<>(this, true, vertexMapper, edgeMapper);
  }

}
