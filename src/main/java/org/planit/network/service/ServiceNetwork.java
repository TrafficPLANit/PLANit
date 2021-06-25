package org.planit.network.service;

import org.planit.network.TopologicalNetwork;
import org.planit.network.service.layer.ServiceNetworkLayer;
import org.planit.utils.id.IdGroupingToken;

/**
 * A service network is a network built on top of a physical network providing services leveraging this underlying network. Each ServiceNetworkLayer in turn relates one-on-one to a
 * physical infrastructure layer where it provides services on that layer
 * 
 * @author markr
 *
 */
public class ServiceNetwork extends TopologicalNetwork<ServiceNetworkLayer, ServiceNetworkLayers> {

  /** generated UID */
  private static final long serialVersionUID = 632938213490189010L;

  public ServiceNetwork(IdGroupingToken tokenId) {
    super(tokenId);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected ServiceNetworkLayers createInfrastructureLayers(IdGroupingToken networkIdToken) {
    // TODO Auto-generated method stub
    return null;
  }

}
