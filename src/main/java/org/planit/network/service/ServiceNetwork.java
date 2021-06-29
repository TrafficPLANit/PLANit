package org.planit.network.service;

import org.planit.network.TopologicalLayerNetwork;
import org.planit.network.layer.service.ServiceNetworkLayer;
import org.planit.utils.id.IdGroupingToken;

/**
 * A service network is a network built on top of a topological (physical) transport network providing services leveraging this underlying network. Each ServiceNetworkLayer in turn
 * relates one-on-one to a (physical) topological layer where it provides services on that layer.
 * 
 * @author markr
 *
 */
public class ServiceNetwork extends TopologicalLayerNetwork<ServiceNetworkLayer, ServiceNetworkLayers> {

  /** generated UID */
  private static final long serialVersionUID = 632938213490189010L;

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public ServiceNetwork(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ServiceNetworkLayers createLayersContainer(IdGroupingToken networkIdToken) {
    return new ServiceNetworkLayers(networkIdToken);
  }

}
