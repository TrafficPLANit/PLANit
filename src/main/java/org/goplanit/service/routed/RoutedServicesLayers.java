package org.goplanit.service.routed;

import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.network.layer.NetworkLayer;

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
   * Check if each layer itself is empty
   *
   * @return true when all empty false otherwise
   */
  public default boolean isEachLayerEmpty() {
    boolean eachLayerEmpty = true;
    for (var layer : this) {
      if (!layer.isEmpty()) {
        eachLayerEmpty = false;
        break;
      }
    }
    return eachLayerEmpty;
  }

}
