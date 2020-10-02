package org.planit.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;

/**
 * Implementation of Edges interface
 * 
 * @author markr
 */
public class EdgesImpl<E extends Edge> implements Edges<E> {

  /**
   * The graph builder to create edges
   */
  private final GraphBuilder<?, E> graphBuilder;

  /**
   * Map to store edges by their Id
   */
  private final Map<Long, E> edgeMap;

  /**
   * Constructor
   * 
   * @param graphBuilder the builder for edge implementations
   */
  public EdgesImpl(GraphBuilder<?, E> graphBuilder) {
    this.graphBuilder = graphBuilder;
    this.edgeMap = new TreeMap<Long, E>();
  }
  
  /**
   * Add edge to the internal container. Do not use this unless you know what you are doing because it can mess up the contiguous internal id
   * structure of the edges. PReferred method is to only use registerNew.
   *
   * @param edge edge to be registered in this network based on its internal id
   * @return edge, in case it overrides an existing edge, the removed edge is returned
   */
  public E register(final E edge) {
    return edgeMap.put(edge.getId(), edge);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(E edge) {
    edgeMap.remove(edge.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<E> iterator() {
    return edgeMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E registerNew(final Vertex vertexA, final Vertex vertexB, final double length) throws PlanItException {
    final E newEdge = graphBuilder.createEdge(vertexA, vertexB, length);
    register(newEdge);
    return newEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public E get(final long id) {
    return edgeMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return edgeMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return size()==0;
  }

}
