package org.planit.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;

public class VerticesImpl<V extends Vertex> implements Vertices<V> {

  /**
   * The graph builder to create vertices
   */
  private final GraphBuilder<V, ?, ?> graphBuilder;

  /**
   * Map to store nodes by their Id
   */
  private Map<Long, V> vertexMap;

  /**
   * Add node to the internal container
   *
   * @param vertex vertex to be registered in this network
   * @return vertex, in case it overrides an existing vertex, the removed vertex is returned
   */
  protected V registerVertex(final V vertex) {
    return vertexMap.put(vertex.getId(), vertex);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the graphbuilder to use to create vertices
   */
  public VerticesImpl(GraphBuilder<V, ?, ?> graphBuilder) {
    this.graphBuilder = graphBuilder;
    this.vertexMap = new TreeMap<Long, V>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<V> iterator() {
    return vertexMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V registerNewVertex() {
    final V newVertex = graphBuilder.createVertex();
    registerVertex(newVertex);
    return newVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V registerNewVertex(Object externalId) {
    final V newVertex = graphBuilder.createVertex();
    newVertex.setExternalId(externalId);
    registerVertex(newVertex);
    return newVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfVertices() {
    return vertexMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V getVertexById(final long id) {
    return vertexMap.get(id);
  }

}
