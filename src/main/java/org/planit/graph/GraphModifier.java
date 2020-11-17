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
   * remove any dangling sub graphs from the graph if they exist and reorder the ids if needed
   * 
   * @throws PlanItException thrown if error
   */
  default void removeDanglingSubGraphs() throws PlanItException {
    boolean alwaysKeepLargest = true;
    removeDanglingSubGraphs(Integer.MAX_VALUE, Integer.MAX_VALUE, alwaysKeepLargest);
  }

  /**
   * remove any dangling subgraphs below a given size from the graph if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subgraphs below the given size
   * @param aboveSize         remove subgraphs above the given size (typically set to maximum value)
   * @param alwaysKeepLargest indicate if the largest of the subgraphs is always to be kept even if it does not match the criteria
   * @throws PlanItException thrown if error
   */
  void removeDanglingSubGraphs(Integer belowsize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException;

  /**
   * remove the subgraph identified by the passed in vertices
   * 
   * @param subNetworkToRemove
   * @param recreateIds        indicate if the ids of the graph entities are to be recreated, if false gaps will occur so it is expected to be handled by the user afterwards in
   *                           this case
   */
  public void removeSubGraph(Set<? extends V> subGraphToRemove, boolean recreateIds);

  /**
   * remove the (sub)graph in which the passed in vertex resides. Apply reordering of internal ids of remaining network.
   * 
   * @param referenceVertex to identify subnetwork by
   * @param recreateIds     indicate if the ids of the graph entities are to be recreated, if false gaps will occur so it is expected to be handled by the user afterwards in this
   *                        case
   * @throws PlanItException thrown if error
   */
  public void removeSubGraphOf(V referenceVertex, boolean recreateIds) throws PlanItException;

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

  /**
   * this method will recreate all ids of the graph's main components, e.g., vertices, edges, and potentially other eligible components of derived graph implementations. Can be
   * used in conjunctions with the removal of subgraphs in case the recreation of ids was switched off manually for some reason.
   */
  public void recreateIds();

}
