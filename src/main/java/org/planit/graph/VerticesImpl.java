package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.VertexFactory;
import org.planit.utils.graph.Vertices;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * 
 * Vertices implementation container and factory access
 * 
 * @author markr
 *
 * @param <V> concrete class of vertices that are being created
 */
public class VerticesImpl<V extends Vertex> extends LongMapWrapperImpl<V> implements Vertices<V> {

  /**
   * The graph builder to create vertices
   */
  private final GraphBuilder<V, ?> graphBuilder;

  /** factory to create edge instances */
  private final VertexFactory<? extends V> vertexFactory;

  /**
   * updates the vertex map keys based on vertex ids in case an external force has changed already registered vertices
   */
  protected void updateIdMapping() {
    /* identify which entries need to be re-registered because of a mismatch */
    Map<Long, V> updatedMap = new TreeMap<Long, V>();
    getMap().forEach((oldId, vertex) -> updatedMap.put(vertex.getId(), vertex));
    setMap(updatedMap);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the graph builder to use to create vertices
   */
  public VerticesImpl(final GraphBuilder<V, ?> graphBuilder) {
    super(new TreeMap<Long, V>(), V::getId);
    this.graphBuilder = graphBuilder;
    this.vertexFactory = new VertexFactoryImpl<V>(graphBuilder, this);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder  the graph builder to use to create vertices
   * @param vertexFactory to use
   */
  public VerticesImpl(final GraphBuilder<V, ?> graphBuilder, final VertexFactory<? extends V> vertexFactory) {
    super(new TreeMap<Long, V>(), V::getId);
    this.graphBuilder = graphBuilder;
    this.vertexFactory = vertexFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexFactory<? extends V> getFactory() {
    return vertexFactory;
  }
}
