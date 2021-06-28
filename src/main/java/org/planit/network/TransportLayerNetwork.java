package org.planit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.planit.mode.ModesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;

/**
 * A transport network with one or more layers. One can choose the container for the different layers as a generic type that defines the container level operations available. Each
 * container has a certain base class for the TransportLayer entities which is the second generic type. This allows one to have a base class for each layer, while the layer itself
 * can derive from this base level. This way the user has maximum flexibility regarding what the functionality and properties of each layer are and how they are exposed via the
 * container
 * 
 * @author markr
 *
 * @param <U> transport layer base class
 * @param <T> transport layer container class where each layer extends {@code <U>}
 */
public abstract class TransportLayerNetwork<U extends TransportLayer, T extends TransportLayers<U>> extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(TransportLayerNetwork.class.getCanonicalName());

  // Protected

  // Public

  /**
   * class instance containing all modes specific functionality across the layers
   */
  public final Modes modes;

  /** stores the various layers grouped by their supported modes of transport */
  public final T transportLayers;

  /**
   * Derived type is to provide the actual layer implementations
   * 
   * @param networkIdToken to use
   * @return infrastructure layers container
   */
  protected abstract T createLayersContainer(IdGroupingToken networkIdToken);

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public TransportLayerNetwork(IdGroupingToken tokenId) {
    super(tokenId);

    /* for mode management */
    this.modes = new ModesImpl(tokenId);
    /* for layer management */
    this.transportLayers = createLayersContainer(getNetworkGroupingTokenId());
  }

  /**
   * collect a layer by mode
   * 
   * @param mode to collect layer for
   * @return corresponding layer, (null if not found)
   */
  public U getLayerByMode(Mode mode) {
    return transportLayers.get(mode);
  }

  /**
   * Tries to initialise and create/register layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers container. Only
   * possible when the network is still empty and no layers are yet active
   * 
   * @param layerConfiguration to use for configuration
   */
  public void initialiseLayers(TransportLayersConfigurator layerConfiguration) {
    if (!transportLayers.isNoLayers()) {
      LOGGER.warning("unable to initialise layers based on provided configuration, since network already has layers defined");
      return;
    }

    /* register layers */
    Map<String, Long> xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : layerConfiguration.transportLayersByXmlId) {
      TransportLayer newLayer = transportLayers.createNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    layerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> transportLayers.get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));

  }

}
