package org.planit.network.macroscopic;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.layer.macroscopic.MacroscopicPhysicalLayer;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of container and factory to manager layers. In this network type, all layers are of the Macroscopic physical network type
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkLayers extends TopologicalLayersImpl<MacroscopicPhysicalLayer> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public MacroscopicNetworkLayers(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicPhysicalLayer createAndRegisterNew() {
    final MacroscopicPhysicalLayer networkLayer = new MacroscopicPhysicalLayer(this.getIdToken());
    register(networkLayer);
    return networkLayer;
  }

}
