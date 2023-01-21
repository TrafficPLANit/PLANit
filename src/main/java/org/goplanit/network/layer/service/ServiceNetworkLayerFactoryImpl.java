package org.goplanit.network.layer.service;

import java.util.logging.Logger;

import org.goplanit.network.layers.ServiceNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.RoutedServiceLayer;
import org.goplanit.utils.network.layers.ServiceNetworkLayerFactory;

/**
 * Factory for creating service network layer instances
 * 
 * @author markr
 */
public class ServiceNetworkLayerFactoryImpl extends ManagedIdEntityFactoryImpl<RoutedServiceLayer> implements ServiceNetworkLayerFactory {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerFactoryImpl.class.getCanonicalName());

  /** container to register instances on */
  private final ServiceNetworkLayersImpl container;

  /**
   * Constructor
   * 
   * @param groupIdToken to use
   * @param container    to use
   */
  public ServiceNetworkLayerFactoryImpl(IdGroupingToken groupIdToken, ServiceNetworkLayersImpl container) {
    super(groupIdToken);
    this.container = container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServiceLayer registerNew(final MacroscopicNetworkLayer parentLayer) {
    if (!container.getParentNetwork().getTransportLayers().containsKey(parentLayer.getId())) {
      LOGGER.warning("IGNORED, unable to create service layer, provided parent layer not present on parent network");
    }
    ServiceNetworkLayerImpl newLayer = new ServiceNetworkLayerImpl(this.getIdGroupingToken(), parentLayer);
    container.register(newLayer);
    return newLayer;
  }

}
