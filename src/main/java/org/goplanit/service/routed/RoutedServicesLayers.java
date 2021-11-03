package org.goplanit.service.routed;

import org.goplanit.utils.id.ManagedIdEntities;

/**
 * Interface for wrapper container class around RoutedServiceLayer instances. This container is used to categorise the entires in RoutedServices by their parent network layers.
 * 
 * @author markr
 *
 */
public interface RoutedServicesLayers extends ManagedIdEntities<RoutedServicesLayer> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServicesLayers clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RoutedServicesLayerFactory getFactory();

}
