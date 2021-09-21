package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.network.layer.ServiceNetworkLayer;

/**
 * Factory for creating routed services layer instances (on container)
 * 
 * @author markr
 */
public class RoutedServicesLayerFactory extends ManagedIdEntityFactoryImpl<RoutedServicesLayer> {

  /** container to use */
  protected final RoutedServicesLayers routedServicesLayers;

  /**
   * Create a newly created instance without registering on the container
   * 
   * @param parentLayer the parent layer these routed services are built upon
   * @return created routed services layer
   */
  protected RoutedServicesLayer createNew(final ServiceNetworkLayer parentLayer) {
    return new RoutedServicesLayerImpl(getIdGroupingToken(), parentLayer);
  }

  /**
   * Constructor
   * 
   * @param tokenId              to use
   * @param routedServicesLayers to use
   */
  protected RoutedServicesLayerFactory(final IdGroupingToken tokenId, final RoutedServicesLayers routedServicesLayers) {
    super(tokenId);
    this.routedServicesLayers = routedServicesLayers;
  }

  /**
   * Register a newly created instance on the underlying container
   * 
   * @param parentLayer the parent layer these routed services are built upon
   * @return created instance
   */
  public RoutedServicesLayer registerNew(final ServiceNetworkLayer parentLayer) {
    RoutedServicesLayer newRoutedServicesLayer = createNew(parentLayer);
    routedServicesLayers.register(newRoutedServicesLayer);
    return newRoutedServicesLayer;
  }

}
