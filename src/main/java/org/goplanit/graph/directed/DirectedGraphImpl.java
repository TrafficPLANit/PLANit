package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedGraph;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovements;

/**
 * 
 * A directed graph implementation consisting of directed vertices, directed edges and edge segments
 * 
 * @author markr
 * @param <E> type of edge
 * @param <ES> type of edge segment
 * @param <V> type of vertex
 */
public class DirectedGraphImpl<V extends DirectedVertex, E extends DirectedEdge,
    ES extends EdgeSegment> extends UntypedDirectedGraphImpl<V, E, ES> implements DirectedGraph<V, E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * DirectedGraph Constructor
   *
   * @param groupToken   contiguous id generation within this group for instances of this class
   * @param vertices     to use
   * @param edges        to use
   * @param edgeSegments to use
   * @param bannedMovements to use
   */
  public DirectedGraphImpl(
      final IdGroupingToken groupToken,
      GraphEntities<V> vertices,
      GraphEntities<E> edges,
      GraphEntities<ES> edgeSegments,
      final BannedMovements bannedMovements) {
    super(groupToken, vertices, edges, edgeSegments, bannedMovements);
  }

  /**
   * Copy constructor
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public DirectedGraphImpl(final DirectedGraphImpl<V, E, ES> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Copy constructor
   *
   * @param directedGraphImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param vertexMapper tracking how original vertices are mapped to new vertices in case of deep copy
   * @param edgeMapper tracking how original edges are mapped to new edges in case of deep copy
   * @param edgeSegmentMapper tracking how original edge segments are mapped to new edge segments in case of deep copy
   * @param movementMapper to apply in case of deep copy to each original to copy combination
   *                       (when provided, may be null)
   */
  public DirectedGraphImpl(
      final DirectedGraphImpl<V, E, ES> directedGraphImpl,
      boolean deepCopy,
      GraphEntityDeepCopyMapper<V> vertexMapper,
      GraphEntityDeepCopyMapper<E> edgeMapper,
      GraphEntityDeepCopyMapper<ES> edgeSegmentMapper,
      ManagedIdDeepCopyMapper<BannedMovement> movementMapper) {
    super(directedGraphImpl, deepCopy, vertexMapper, edgeMapper, edgeSegmentMapper,movementMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedGraphImpl<V, E, ES> shallowClone() {
    return new DirectedGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   *
   * For directed graphs we also update the internal interdependencies based on available knowledge
   */
  @Override
  public DirectedGraphImpl<V, E, ES> deepClone() {
    return new DirectedGraphImpl<>(
        this,
        true,
        new GraphEntityDeepCopyMapper<>(),
        new GraphEntityDeepCopyMapper<>(),
        new GraphEntityDeepCopyMapper<>(),
        new ManagedIdDeepCopyMapper<>());
  }

}
