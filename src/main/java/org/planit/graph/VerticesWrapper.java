package org.planit.graph;

import java.util.Iterator;

import org.planit.utils.graph.Vertex;
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
  public void remove(final V vertex) {
    vertices.remove(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(long id) {
    vertices.remove(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V createNew() {
    return vertices.createNew();
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
  public V registerNew() {
    return vertices.registerNew();
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
  public V get(final long id) {
    return vertices.get(id);
  }

}
