package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;

/**
 * Implementation of the RoutedServicesLayers interface.
 * 
 * @author markr
 */
public class RoutedServicesLayersImpl extends ManagedIdEntitiesImpl<RoutedServicesLayer> implements RoutedServicesLayers {

  /** factory for this container class */
  protected final RoutedServicesLayerFactory factory;

  /**
   * Constructor
   */
  protected RoutedServicesLayersImpl(final IdGroupingToken tokenId) {
    super(RoutedServicesLayer::getId, RoutedServicesLayer.ROUTED_SERVICES_LAYER_ID_CLASS);
    this.factory = new RoutedServicesLayerFactory(tokenId, this);
  }

  /**
   * Copy constructor
   * 
   * @param routedServicesLayersImpl to copy
   */
  public RoutedServicesLayersImpl(RoutedServicesLayersImpl routedServicesLayersImpl) {
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
  public RoutedServicesLayerFactory getFactory() {
    return factory;
  }

}
