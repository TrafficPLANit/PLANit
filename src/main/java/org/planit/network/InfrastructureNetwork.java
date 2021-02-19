package org.planit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.planit.mode.ModesImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;

/**
 * A network with physical infrastructure layers. One can choose the container for the different layers as a generic type that defines the
 * container level operations available. Each container has a certain base class for the InfrastructureLayer entities which is the second generic type.
 * This allows one to have a base class for each layer, while the layer itself can derive from this base level. This way the user has maximum flexibility regarding
 * what the functinoality and properties of each layer are and how they are exposed via the container
 * 
 * @author markr
 *
 * @param <U> infrastructure layer base class
 * @param <T> infrastructure layer container class where each layer extends \<U\>
 */
public abstract class InfrastructureNetwork<U extends InfrastructureLayer, T extends InfrastructureLayers<U>> extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(InfrastructureNetwork.class.getCanonicalName());

  // Protected

  // Public

  /**
   * class instance containing all modes specific functionality across the layers
   */
  public final Modes modes;

  /** stores the various layers grouped by their supported modes of transport */
  public final T infrastructureLayers;
  
  /**Derived type is to provide the actual layer implementations
   * 
   * @param networkIdToken to use
   * @return infrastructure layers container
   */
  protected abstract T createInfrastructureLayers(IdGroupingToken networkIdToken);

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public InfrastructureNetwork(IdGroupingToken tokenId) {
    super(tokenId);
    
    /* for mode management */
    this.modes = new ModesImpl(tokenId);
    /* for layer management */
    this.infrastructureLayers = createInfrastructureLayers(getNetworkGroupingTokenId());
  }

  /**
   * collect an infrastructure layer by mode (identical to this.infrastructureLayers.get(mode))
   * 
   * @param mode to collect layer for
   * @return corresponding layer, null if not found)
   */
  public U getInfrastructureLayerByMode(Mode mode) {
    return infrastructureLayers.get(mode);
  }

  /**
   * Tries to intialise and create/register infrastructure layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers
   * container. Only possible when the network is still empty and no layers are yet active
   * 
   * @param planitInfrastructureLayerConfiguration to use for configuration
   */
  public void initialiseInfrastructureLayers(InfrastructureLayersConfigurator planitInfrastructureLayerConfiguration) {
    if (!infrastructureLayers.isNoLayers()) {
      LOGGER.warning("unable to initialise infrastructure layers based on provided configuration, since network already has layers defined");
      return;
    }

    /* register layers */
    Map<String, Long> xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : planitInfrastructureLayerConfiguration.infrastructureLayersByXmlId) {
      InfrastructureLayer newLayer = infrastructureLayers.createNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    planitInfrastructureLayerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> infrastructureLayers.get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));

  }


}
