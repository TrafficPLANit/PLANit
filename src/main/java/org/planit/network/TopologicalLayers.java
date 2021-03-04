package org.planit.network;

import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.network.physical.Link;

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
  
  
  /** Collect the layer on which this link is registered, if no layer can be found, null is returned
   * @param link to find layer for
   * @return found layer, null if no match found
   */
  public abstract T get(Link link);

}
