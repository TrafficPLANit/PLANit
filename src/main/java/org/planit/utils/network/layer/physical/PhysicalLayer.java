package org.planit.utils.network.layer.physical;

import org.planit.utils.network.layer.TopologicalLayer;

/**
 * Physical topological Network consisting of nodes, links and link segments.
 *
 * @author markr
 */
public interface PhysicalLayer<N extends Node, L extends Link, LS extends LinkSegment> extends TopologicalLayer {

  // PUBLIC

  /**
   * Collect the links
   * 
   * @return the links
   */
  public abstract Links<L> getLinks();

  /**
   * Collect the link segments
   * 
   * @return the linkSegments
   */
  public abstract LinkSegments<LS> getLinkSegments();

  /**
   * Collect the nodes
   * 
   * @return the nodes
   */
  public abstract Nodes<N> getNodes();

  /**
   * Number of nodes
   * 
   * @return number of nodes
   */
  public abstract long getNumberOfNodes();

  /**
   * Number of links
   * 
   * @return number of links
   */
  public abstract long getNumberOfLinks();

  /**
   * Number of link segments
   * 
   * @return number of link segments
   */
  public abstract long getNumberOfLinkSegments();

}
