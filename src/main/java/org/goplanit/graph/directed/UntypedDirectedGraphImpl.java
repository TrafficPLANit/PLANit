package org.goplanit.graph.directed;

import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.graph.UntypedGraphImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.UntypedDirectedGraph;
import org.goplanit.utils.graph.directed.*;
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
  public UntypedDirectedGraphImpl(
      final UntypedDirectedGraphImpl<V, E, ES> directedGraphImpl,
      boolean deepCopy) {
    this(directedGraphImpl, deepCopy, null, null, null);
  }

  /**
   * Copy constructor
   * 
   * @param directedGraphImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param vertexMapper tracking how orignal vertices are mapped to new vertices in case of deep copy
   * @param edgeMapper tracking how orignal edges are mapped to new edges in case of deep copy
   * @param edgeSegmentMapper tracking how orignal edge segments are mapped to new edge segments in case of deep copy
   */
  public UntypedDirectedGraphImpl(
      final UntypedDirectedGraphImpl<V, E, ES> directedGraphImpl,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<V> vertexMapper,
      GraphEntityDeepCopyMapper<E> edgeMapper,
      GraphEntityDeepCopyMapper<ES> edgeSegmentMapper) {
    super(directedGraphImpl, deepCopy, vertexMapper, edgeMapper);

    // container class, so clone upon shallow copy
    if(deepCopy) {
      this.edgeSegments = directedGraphImpl.getEdgeSegments().deepCloneWithMapping(edgeSegmentMapper);
      EdgeSegmentUtils.updateEdgeSegmentParentEdges(edgeSegments, (E originalEdge) -> edgeMapper.getMapping(originalEdge), true);
      DirectedEdgeUtils.updateDirectedEdgeEdgeSegments(edges, (ES originalEdgeSegment) -> edgeSegmentMapper.getMapping(originalEdgeSegment), true);
    }else{
      this.edgeSegments = directedGraphImpl.getEdgeSegments().shallowClone();
    }
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

  /**
   * A smart deep clone updates known interdependencies between vertices, edges, and edge segments utilising the graph entity deep copy mappers
   *
   * @param vertexMapper tracking original to copy mappings
   * @param edgeMapper tracking original to copy mappings
   * @param edgeSegmentMapper tracking original to copy mappings
   * @return created copy
   */
  public UntypedDirectedGraphImpl<V, E, ES> smartDeepClone(
      GraphEntityDeepCopyMapper<V> vertexMapper, GraphEntityDeepCopyMapper<E> edgeMapper, GraphEntityDeepCopyMapper<ES> edgeSegmentMapper) {
    return new UntypedDirectedGraphImpl<>(this, true, vertexMapper, edgeMapper, edgeSegmentMapper);
  }

}
