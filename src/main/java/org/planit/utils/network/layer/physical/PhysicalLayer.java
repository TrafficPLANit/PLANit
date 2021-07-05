package org.planit.utils.network.layer.physical;

import org.planit.utils.network.layer.TopologicalLayer;

/**
 * Physical topological Network consisting of nodes, links and link segments.
 *
 * @author markr
 */
public interface PhysicalLayer extends TopologicalLayer {

  // PUBLIC

  /**
   * Collect the links
   * 
   * @return the links
   */
  public abstract Links getLinks();

  /**
   * Collect the link segments
   * 
   * @return the linkSegments
   */
  public abstract LinkSegments getLinkSegments();

  /**
   * Collect the nodes
   * 
   * @return the nodes
   */
  public abstract Nodes getNodes();

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
