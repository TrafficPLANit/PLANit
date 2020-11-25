package org.planit.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;

public class EdgeSegmentsImpl<ES extends EdgeSegment> implements EdgeSegments<ES> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsImpl.class.getCanonicalName());

  /**
   * The graph builder to create edgse segments
   */
  protected DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> directedGraphBuilder;

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
   * Register an edge segment. Use cautiously, Only register via a factory method to ensure correct id generation within the container
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
  public EdgeSegmentsImpl(DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> graphBuilder) {
    this.directedGraphBuilder = graphBuilder;
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
  public ES create(final DirectedEdge parentEdge, final boolean directionAB) throws PlanItException {
    final ES edgeSegment = directedGraphBuilder.createEdgeSegment(parentEdge, directionAB);
    return edgeSegment;
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
  public ES registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnNodeAndLink)
      throws PlanItException {
    ES edgeSegment = create(parentEdge, directionAb);
    register(parentEdge, edgeSegment, directionAb);
    if (registerOnNodeAndLink) {
      parentEdge.registerEdgeSegment(edgeSegment, directionAb);
      if (parentEdge.getVertexA() instanceof DirectedVertex) {
        ((DirectedVertex) parentEdge.getVertexA()).addEdgeSegment(edgeSegment);
        ((DirectedVertex) parentEdge.getVertexB()).addEdgeSegment(edgeSegment);
      }
    }
    return edgeSegment;
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
  public long size() {
    return edgeSegmentMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES registerUniqueCopyOf(ES edgeSegmentToCopy, DirectedEdge newParentEdge) {
    final ES copy = directedGraphBuilder.createUniqueCopyOf(edgeSegmentToCopy, newParentEdge);
    register(copy);
    return copy;
  }



}
