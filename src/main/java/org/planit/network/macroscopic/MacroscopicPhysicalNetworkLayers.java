package org.planit.network.macroscopic;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of container and factory to manager layers. In this network type, all layers are of the Macroscopic physical network type
 * 
 * @author markr
 *
 */
public class MacroscopicPhysicalNetworkLayers extends TopologicalLayersImpl<MacroscopicPhysicalNetwork>{

  /** Constructor
   * 
   * @param groupingId for id generation
   */
  public MacroscopicPhysicalNetworkLayers(IdGroupingToken groupingId) {
    super(groupingId);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicPhysicalNetwork createNew() {
    final MacroscopicPhysicalNetwork networkLayer  = new MacroscopicPhysicalNetwork(this.getIdToken());
    register(networkLayer);
    return networkLayer;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public long getNumberOfNodes() {
    long sum = 0;
    for(MacroscopicPhysicalNetwork layer : this) {
      sum += layer.getNumberOfNodes();
    }
    return sum;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public long getNumberOfLinks() {
    long sum = 0;
    for(MacroscopicPhysicalNetwork layer : this) {
      sum += layer.getNumberOfLinks();
    }
    return sum;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public long getNumberOfLinkSegments() {
    long sum = 0;
    for(MacroscopicPhysicalNetwork layer : this) {
      sum += layer.getNumberOfLinkSegments();
    }
    return sum;
  }  

}
