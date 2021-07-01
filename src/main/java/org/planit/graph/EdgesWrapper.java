package org.planit.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeFactory;
import org.planit.utils.graph.Edges;

/**
 * Class that wraps and Edges implementation
 * 
 * @author markr
 *
 * @param <E> type of edge
 */
public class EdgesWrapper<E extends Edge> implements Edges<E> {

  /**
   * The edges we are wrapping
   */
  private final Edges<E> edges;

  /**
   * Constructor
   * 
   * @param edges the edges to use to create and register links on
   */
  public EdgesWrapper(final Edges<E> edges) {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public E register(E value) {
    return edges.register(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E get(Long key) {
    return get(key.longValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<E> toCollection() {
    return edges.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<E> copyOfValuesAsSet() {
    return edges.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E findFirst(Predicate<E> valuePredicate) {
    return edges.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeFactory<? extends E> getFactory() {
    return edges.getFactory();
  }
}
