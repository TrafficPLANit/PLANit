package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.ConjugateVertex;
import org.goplanit.utils.graph.Graph;
import org.goplanit.utils.graph.GraphEntities;
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
   */
  public ConjugateGraphImpl(final ConjugateGraphImpl<V, E> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateGraphImpl<V, E> clone() {
    return new ConjugateGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateGraphImpl<V, E> deepClone() {
    return new ConjugateGraphImpl<>(this, true);
  }

}
