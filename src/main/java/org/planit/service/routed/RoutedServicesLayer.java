package org.planit.service.routed;

import org.planit.utils.id.ExternalIdAble;
import org.planit.utils.id.ManagedId;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.ServiceNetworkLayer;

/**
 * Interface to reflect a routed services layer which in turn is to be managed by a container class that implements the RoutedServicesLayers interface. A RoutedServiceLayer
 * contains routed services for a given parent service network layer such as for example - but not limited to - bus routes, train lines etc.
 * 
 * @author markr
 *
 */
public interface RoutedServicesLayer extends ManagedId, ExternalIdAble {

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
  public abstract RoutedModeServices getServicesByMode(Mode mode);
}
