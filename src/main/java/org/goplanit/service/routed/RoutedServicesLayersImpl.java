package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayerFactory;
import org.goplanit.utils.service.routed.RoutedServicesLayers;

/**
 * Implementation of the RoutedServicesLayers interface.
 * 
 * @author markr
 */
public class RoutedServicesLayersImpl extends ManagedIdEntitiesImpl<RoutedServicesLayer> implements RoutedServicesLayers {

  /** factory for this container class */
  protected final RoutedServicesLayerFactoryImpl factory;

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedServicesLayersImpl(final IdGroupingToken tokenId) {
    super(RoutedServicesLayer::getId, RoutedServicesLayer.ROUTED_SERVICES_LAYER_ID_CLASS);
    this.factory = new RoutedServicesLayerFactoryImpl(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedServicesLayersImpl to copy
   */
  public RoutedServicesLayersImpl(final RoutedServicesLayersImpl routedServicesLayersImpl) {
    super(routedServicesLayersImpl);
    this.factory = routedServicesLayersImpl.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl clone() {
    return new RoutedServicesLayersImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerFactoryImpl getFactory() {
    return factory;
  }

}
