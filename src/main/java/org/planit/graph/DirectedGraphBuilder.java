package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface DirectedGraphBuilder<V extends DirectedVertex, E extends Edge, ES extends EdgeSegment> extends GraphBuilder<V, E> {

  /**
   * Create a new physical link segment instance
   * 
   * @param parentEdge  the parent edge of the edge segment
   * @param directionAB direction of travel
   * @return edgeSegment the created edge segment
   * @throws PlanItException thrown if error
   */
  public ES createEdgeSegment(E parentEdge, boolean directionAB) throws PlanItException;

  /**
   * recreate the ids for all passed in edge segments
   * 
   * @param edge segments to recreate ids for
   */
  public void recreateIds(EdgeSegments<? extends E, ? extends ES> edgeSegments);

}
