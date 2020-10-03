package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphBuilder;
import org.planit.utils.id.IdGroupingToken;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface DirectedGraphBuilder<V extends DirectedVertex, E extends Edge, ES extends EdgeSegment> extends GraphBuilder<V,E> {

  /**
   * Create a new physical link segment instance
   * 
   * @param parentEdge  the parent edge of the edge segment
   * @param directionAB direction of travel
   * @return edgeSegment the created edge segment
   * @throws PlanItException thrown if error
   */
  public ES createEdgeSegment(Edge parentEdge, boolean directionAB) throws PlanItException;

  /**
   * Each builder needs a group if token to allow all underlying factory methods to generated ids uniquely tied to the group the entities belong to
   * 
   * @param groupId, contiguous id generation within this group for instances created with the factory methods
   */
  public void setIdGroupingToken(IdGroupingToken groupId);

  /**
   * Collect the id grouping token used by this builder
   * 
   * @return idGroupingToken the id grouping token used by this builder
   */
  public IdGroupingToken getIdGroupingToken();
  
  /**
   * Remove any existing id gaps for the passed in directed graph's entities (edges, vertices, edges segments). With an id gap is meant
   * that there exists an entity with id x, and id x+2 but not x+1. Then x+1 is a gap. This method ensures all ids are contiguous
   * such that the highest id reflects the number of available entities-1 (starting at zero).
   * 
   * @param graph the graph to remove the id gaps from.
   */
  public void removeIdGaps(DirectedGraph<V, E, ES> directedGraph);
  

}
