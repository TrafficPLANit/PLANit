package org.planit.graph;

import java.util.Iterator;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;

/**
 * Class that wraps and Edges implementation
 * 
 * @author markr
 *
 * @param <E> type of edge
 */
public class EdgesWrapper<V extends Vertex, E extends Edge> implements Edges<V, E> {

  /**
   * The edges we are wrapping
   */
  private final Edges<V, E> edges;

  /**
   * Constructor
   * 
   * @param edges the edges to use to create and register links on
   */
  public EdgesWrapper(final Edges<V, E> edges) {
    this.edges = edges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E remove(final E edge) {
    return edges.remove(edge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E remove(final long edgeId) {
    return edges.remove(edgeId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E registerNew(final V vertexA, final V vertexB, boolean registerOnVertices) throws PlanItException {
    return edges.registerNew(vertexA, vertexB, registerOnVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E registerUniqueCopyOf(E edgeToCopy) {
    return edges.registerUniqueCopyOf(edgeToCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E get(long id) {
    return edges.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return edges.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return edges.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<E> iterator() {
    return edges.iterator();
  }
}
