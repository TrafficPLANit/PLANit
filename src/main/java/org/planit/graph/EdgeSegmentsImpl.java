package org.planit.graph;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraphBuilder;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
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
   * The graph builder to create edgse segments
   */
  protected DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> directedGraphBuilder;

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
   * @param graphBuilder the graphBuilder to use to create edge segments
   */
  public EdgeSegmentsImpl(DirectedGraphBuilder<? extends DirectedVertex, ? extends DirectedEdge, ES> graphBuilder) {
    super(new TreeMap<Long, ES>(), ES::getId);
    this.directedGraphBuilder = graphBuilder;
  }

  /**
   * {@inheritDoc}
   */
  public ES create(final DirectedEdge parentEdge, final boolean directionAB) throws PlanItException {
    final ES edgeSegment = directedGraphBuilder.createEdgeSegment(directionAB);
    edgeSegment.setParentEdge(parentEdge);
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
  public ES registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
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
   * Return an edge segment by its Xml id
   * 
   * Note: not an efficient implementation since it loops over all edge segments in linear time to identify the correct one, preferably use get instead whenever possible.
   * 
   * @param xmlId the XML id of the edge segment
   * @return the specified edge segment instance
   */
  @Override
  public ES getByXmlId(String xmlId) {
    return findFirst(edgeSegment -> xmlId.equals(((ES) edgeSegment).getXmlId()));
  }

  /**
   * Return an edge segment by its external id
   * 
   * Note: not an efficient implementation since it loops over all edge segments in linear time to identify the correct one, preferably use get instead whenever possible.
   * 
   * @param xmlId the XML id of the edge segment
   * @return the specified edge segment instance
   */
  @Override
  public ES getByExternalId(String externalId) {
    return findFirst(edgeSegment -> externalId.equals(((ES) edgeSegment).getExternalId()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES registerUniqueCopyOf(ES edgeSegmentToCopy, DirectedEdge newParentEdge) {
    final ES copy = directedGraphBuilder.createUniqueCopyOf(edgeSegmentToCopy, null /* cannot set new parent edge directly due to generics */);
    copy.setParentEdge(newParentEdge);
    register(copy);
    return copy;
  }

}
