package org.planit.graph;

import java.util.Set;

import org.planit.utils.graph.Vertex;

/**
 * Modify network elements based on chosen network view.
 * 
 * @author markr
 *
 */
public interface GraphModifier<V extends Vertex> {

  /**
   * remove any dangling sub graphs from the graph if they exist
   * 
   */
  default void removeDanglingSubGraphs() {
    removeDanglingSubGraphs(Integer.MAX_VALUE);
  }

  /**
   * remove any dangling subgraphs below a given size from the graph if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize only remove sub graphs below the given size
   */
  void removeDanglingSubGraphs(Integer belowsize);

  /**
   * remove the subgraph identified by the passed in vertices
   * 
   * @param subNetworkToRemove
   */
  public void removeSubGraph(Set<V> subGraphToRemove);

  /**
   * remove the (sub)graph in which the passed in vertex resides. Apply reordering of internal ids of remaining network.
   * 
   * @param referenceVertex to identify subnetwork by
   */
  public void removeSubGraphOf(V referenceVertex);

}
