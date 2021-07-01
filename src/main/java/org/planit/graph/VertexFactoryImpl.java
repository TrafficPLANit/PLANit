package org.planit.graph;

import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.VertexFactory;
import org.planit.utils.graph.Vertices;

public class VertexFactoryImpl<V extends Vertex> implements VertexFactory<V> {

  private final GraphBuilder<V, ?> graphBuilder;
  private final Vertices<V> vertices;

  protected VertexFactoryImpl(GraphBuilder<V, ?> graphBuilder, final Vertices<V> vertices) {
    this.graphBuilder = graphBuilder;
    this.vertices = vertices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V createNew() {
    return graphBuilder.createVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V registerNew() {
    final V newVertex = createNew();
    vertices.register(newVertex);
    return newVertex;
  }

}
