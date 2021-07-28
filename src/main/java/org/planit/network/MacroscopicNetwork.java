package org.planit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.planit.network.layers.MacroscopicNetworkLayersImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layers.MacroscopicNetworkLayers;

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
   * Tries to initialise and create/register layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers container. Only
   * possible when the network is still empty and no layers are yet active
   * 
   * @param layerConfiguration to use for configuration
   */
  public void initialiseLayers(MacroscopicNetworkLayerConfigurator layerConfiguration) {
    if (!getTransportLayers().isNoLayers()) {
      LOGGER.warning("unable to initialise layers based on provided configuration, since network already has layers defined");
      return;
    }

    /* register layers */
    Map<String, Long> xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : layerConfiguration.transportLayersByXmlId) {
      TransportLayer newLayer = getTransportLayers().getFactory().registerNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    layerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> getTransportLayers().get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));

  }

}
