package org.goplanit.graph;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.utils.graph.*;
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

      updateEdgeVertices( originalVertex -> vertexMapper.getMapping(originalVertex), true);
      updateVertexEdges( originalEdge -> edgesMapper.getMapping(originalEdge), true );
    }else{
      this.edges    = other.getEdges().shallowClone();
      this.vertices = other.getVertices(). shallowClone();
    }

    this.groupId  = other.groupId;
  }

  /**
   * Update the edges of all vertices based on the mapping provided (if any)
   * @param edgeToEdgeMapping to use should contain original edge as currently used on vertex and then the value is the new edge to replace it
   * @param removeMissingMappings when true if there is no mapping, the edge is removed as adjacent to the vertex, otherwise they are left in-tact
   */
  public void updateVertexEdges(Function<E,E> edgeToEdgeMapping, boolean removeMissingMappings) {
    for(var vertex : this.vertices){
      var edgeIter = vertex.getEdges().iterator();
      var toBeAdded = new ArrayList<E>(vertex.getEdges().size());
      while (edgeIter.hasNext()) {
        var currEdge = edgeIter.next();
        var newEdge = edgeToEdgeMapping.apply((E) currEdge);
        if (newEdge != null) {
          toBeAdded.add(newEdge);
        }
        if (removeMissingMappings && newEdge == null) {
          edgeIter.remove();
        }
      }
      vertex.addEdges(toBeAdded);
    }
  }

  /**
   * Update the vertices of all edges based on the mapping provided. If no mapping exists, the edge will be assigned a null reference, unless indicated otherwise
   * @param vertexToVertexMapping to use should contain original vertex as currently used on edge and then the value is the new vertex to replace it
   * @param replaceMissingMappings when true missing mappings results in a null assignment, otherwise they are left in-tact
   */
  public void updateEdgeVertices(Function<V,V> vertexToVertexMapping, boolean replaceMissingMappings) {
    this.edges.forEach( edge -> {
      var newVertexA = vertexToVertexMapping.apply((V)edge.getVertexA());
      if(newVertexA!= null || replaceMissingMappings) {
        edge.replace(edge.getVertexA(), newVertexA);
      }
      var newVertexB = vertexToVertexMapping.apply((V)edge.getVertexB());
      if(newVertexB!= null || replaceMissingMappings) {
        edge.replace(edge.getVertexB(), newVertexB);
      }
    });
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
   */
  public UntypedGraphImpl<V, E> smartDeepClone(
      GraphEntityDeepCopyMapper<V> vertexMapper, GraphEntityDeepCopyMapper<E> edgeMapper) {
    return new UntypedGraphImpl<>(this, true, vertexMapper, edgeMapper);
  }

}
