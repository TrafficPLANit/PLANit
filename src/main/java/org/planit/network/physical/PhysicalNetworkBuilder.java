package org.planit.network.physical;

import org.planit.graph.DirectedGraphBuilder;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements.
 * The only difference with the graphBuilder is that we enforce our network elements to be derived from Node, Link, and LinkSegment
 * 
 * @author markr
 *
 */
public interface PhysicalNetworkBuilder<N extends Node, L extends Link, LS extends LinkSegment> extends DirectedGraphBuilder<N, L, LS> {

  /**
   * remove Id gaps for a physical network
   * 
   * @param physicalNetwork to rmeove id gaps from
   */
  @SuppressWarnings("unchecked")
  default void removeIdGaps(PhysicalNetwork<N, L, LS> physicalNetworks) {
    removeIdGaps((DirectedGraph<N,L,LS>)physicalNetworks);
  }
}
