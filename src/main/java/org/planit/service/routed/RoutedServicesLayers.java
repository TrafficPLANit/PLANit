package org.planit.service.routed;

import org.planit.utils.id.ManagedIdEntities;

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

  /**
   * Collect the first available layer based on what the iterator would provide as its first entry (not necessarily sorted by key)
   * 
   * @return first available entry, null if layers are empty
   */
  public default RoutedServicesLayer getFirst() {
    if (!isEmpty()) {
      return null;
    }
    return iterator().next();
  }

}
