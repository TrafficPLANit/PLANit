package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedEdgeFactory;
import org.planit.utils.graph.DirectedEdges;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Implementation of DirectedEdges interface
 * 
 * @author markr
 */
public class DirectedEdgesImpl<E extends DirectedEdge> extends LongMapWrapperImpl<E> implements DirectedEdges<E> {

  /**
   * The graph builder to create edges
   */
  private final GraphBuilder<? extends DirectedVertex, ? extends E> graphBuilder;

  private final DirectedEdgeFactory<? extends E> directedEdgeFactory;

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
  public DirectedEdgesImpl(GraphBuilder<? extends DirectedVertex, ? extends E> graphBuilder, DirectedEdgeFactory<? extends E> directedEdgeFactory) {
    super(new TreeMap<Long, E>(), DirectedEdge::getId);
    this.graphBuilder = graphBuilder;
    this.directedEdgeFactory = directedEdgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeFactory<? extends E> getFactory() {
    return directedEdgeFactory;
  }

}
