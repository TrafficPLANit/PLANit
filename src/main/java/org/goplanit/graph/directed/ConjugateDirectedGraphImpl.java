package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedGraph;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A conjugate directed graph implementation consisting of conjugate directed vertices, conjugate directed edges and conjugate edge segments
 * 
 * @author markr
 *
 */
public class ConjugateDirectedGraphImpl<V extends ConjugateDirectedVertex, E extends ConjugateDirectedEdge, ES extends ConjugateEdgeSegment>
    extends UntypedDirectedGraphImpl<V, E, ES> implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken            contiguous id generation within this group for instances of this class
   * @param conjugateVertices     to use
   * @param conjugateEdges        to use
   * @param conjugateEdgeSegments to use
   */
  public ConjugateDirectedGraphImpl(final IdGroupingToken groupToken, GraphEntities<V> conjugateVertices, GraphEntities<E> conjugateEdges,
      GraphEntities<ES> conjugateEdgeSegments) {
    super(groupToken, conjugateVertices, conjugateEdges, conjugateEdgeSegments);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param conjVertexMapper tracking how original vertices are mapped to new vertices in case of deep copy
   * @param conjEdgeMapper tracking how original edges are mapped to new edges in case of deep copy
   * @param conjEdgeSegmentMapper tracking how original edge segments are mapped to new edge segments in case of deep copy
   */
  public ConjugateDirectedGraphImpl(
      final ConjugateDirectedGraphImpl<V, E, ES> other, boolean deepCopy, GraphEntityDeepCopyMapper<V> conjVertexMapper, GraphEntityDeepCopyMapper<E> conjEdgeMapper, GraphEntityDeepCopyMapper<ES> conjEdgeSegmentMapper) {
    super(other, deepCopy, conjVertexMapper, conjEdgeMapper, conjEdgeSegmentMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedGraphImpl<V, E, ES> shallowClone() {
    return new ConjugateDirectedGraphImpl<>(this, false, null, null, null);
  }

  /**
   * {@inheritDoc}
   *
   * for conjugate graphs a deep copy also updates known interdependencies between its internal entities
   */
  @Override
  public ConjugateDirectedGraphImpl<V, E, ES> deepClone() {
    return new ConjugateDirectedGraphImpl<>(
        this, true, new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

}
