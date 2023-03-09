package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.*;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A conjugate graph implementation consisting of conjugate vertices and conjugate edges
 * 
 * @author markr
 *
 */
public class ConjugateGraphImpl<V extends ConjugateVertex, E extends ConjugateEdge> extends UntypedGraphImpl<V, E> implements Graph<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateGraphImpl.class.getCanonicalName());

  // Protected

  /**
   * Graph Constructor
   *
   * @param groupId  contiguous id generation within this group for instances of this class
   * @param conjugateVertices to use
   * @param conjugateEdges    to use
   */
  public ConjugateGraphImpl(final IdGroupingToken groupId, final GraphEntities<V> conjugateVertices, final GraphEntities<E> conjugateEdges) {
    super(groupId, conjugateVertices, conjugateEdges);
  }

  // Getters - Setters

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param conjVertexMapper tracking how original vertices are mapped to new vertices in case of deep copy
   * @param conjEdgeMapper tracking how original edges are mapped to new edges in case of deep copy
   */
  public ConjugateGraphImpl(final ConjugateGraphImpl<V, E> other, boolean deepCopy, GraphEntityDeepCopyMapper<V> conjVertexMapper, GraphEntityDeepCopyMapper<E> conjEdgeMapper) {
    super(other, deepCopy, conjVertexMapper, conjEdgeMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateGraphImpl<V, E> shallowClone() {
    return new ConjugateGraphImpl<>(this, false, null, null);
  }

  /**
   * {@inheritDoc}
   *
   * For conjugate graphs we also update the known interdependencies between its entities
   */
  @Override
  public ConjugateGraphImpl<V, E> deepClone() {
    return new ConjugateGraphImpl<>(this, true, new GraphEntityDeepCopyMapper<>(), new GraphEntityDeepCopyMapper<>());
  }

}
