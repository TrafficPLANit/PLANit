package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.GraphEntities;
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
   */
  public ConjugateDirectedGraphImpl(final ConjugateDirectedGraphImpl<V, E, ES> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedGraphImpl<V, E, ES> clone() {
    return new ConjugateDirectedGraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedGraphImpl<V, E, ES> deepClone() {
    return new ConjugateDirectedGraphImpl<>(this, true);
  }

}
