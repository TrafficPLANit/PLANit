package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.wrapper.LongMapWrapper;

/**
 * Implementation of Edges interface
 * 
 * @author markr
 */
public class EdgesImpl<V extends Vertex, E extends Edge> extends LongMapWrapper<E> implements Edges<E> {

  /**
   * The graph builder to create edges
   */
  private final GraphBuilder<V, E> graphBuilder;

  /**
   * updates the edge map keys based on edge ids in case an external force has changed already registered edges
   */
  protected void updateIdMapping() {
    /* identify which entries need to be re-registered because of a mismatch */
    Map<Long, E> updatedMap = new TreeMap<Long, E>();
    getMap().forEach((oldId, edge) -> updatedMap.put(edge.getId(), edge));
    getMap().clear();
    setMap(updatedMap);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the builder for edge implementations
   */
  public EdgesImpl(GraphBuilder<V, E> graphBuilder) {
    super(new TreeMap<Long, E>(), E::getId);
    this.graphBuilder = graphBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <V extends Vertex> E registerNew(V vertexA, V vertexB, boolean registerOnVertices) throws PlanItException {
    final E newEdge = graphBuilder.createEdge(vertexA, vertexB);
    register(newEdge);
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
    register(copy);
    return copy;
  }

}
