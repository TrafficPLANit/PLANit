package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayerFactory;
import org.goplanit.utils.service.routed.RoutedServicesLayers;

import java.util.function.BiConsumer;

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
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public RoutedServicesLayersImpl(final RoutedServicesLayersImpl other, boolean deepCopy, BiConsumer<RoutedServicesLayer, RoutedServicesLayer> mapper) {
    super(other, deepCopy, mapper);
    this.factory = new RoutedServicesLayerFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerFactoryImpl getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl shallowClone() {
    return new RoutedServicesLayersImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl deepClone() {
    return new RoutedServicesLayersImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayersImpl deepCloneWithMapping(BiConsumer<RoutedServicesLayer, RoutedServicesLayer> mapper) {
    return new RoutedServicesLayersImpl(this, true, mapper);
  }

}
