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
   * Copy constructor for shallow copy
   * 
   * @param graphImpl to copy
   */
  public ConjugateGraphImpl(final ConjugateGraphImpl<V, E> graphImpl) {
    super(graphImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateGraphImpl<V, E> clone() {
    return new ConjugateGraphImpl<V, E>(this);
  }

}
