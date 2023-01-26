package org.goplanit.graph;

import java.util.logging.Logger;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Graph;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * A graph implementation consisting of vertices and edges
 * 
 * @author markr
 *
 */
public class GraphImpl<V extends Vertex, E extends Edge> extends UntypedGraphImpl<V, E> implements Graph<V, E> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(GraphImpl.class.getCanonicalName());

  // Protected

  /**
   * Graph Constructor
   *
   * @param groupId  contiguous id generation within this group for instances of this class
   * @param vertices to use
   * @param edges    to use
   */
  public GraphImpl(final IdGroupingToken groupId, final GraphEntities<V> vertices, final GraphEntities<E> edges) {
    super(groupId, vertices, edges);
  }

  // Getters - Setters

  /**
   * Copy constructor for shallow copy
   * 
   * @param graphImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public GraphImpl(final GraphImpl<V, E> graphImpl, boolean deepCopy) {
    super(graphImpl, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphImpl<V, E> clone() {
    return new GraphImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphImpl<V, E> deepClone() {
    return new GraphImpl<>(this, true);
  }

}
