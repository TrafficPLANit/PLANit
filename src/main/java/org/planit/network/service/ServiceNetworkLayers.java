package org.planit.network.service;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.service.layer.ServiceNetworkLayer;
import org.planit.network.service.layer.ServiceNetworkLayerBuilderImpl;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of container and factory to manager service network layers. In this network type, all layers are of the ServiceNetworkLayer type
 * 
 * @author markr
 *
 */
public class ServiceNetworkLayers extends TopologicalLayersImpl<ServiceNetworkLayer> {

  /**
   * Constructor
   * 
   * @param tokenId for id generation
   */
  public ServiceNetworkLayers(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayer createNew() {
    return new ServiceNetworkLayer(getIdToken(), new ServiceNetworkLayerBuilderImpl(getIdToken()));
  }

}
