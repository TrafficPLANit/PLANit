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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedServicesLayersImpl(final RoutedServicesLayersImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.factory = new RoutedServicesLayerFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl clone() {
    return new RoutedServicesLayersImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl deepClone() {
    return new RoutedServicesLayersImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerFactoryImpl getFactory() {
    return factory;
  }

}
