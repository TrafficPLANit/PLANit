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
public class VerticesWrapper implements Vertices {

  /**
   * The vertices we are wrapping
   */
  private final Vertices vertices;

  /**
   * Constructor
   * 
   * @param vertices the vertices to wrap
   */
  public VerticesWrapper(final Vertices vertices) {
    this.vertices = vertices;
  }

  /**
   * Copy constructor
   * 
   * @param verticesWrapper to copy
   */
  public VerticesWrapper(final VerticesWrapper verticesWrapper) {
    this.vertices = verticesWrapper.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex remove(final Vertex vertex) {
    return vertices.remove(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex remove(final long key) {
    return vertices.remove(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex register(final Vertex vertex) {
    return vertices.register(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Vertex> iterator() {
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
  public Vertex get(Long key) {
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
  public Collection<Vertex> toCollection() {
    return vertices.toCollection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Vertex> copyOfValuesAsSet() {
    return vertices.copyOfValuesAsSet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Vertex findFirst(Predicate<Vertex> valuePredicate) {
    return vertices.findFirst(valuePredicate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexFactory getFactory() {
    return vertices.getFactory();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds() {
    vertices.recreateIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VerticesWrapper clone() {
    return new VerticesWrapper(this);
  }

}
