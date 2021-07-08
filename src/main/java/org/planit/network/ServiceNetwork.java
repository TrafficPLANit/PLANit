package org.planit.network;

import org.planit.network.layers.ServiceNetworkLayersImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layers.ServiceNetworkLayers;

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

  /** the parent network and its layers upon which the service layers can built */
  private final MacroscopicNetwork parentNetwork;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ServiceNetworkLayers createLayersContainer(IdGroupingToken networkIdToken) {
    return new ServiceNetworkLayersImpl(networkIdToken, parentNetwork);
  }

  /**
   * Constructor
   * 
   * @param tokenId       to use for id generation
   * @param parentNetwork to use
   */
  public ServiceNetwork(IdGroupingToken tokenId, final MacroscopicNetwork parentNetwork) {
    super(tokenId);
    this.parentNetwork = parentNetwork;
  }

  /**
   * The parent network of the service network
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  }

}
