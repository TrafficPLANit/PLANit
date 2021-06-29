package org.planit.service.routed;

import java.io.Serializable;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.network.service.ServiceNetwork;
import org.planit.utils.id.IdGroupingToken;

/**
 * Routed services are based on predefined paths (routes) that are offered as a service of some sort, i.e., it either follows a schedule or a frequency. The most well known routed
 * service would a public transport service, or alternatively scheduled freight movements between warehouses.
 * <p>
 * Each routed service can have one or more trips based on a schedule. The trips follow a service path comprising of legs between predefined stopping points. In PLANit these points
 * are modelled as connectoids. Since connectoids are in turn connected to (transfer) zones, the combination of (travel) demand between zones and routed services allows for a
 * multi-modal trip chain using one or more routed services. This holds for people but potentially also for goods.
 * <p>
 * Routed services rely on routes that exist within a single layer of the network. Therefore any instance of routesServices resides within a network layer rather than on the
 * network as a whole (unless the network onyl has a single layer).
 * <p>
 * Routed services are a top-level input in PLANit and therefore extend TrafficAssignmentComponent such that they can be integrated into any input builder
 * 
 * @author markr
 *
 */
public class RoutedServices extends TrafficAssignmentComponent<RoutedServices> implements Serializable {

  /**
   * generated UID
   */
  private static final long serialVersionUID = -5966641341343291539L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedServices.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param tokenId to use for generation of the id of this routed services instance
   */
  public RoutedServices(IdGroupingToken tokenId) {
    super(tokenId, RoutedServices.class);
  }

  /**
   * Factory method to create a new container for service layers linked to a particular service network
   * 
   * @param parentServiceNetwork the service network the layers are built upon
   * @return created layers container
   */
  public RoutedServiceLayers createServiceLayersContainer(ServiceNetwork parentServiceNetwork) {
    if (parentServiceNetwork == null) {
      LOGGER.warning("Unable to create ServiceLayers container, no parent service network provided");
      return null;
    }
    return new RoutedServiceLayers(parentServiceNetwork);
  }

}
