package org.goplanit.service.routed;

import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;

import java.util.Collection;

/**
 * Interface to reflect a routed services layer which in turn is to be managed by a container class that implements the RoutedServicesLayers interface. A RoutedServiceLayer
 * contains routed services for a given parent service network layer such as for example - but not limited to - bus routes, train lines etc.
 * 
 * @author markr
 *
 */
public interface RoutedServicesLayer extends ManagedId, ExternalIdAble, Iterable<RoutedModeServices> {

  /** id class for generating ids */
  public static final Class<RoutedServicesLayer> ROUTED_SERVICES_LAYER_ID_CLASS = RoutedServicesLayer.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public default Class<RoutedServicesLayer> getIdClass() {
    return ROUTED_SERVICES_LAYER_ID_CLASS;
  }

  /**
   * invoked by entities inquiring about general information about the layer to display to users
   * 
   * @param prefix optional prefix to include in each line of logging
   */
  public abstract void logInfo(final String prefix);

  /**
   * Check if the layer is empty of any routed services
   *
   * @return true when empty, false otherwise
   */
  public abstract boolean isEmpty();

  /**
   * The parent service layer of this routed services layer
   * 
   * @return parent layer
   */
  public abstract ServiceNetworkLayer getParentLayer();

  /**
   * The services for a given mode available on this layer. If no services are yet available an empty instance is provided
   * 
   * @param mode to obtain services for
   * @return services by mode, empty instance if none have been registered yet
   */
  public abstract RoutedModeServices getServicesByMode(final Mode mode);

  /**
   * {@inheritDoc}
   */
  @Override
  public default void resetChildManagedIdEntities() {
    forEach(routedModeServices -> routedModeServices.reset());
  }

  /** Collect the supported modes that potentially have services registered
   * @return supported modes
   */
  public default Collection<Mode> getSupportedModes(){
    return getParentLayer().getSupportedModes();
  }
}
