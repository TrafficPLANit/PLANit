package org.planit.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;

public class EdgeSegmentsImpl<E extends DirectedEdge, ES extends EdgeSegment> implements EdgeSegments<E, ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsImpl.class.getCanonicalName());

  /**
   * The graph builder to create edgse segments
   */
  protected DirectedGraphBuilder<?, E, ES> graphBuilder;

  /**
   * Map to store edge segments by their Id
   */
  private Map<Long, ES> edgeSegmentMap;

  /**
   * updates the edge segments map keys based on edge segment ids in case an external force has changed already registered edges
   */
  protected void updateIdMapping() {
    /* identify which entries need to be re-registered because of a mismatch */
    Map<Long, ES> updatedMap = new HashMap<Long, ES>(edgeSegmentMap.size());
    edgeSegmentMap.forEach((oldId, edgeSegment) -> updatedMap.put(edgeSegment.getId(), edgeSegment));
    edgeSegmentMap = updatedMap;
  }

  /**
   * Register an edge segment on the network. Use cautiously, if p only register via a factory method to ensure correct id generation within the container
   *
   * @param edgeSegment the link segment to be registered
   */
  public void register(final ES edgeSegment) {
    edgeSegmentMap.put(edgeSegment.getId(), edgeSegment);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the grpahBuilder to use to create edge segments
   */
  public EdgeSegmentsImpl(DirectedGraphBuilder<?, E, ES> graphBuilder) {
    this.graphBuilder = graphBuilder;
    this.edgeSegmentMap = new TreeMap<Long, ES>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(ES edgeSegment) {
    edgeSegmentMap.remove(edgeSegment.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(long edgeSegmentId) {
    edgeSegmentMap.remove(edgeSegmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<ES> iterator() {
    return edgeSegmentMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  public ES create(final E parentEdge, final boolean directionAB) throws PlanItException {
    final ES edgeSegment = graphBuilder.createEdgeSegment(parentEdge, directionAB);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  public void createAndRegister(final E parentEdge, final ES edgeSegment, final boolean directionAB) throws PlanItException {
    parentEdge.registerEdgeSegment(edgeSegment, directionAB);
    register(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  public ES get(final long id) {
    return edgeSegmentMap.get(id);
  }

  /**
   * {@inheritDoc}
   */
  public int size() {
    return edgeSegmentMap.size();
  }

}
