package org.goplanit.service.routed;

import java.io.Serializable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayers;

/**
 * Routed services follow a predefined path (route) on a service network layer and are offered as a service of some
 * sort: they follow either a schedule or a frequency. The best known routed service is a public transport service;
 * another is scheduled freight movements between warehouses.
 * <p>
 * Each routed service can have one or more trips based on a schedule. The trips follow a service path of legs
 * between predefined stopping points, modelled in PLANit as connectoids. Connectoids are connected to (transfer)
 * zones, so (travel) demand between zones combined with routed services allows for a multi-modal trip chain using
 * one or more routed services. This holds for people but potentially also for goods.
 * <p>
 * Routed services rely on routes within a single layer of a (service) network. So any instance of routedServices
 * resides within this layer rather than on the network as a whole.
 * <p>
 * Routed services are a top-level input in PLANit, so they extend TrafficAssignmentComponent and can be integrated
 * into any input builder
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

  /** the layers providing access to the dedicated routed services per layer */
  private final RoutedServicesLayers layers;

  /**
   * Constructor
   * 
   * @param tokenId              to use for generation of the id of this routed services instance
   * @param parentServiceNetwork the parent service network for these routed services
   */
  public RoutedServices(final IdGroupingToken tokenId, final ServiceNetwork parentServiceNetwork) {
    super(tokenId, RoutedServices.class);
    this.parentServiceNetwork = parentServiceNetwork;
    this.layers = new RoutedServicesLayersImpl(tokenId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public RoutedServices(
      final RoutedServices other, boolean deepCopy, BiConsumer<RoutedServicesLayer, RoutedServicesLayer> mapper) {
    super(other, deepCopy);
    this.parentServiceNetwork = other.parentServiceNetwork;

    // container wrappers so require clone always
    this.layers = deepCopy ? other.layers.deepCloneWithMapping(mapper) : other.layers.shallowClone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitComponent<RoutedServices> shallowClone() {
    return new RoutedServices(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitComponent<RoutedServices> deepClone() {
    return new RoutedServices(this, true, new ManagedIdDeepCopyMapper<>());
  }

  /**
   * reset by removing all layers and what is in them
   */
  @Override
  public void reset() {
    layers.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

  /**
   * Collect the parent service network on top of which these services are defined
   * 
   * @return parent service network
   */
  public ServiceNetwork getParentNetwork() {
    return parentServiceNetwork;
  }

  /**
   * Provide access to the layer specific routed services
   * 
   * @return routed services layers container
   */
  public RoutedServicesLayers getLayers() {
    return layers;
  }

  /** Log the stats for the routed services , e.g., the layers and their aggregate contents
   *
   * @param prefix to apply
   * */
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s XML id %s (external id: %s) has %d layers",
        prefix, getXmlId(), getExternalId(), getLayers().size()));
    getLayers().forEach( layer ->
        layer.logInfo(prefix.concat(LoggingUtils.routedServiceLayerPrefix(layer.getId()))));
  }
}
