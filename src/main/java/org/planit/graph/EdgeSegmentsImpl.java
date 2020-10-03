package org.planit.graph;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;

public class EdgeSegmentsImpl<ES extends EdgeSegment> implements EdgeSegments<ES> {

  /**
   * The graph builder to create edgse segments
   */
  DirectedGraphBuilder<?, ?, ES> graphBuilder;

  /**
   * Map to store edge segments by their Id
   */
  private Map<Long, ES> edgeSegmentMap;

  /**
   * Register a link segment on the network
   *
   * @param edgeSegment the link segment to be registered
   * @throws PlanItException thrown if the current link segment external Id has already been assigned
   */
  protected void registerEdgeSegment(final ES edgeSegment) throws PlanItException {
    edgeSegmentMap.put(edgeSegment.getId(), edgeSegment);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder the grpahBuilder to use to create edge segments
   */
  public EdgeSegmentsImpl(DirectedGraphBuilder<?, ?, ES> graphBuilder) {
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
  public Iterator<ES> iterator() {
    return edgeSegmentMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  public ES create(final Edge parentEdge, final boolean directionAB) throws PlanItException {
    final ES edgeSegment = graphBuilder.createEdgeSegment(parentEdge, directionAB);
    return edgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  public void createAndRegister(final Edge parentEdge, final ES edgeSegment, final boolean directionAB) throws PlanItException {
    parentEdge.registerEdgeSegment(edgeSegment, directionAB);
    registerEdgeSegment(edgeSegment);
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
