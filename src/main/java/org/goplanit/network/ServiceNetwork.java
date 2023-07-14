package org.goplanit.network;

import org.goplanit.network.layers.ServiceNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layers.ServiceNetworkLayers;

import java.util.logging.Logger;

/**
 * A service network is a network built on top of a topological (physical) transport network providing services leveraging this underlying network. Each ServiceNetworkLayer in turn
 * relates one-on-one to a (physical) topological layer where it provides services on that layer.
 * 
 * @author markr
 *
 */
public class ServiceNetwork extends TopologicalLayerNetwork<ServiceNetworkLayer, ServiceNetworkLayers> {

  /** generated UID */
  private static final long serialVersionUID = 632938213490189010L;

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetwork.class.getCanonicalName());

  /** the parent network and its layers upon which the service layers can built */
  private final MacroscopicNetwork parentNetwork;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ServiceNetworkLayers createLayersContainer(IdGroupingToken networkIdToken) {
    return new ServiceNetworkLayersImpl(networkIdToken, parentNetwork);
  }

  /**
   * Constructor
   * 
   * @param tokenId       to use for id generation
   * @param parentNetwork to use
   */
  public ServiceNetwork(IdGroupingToken tokenId, final MacroscopicNetwork parentNetwork) {
    super(tokenId);
    this.parentNetwork = parentNetwork;
  }

  /**
   * Copy constructor.
   *
   * @param other to copy.
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param layerMapper to track mapping from original to copy
   */
  public ServiceNetwork(final ServiceNetwork other, boolean deepCopy, ManagedIdDeepCopyMapper<ServiceNetworkLayer> layerMapper) {
    super(other, deepCopy, null, layerMapper); // modes are inherited from parent network, so not used, no need for mapping
    this.parentNetwork = other.parentNetwork;
  }

  /**
   * The parent network of the service network
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  }

  /** Log the stats for the service network, e.g., the layers and their aggregate contents
   *
   * @param prefix to apply
   * */
  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("[STATS] %s Service network %s (external id: %s) has %d layers", prefix, getXmlId(), getExternalId(), getTransportLayers().size()));
    getTransportLayers().forEach( layer -> layer.logInfo(prefix.concat(LoggingUtils.serviceNetworkLayerPrefix(layer.getId()))));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetwork shallowClone() {
    return new ServiceNetwork(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetwork deepClone() {
    return new ServiceNetwork(this, true, new ManagedIdDeepCopyMapper<>());
  }

}
