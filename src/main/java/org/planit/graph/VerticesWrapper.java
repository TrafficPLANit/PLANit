package org.planit.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.VertexFactory;
import org.planit.utils.graph.Vertices;

/**
 * 
 * Wrapper around Vertices implementation that utilises passed in vertices of the desired generic type to delegate to
 * 
 * @author markr
 *
 * @param <V> concrete class of vertices
 */
public class VerticesWrapper<V extends Vertex> implements Vertices<V> {

  /**
   * The vertices we are wrapping
   */
  private final Vertices<V> vertices;

  /**
   * Constructor
   * 
   * @param vertices the vertices to wrap
   */
  public VerticesWrapper(final Vertices<V> vertices) {
    this.vertices = vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V remove(final V vertex) {
    return vertices.remove(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V register(final V vertex) {
    return vertices.register(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<V> iterator() {
    return vertices.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return vertices.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V get(Long key) {
    return vertices.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return vertices.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<V> toCollection() {
    return vertices.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<V> copyOfValuesAsSet() {
    return vertices.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V findFirst(Predicate<V> valuePredicate) {
    return vertices.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexFactory<? extends V> getFactory() {
    return vertices.getFactory();
  }

}
