package org.planit.network.service;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.layer.service.ServiceNetworkLayer;
import org.planit.network.layer.service.ServiceNetworkLayerBuilderImpl;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of container and factory to manage service network layers. In this network type, all layers are of the ServiceNetworkLayer type
 * 
 * @author markr
 *
 */
public class ServiceNetworkLayers extends TopologicalLayersImpl<ServiceNetworkLayer> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public ServiceNetworkLayers(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * Create and register a new service network layer
   * 
   * @return created ServiceNetworkLayer
   */
  @Override
  public ServiceNetworkLayer createAndRegisterNew() {
    final ServiceNetworkLayer serviceNetworkLayer = new ServiceNetworkLayer(getIdToken(), new ServiceNetworkLayerBuilderImpl(getIdToken()));
    register(serviceNetworkLayer);
    return serviceNetworkLayer;
  }

}
