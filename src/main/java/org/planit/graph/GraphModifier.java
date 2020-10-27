package org.planit.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;

/**
 * Modify network elements based on chosen network view.
 * 
 * @author markr
 *
 */
public interface GraphModifier<V extends Vertex, E extends Edge> {

  /**
   * remove any dangling sub graphs from the graph if they exist
   */
  default void removeDanglingSubGraphs() {
    removeDanglingSubGraphs(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * remove any dangling subgraphs below a given size from the graph if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subgraphs below the given size
   * @param aboveSize         remove subgraphs above the given size (typically set to maximum value)
   * @param alwaysKeepLargest indicate if the largest of the subgraphs is always to be kept even if it does not match the criteria
   */
  void removeDanglingSubGraphs(Integer belowsize, Integer aboveSize, boolean alwaysKeepLargest);

  /**
   * remove the subgraph identified by the passed in vertices
   * 
   * @param subNetworkToRemove
   */
  public void removeSubGraph(Set<? extends V> subGraphToRemove);

  /**
   * remove the (sub)graph in which the passed in vertex resides. Apply reordering of internal ids of remaining network.
   * 
   * @param referenceVertex to identify subnetwork by
   */
  public void removeSubGraphOf(V referenceVertex);

  /**
   * Break the passed in edges by inserting the passed in vertex in between. After completion the original edges remain as VertexA->VertexToBreakAt, and new edges are inserted for
   * VertexToBreakAt->VertexB.
   * 
   * @param edgesToBreak    the links to break
   * @param vertexToBreakAt the node to break at
   * @returns affectedEdges the list of all result edges of the breaking of links by their original link id
   * @throws PlanItException thrown if error
   */
  public Map<Long, Set<E>> breakEdgesAt(List<? extends E> edgesToBreak, V vertexToBreakAt) throws PlanItException;

}
