package org.planit.graph;

import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;

/**
 * A directed subgraph interface for a given parent graph by registering edge segments on it (and therefore vertices and edges)
 * 
 * @author markr
 *
 * @param <V>
 * @param <E>
 * @param <ES>
 */
public interface DirectedSubGraph<V extends DirectedVertex, E extends Edge, ES extends EdgeSegment> {
  
  /**
   * collect the id of this subgraph
   * 
   * @return sub graph id
   */
  public long getId();

  /** the parent graph
   * @return parent graph
   */
  public DirectedGraph<V,E,ES> getParentGraph();
  
  /** collect the root vertex
   * @return root vertex
   */
  public DirectedVertex getRootVertex();
  
  /** register an edge segment on the subgraph
   * 
   * @param edgeSegment
   * @return true when successful, false otherwise (for example when the edge segment is not present on the parent graph)
   */
  public boolean addEdgeSegment(EdgeSegment edgeSegment);
  
}
