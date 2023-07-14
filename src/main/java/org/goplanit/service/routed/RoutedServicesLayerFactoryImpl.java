package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayerFactory;
import org.goplanit.utils.service.routed.RoutedServicesLayers;

/**
 * Factory for creating routed services layer instances (on container)
 * 
 * @author markr
 */
public class RoutedServicesLayerFactoryImpl extends ManagedIdEntityFactoryImpl<RoutedServicesLayer> implements RoutedServicesLayerFactory {

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
  protected RoutedServicesLayerFactoryImpl(final IdGroupingToken tokenId, final RoutedServicesLayers routedServicesLayers) {
    super(tokenId);
    this.routedServicesLayers = routedServicesLayers;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayer registerNew(final ServiceNetworkLayer parentLayer) {
    RoutedServicesLayer newRoutedServicesLayer = createNew(parentLayer);
    routedServicesLayers.register(newRoutedServicesLayer);
    return newRoutedServicesLayer;
  }

}
