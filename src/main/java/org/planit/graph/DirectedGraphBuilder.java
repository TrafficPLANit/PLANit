package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;

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
   * Each builder needs a group if token to allow all underlying factory methods to generated ids uniquely tied to the group the entities belong to
   * 
   * @param grouptoken, contiguous id generation within this group for instances created with the factory methods
   */
  public void setIdGroupingToken(IdGroupingToken grouptoken);

  /**
   * Collect the id grouping token used by this builder
   * 
   * @return idGroupingToken the id grouping token used by this builder
   */
  public IdGroupingToken getIdGroupingToken();

}
