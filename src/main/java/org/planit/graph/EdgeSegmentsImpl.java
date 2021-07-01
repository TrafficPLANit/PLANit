package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraphBuilder;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegmentFactory;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Implementation of EdgeSegments interface.
 * 
 * 
 * @author markr
 *
 * @param <ES> edge segments type
 */
public class EdgeSegmentsImpl<ES extends EdgeSegment> extends LongMapWrapperImpl<ES> implements EdgeSegments<ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsImpl.class.getCanonicalName());

  /**
   * The graph builder to create edge segments
   */
  protected DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> directedGraphBuilder;

  /** factory to create edge segment instances */
  private final EdgeSegmentFactory<ES> edgeSegmentFactory;

  /**
   * updates the edge segments map keys based on edge segment ids in case an external force has changed already registered edges
   */
  protected void updateIdMapping() {
    /* identify which entries need to be re-registered because of a mismatch */
    Map<Long, ES> updatedMap = new TreeMap<Long, ES>();
    getMap().forEach((oldId, edgeSegment) -> updatedMap.put(edgeSegment.getId(), edgeSegment));
    setMap(updatedMap);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder       the graphBuilder to use to create edge segments
   * @param edgeSegmentFactory to use
   */
  public EdgeSegmentsImpl(DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> graphBuilder) {
    super(new TreeMap<Long, ES>(), ES::getId);
    this.directedGraphBuilder = graphBuilder;
    this.edgeSegmentFactory = new EdgeSegmentFactoryImpl<ES>(graphBuilder, this);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder       the graphBuilder to use to create edge segments
   * @param edgeSegmentFactory to use
   */
  public EdgeSegmentsImpl(DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> graphBuilder, final EdgeSegmentFactory<ES> edgeSegmentFactory) {
    super(new TreeMap<Long, ES>(), ES::getId);
    this.directedGraphBuilder = graphBuilder;
    this.edgeSegmentFactory = edgeSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  public void register(final DirectedEdge parentEdge, final ES edgeSegment, final boolean directionAB) throws PlanItException {
    parentEdge.registerEdgeSegment(edgeSegment, directionAB);
    register(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentFactory<ES> getFactory() {
    return edgeSegmentFactory;
  }

}
