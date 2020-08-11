package org.planit.network.physical;

import org.planit.graph.GraphBuilder;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;

/**
 * Build network elements based on chosen network view. Implementations are registered on the network class which uses it to construct network elements
 * 
 * @author markr
 *
 */
public interface PhysicalNetworkBuilder<N extends Node, L extends Link, LS extends LinkSegment> extends GraphBuilder<N, L, LS> {

  /**
   * Create a new mode
   * 
   * @param pcu            pcu value of the mode
   * @param name           name of the mode
   * @param externalModeId external id of the mode
   * @return created mode
   */
  public Mode createMode(long externalModeId, String name, double pcu);

}
