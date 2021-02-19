package org.planit.network;

import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;

/**
 * interface to manage infrastructure layers. Currently we only support macroscopic infrastructure layers so every instance created through this class will return a
 * {@link MacroscopicPhysicalNetwork}. In future versions the user can choose which type is to be created.
 * 
 * @author markr
 *
 */
public interface TopologicalLayers<T extends TopologicalLayer> extends InfrastructureLayers<T> {
  
  /** Number of nodes across all layers
   * 
   * @return number of nodes
   */
  public abstract long getNumberOfNodes();

  /** Number of links across all layers
   * 
   * @return number of links
   */  
  public abstract long getNumberOfLinks();
  
  /** Number of link segments across all layers
   * @return number of link segments
   */  
  public abstract long getNumberOfLinkSegments(); 

}
