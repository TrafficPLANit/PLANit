package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * 
 * Vertices implementation using a graphbuilder &lt;V&gt; to create the vertices
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
    register(newVertex);
    return newVertex;
  }

}
