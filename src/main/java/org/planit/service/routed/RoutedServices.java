package org.planit.service.routed;

import java.io.Serializable;
import java.util.logging.Logger;

import org.planit.component.PlanitComponent;
import org.planit.network.ServiceNetwork;
import org.planit.utils.id.IdGroupingToken;

/**
 * Routed services are service that follow a predefined paths (route) on a service network layer that are offered as a service of some sort, i.e., it either follows a schedule or a
 * frequency. The most well known routed service would a public transport service, or alternatively scheduled freight movements between warehouses.
 * <p>
 * Each routed service can have one or more trips based on a schedule. The trips follow a service path comprising of legs between predefined stopping points. In PLANit these points
 * are modelled as connectoids. Since connectoids are in turn connected to (transfer) zones, the combination of (travel) demand between zones and routed services allows for a
 * multi-modal trip chain using one or more routed services. This holds for people but potentially also for goods.
 * <p>
 * Routed services rely on routes that exist within a single layer of a (service) network. Therefore any instance of routesServices resides within this layer rather than on the
 * network as a whole.
 * <p>
 * Routed services are a top-level input in PLANit and therefore extend TrafficAssignmentComponent such that they can be integrated into any input builder
 * 
 * @author markr
 *
 */
public class RoutedServices extends PlanitComponent<RoutedServices> implements Serializable {

  /**
   * generated UID
   */
  private static final long serialVersionUID = -5966641341343291539L;

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(RoutedServices.class.getCanonicalName());

  /** the parent network these routed services are built upon */
  private ServiceNetwork parentServiceNetwork;

  /**
   * Constructor
   * 
   * @param tokenId to use for generation of the id of this routed services instance
   */
  public RoutedServices(IdGroupingToken tokenId, final ServiceNetwork parentServiceNetwork) {
    super(tokenId, RoutedServices.class);
    this.parentServiceNetwork = parentServiceNetwork;
  }

  /**
   * Copy constructor
   * 
   * @param routedServices to copy
   */
  public RoutedServices(RoutedServices routedServices) {
    super(routedServices);
    this.parentServiceNetwork = routedServices.parentServiceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitComponent<RoutedServices> clone() {
    return new RoutedServices(this);
  }

}
