package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeFactory;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;

public class EdgeFactoryImpl<E extends Edge> implements EdgeFactory<E> {

  private final GraphBuilder<?, E> graphBuilder;
  private final Edges<E> edges;

  protected EdgeFactoryImpl(GraphBuilder<?, E> graphBuilder, Edges<E> edges) {
    this.graphBuilder = graphBuilder;
    this.edges = edges;
  }

  @Override
  public E registerNew(Vertex vertexA, Vertex vertexB, boolean registerOnVertices) throws PlanItException {
    final E newEdge = graphBuilder.createEdge(vertexA, vertexB);
    edges.register(newEdge);
    if (registerOnVertices) {
      vertexA.addEdge(newEdge);
      vertexB.addEdge(newEdge);
    }
    return newEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E registerUniqueCopyOf(E edgeToCopy) {
    final E copy = graphBuilder.createUniqueCopyOf(edgeToCopy);
    edges.register(copy);
    return copy;
  }

}
