package org.planit.service.routed;

import java.util.TreeMap;

import org.planit.network.layer.service.ServiceNetworkLayer;
import org.planit.network.service.ServiceNetwork;
import org.planit.service.routed.layer.RoutedServiceLayer;
import org.planit.utils.wrapper.LongMapWrapper;

/**
 * Implementation of container and factory to manage routed service layers. Each service layer is directly related to a single ServiceNetworkLayer on top of which the services in
 * this layer are built.Each service layer can support services for one or more modes supported by the service network layer it references
 * 
 * @author markr
 *
 */
public class RoutedServiceLayers extends LongMapWrapper<RoutedServiceLayer> {

  /** the parent service network the underlying layers build upon */
  protected final ServiceNetwork parentServiceNetwork;

  /**
   * Constructor
   * 
   * @param parentServiceNetwork to build the underlying layers upon
   */
  protected RoutedServiceLayers(ServiceNetwork parentServiceNetwork) {
    super(new TreeMap<Long, RoutedServiceLayer>(), RoutedServiceLayer::getId);
    this.parentServiceNetwork = parentServiceNetwork;
  }

  /**
   * Create a new routed service layer instance based on the provided parent network layer
   * 
   * @param parentServiceNetworkLayer to use
   * @return created routed service layer
   */
  public RoutedServiceLayer createNew(ServiceNetworkLayer parentNetworkLayer) {
    return new RoutedServiceLayer(getIdToken(), parentNetworkLayer);
  }

  /**
   * Create a new routed service layer instance based on the provided parent network layer
   * 
   * @param parentServiceNetworkLayer to use
   * @return created routed service layer
   */
  public RoutedServiceLayer createAndRegisterNew(ServiceNetworkLayer parentNetworkLayer) {
    RoutedServiceLayer routedServiceLayer = new RoutedServiceLayer(getIdToken(), parentNetworkLayer);
    register(routedServiceLayer);
    return routedServiceLayer;
  }

}
