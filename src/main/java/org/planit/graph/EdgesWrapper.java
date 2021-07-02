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
public class EdgesWrapper implements Edges {

  /**
   * The edges we are wrapping
   */
  private final Edges edges;

  /**
   * Constructor
   * 
   * @param edges the edges to use to create and register links on
   */
  public EdgesWrapper(final Edges edges) {
    this.edges = edges;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public EdgesWrapper(final EdgesWrapper other) {
    this.edges = other.edges.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge remove(final Edge edge) {
    return edges.remove(edge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge remove(final long id) {
    return edges.remove(id);
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
  public Iterator<Edge> iterator() {
    return edges.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge register(Edge value) {
    return edges.register(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge get(Long key) {
    return get(key.longValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Edge> toCollection() {
    return edges.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Edge> copyOfValuesAsSet() {
    return edges.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge findFirst(Predicate<Edge> valuePredicate) {
    return edges.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeFactory getFactory() {
    return edges.getFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds() {
    edges.recreateIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgesWrapper clone() {
    return new EdgesWrapper(this);
  }
}
