package org.planit.network.macroscopic;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.macroscopic.MacroscopicNetworkLayer;
import org.planit.utils.network.physical.PhysicalNetworkLayers;

/**
 * Implementation of container and factory to manager layers. In this network type, all layers are of the Macroscopic physical network type
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkLayers extends TopologicalLayersImpl<MacroscopicNetworkLayer> implements PhysicalNetworkLayers<MacroscopicNetworkLayer> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public MacroscopicNetworkLayers(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public MacroscopicNetworkLayers(MacroscopicNetworkLayers other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer createAndRegisterNew() {
    final MacroscopicNetworkLayerImpl networkLayer = new MacroscopicNetworkLayerImpl(this.getIdToken());
    register(networkLayer);
    return networkLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayers clone() {
    return new MacroscopicNetworkLayers(this);
  }

}
