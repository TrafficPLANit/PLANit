package org.planit.graph;

import java.util.Set;

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

  /**
   * remove the subnetwork from the graph. It is assumed the graph has been built by this builder. All edges and edge segments connected to the nodes in the subnetwork are removed
   * as well, also all internal ids are updated accordingly to avoid any internal id gaps
   * 
   * @param directedGraph      to remove the subnetwork from
   * @param subNetworkToRemove the subnetwork to remove
   */
  void removeSubNetwork(DirectedGraph<V, E, ES> directedGraph, Set<DirectedVertex> subNetworkToRemove);

}
