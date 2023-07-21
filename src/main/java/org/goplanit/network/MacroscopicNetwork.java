package org.goplanit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.goplanit.network.layer.macroscopic.MacroscopicGridNetworkLayerGenerator;
import org.goplanit.network.layers.MacroscopicNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

/**
 * Macroscopic Network which stores one or more macroscopic network infrastructure layers that together form the complete (intermodal) network.
 *
 * @author markr
 *
 */
public class MacroscopicNetwork extends UntypedPhysicalNetwork<MacroscopicNetworkLayer, MacroscopicNetworkLayers> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetwork.class.getCanonicalName());

  /** Generated UID */
  private static final long serialVersionUID = -4208133694967189790L;

  // Protected

  /**
   * {@inheritDoc}
   */
  @Override
  protected MacroscopicNetworkLayersImpl createLayersContainer(IdGroupingToken networkIdToken) {
    return new MacroscopicNetworkLayersImpl(networkIdToken);
  }

  // Public

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public MacroscopicNetwork(final IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * Copy constructor.
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param modeMapper to use for tracking mapping between original and copied modes
   * @param layerMapper to use for tracking mapping between original and copied layers
   */
  protected MacroscopicNetwork(
      final MacroscopicNetwork other, boolean deepCopy, ManagedIdDeepCopyMapper<Mode> modeMapper, ManagedIdDeepCopyMapper<MacroscopicNetworkLayer> layerMapper) {
    super(other, deepCopy, modeMapper, layerMapper);
  }

  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s XML id %s (external id: %s) has %d layers", prefix, getXmlId(), getExternalId(), getTransportLayers().size()));
    /* for each layer log information regarding contents */
    for(NetworkLayer networkLayer : getTransportLayers()) {
      networkLayer.logInfo(LoggingUtils.networkLayerPrefix(networkLayer.getId()));
    }
  }

  /**
   * Tries to initialise and create/register layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers container. Only
   * possible when the network is still empty and no layers are yet active
   * 
   * @param layerConfiguration to use for configuration
   */
  public void createAndRegisterLayers(MacroscopicNetworkLayerConfigurator layerConfiguration) {
    if (!getTransportLayers().isEmpty()) {
      LOGGER.warning("unable to initialise layers based on provided configuration, since network already has layers defined");
      return;
    }

    /* register layers */
    Map<String, Long> xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : layerConfiguration.transportLayersByXmlId) {
      NetworkLayer newLayer = getTransportLayers().getFactory().registerNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    layerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> getTransportLayers().get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));
  }

  /**
   * Create a macroscopic network instance using the id token provided and in addition generate a simple grid-based network layer for the predefined car mode, where each link is
   * bi-directional and has a single link segment type with access for car (nothing else set). For a more sophisticated grid generator configure the dedicated generator class
   * MacroscopicGridNetworkLayerGenerator by overriding its defaults that are used here.
   * 
   * @param tokenId to use
   * @param rows    in the grid
   * @param columns in the grid
   * @return created grid network
   */
  public static MacroscopicNetwork createSimpleGrid(final IdGroupingToken tokenId, int rows, int columns) {
    var network = new MacroscopicNetwork(tokenId);
    var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    MacroscopicGridNetworkLayerGenerator.create(rows, columns, network.getTransportLayers(), carMode).generate();
    return network;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork shallowClone() {
    return new MacroscopicNetwork(this, false, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork deepClone() {
    return new MacroscopicNetwork(this, true, new ManagedIdDeepCopyMapper<>(), new ManagedIdDeepCopyMapper<>());
  }

}
