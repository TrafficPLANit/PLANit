package org.planit.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;

/**
 * 
 * Vertices implementation using a graphbuilder<V> to create the vertices
 * 
 * @author markr
 *
 * @param <V> concrete class of vertices that are being created
 */
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
   * Constructor
   * 
   * @param graphBuilder the graph builder to use to create vertices
   */
  public VerticesImpl(GraphBuilder<V, ?, ?> graphBuilder) {
    this.graphBuilder = graphBuilder;
    this.vertexMap = new TreeMap<Long, V>();
  }
  
  /**
   * {@inheritDoc}
   */  
  @Override
  public V createNewVertex() {
    return graphBuilder.createVertex();
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public V registerVertex(final V vertex) {
    return vertexMap.put(vertex.getId(), vertex);
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
    final V newVertex = createNewVertex();
    registerVertex(newVertex);
    return newVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V registerNewVertex(Object externalId) {
    final V newVertex = createNewVertex();
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
