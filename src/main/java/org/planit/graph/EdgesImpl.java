package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeFactory;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.graph.Vertex;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Implementation of Edges interface
 * 
 * @author markr
 */
public class EdgesImpl<E extends Edge> extends LongMapWrapperImpl<E> implements Edges<E> {

  /**
   * The graph builder to create edges
   */
  private final GraphBuilder<?, E> graphBuilder;

  /** factory to create edge instances */
  private final EdgeFactory<E> edgeFactory;

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
  public EdgesImpl(GraphBuilder<?, E> graphBuilder) {
    super(new TreeMap<Long, E>(), E::getId);
    this.graphBuilder = graphBuilder;
    this.edgeFactory = new EdgeFactoryImpl<E>(graphBuilder, this);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the builder for edge implementations
   */
  public EdgesImpl(GraphBuilder<? extends Vertex, E> graphBuilder, EdgeFactory<E> edgeFactory) {
    super(new TreeMap<Long, E>(), E::getId);
    this.graphBuilder = graphBuilder;
    this.edgeFactory = edgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeFactory<? extends E> getFactory() {
    return edgeFactory;
  }

}
