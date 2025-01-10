package org.goplanit.network;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.goplanit.network.layer.macroscopic.MacroscopicGridNetworkLayerGenerator;
import org.goplanit.network.layers.MacroscopicNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;

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
    var xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : layerConfiguration.transportLayersByXmlId) {
      NetworkLayer newLayer = getTransportLayers().getFactory().registerNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    layerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> getTransportLayers().get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork shallowClone() {
    return new MacroscopicNetwork(
            this, false, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork deepClone() {
    return new MacroscopicNetwork(
            this, true, new ManagedIdDeepCopyMapper<>(), new ManagedIdDeepCopyMapper<>());
  }

  /**
   * Construct a conjugate macroscopic network based on this network.
   * TODO: currently we only support a single layer version for this by reuing the virtual network's network token for
   * id generation to ensure contiguous numbering of ids across both networks
   *
   * @param token groupIdToken to use for the conjugate network id generation
   * @param conjugateVirtualNetwork (optional) when present, integrate conjugate physical network with virtual network
   *                                and use the virtual networks network id token for the managed layer id generation
   * @return created conjugate Macroscopic network
   */
  public ConjugateMacroscopicNetwork createConjugate(
      IdGroupingToken token, ConjugateVirtualNetwork conjugateVirtualNetwork) {

    // create instance
    ConjugateMacroscopicNetwork conjugateNetwork;
    if(conjugateVirtualNetwork==null) {
      conjugateNetwork = new ConjugateMacroscopicNetwork(token, this);
    }else{
      conjugateNetwork = new ConjugateMacroscopicNetwork(
              token, conjugateVirtualNetwork.getLayer().getLayerIdGroupingToken(), this);
    }
    // create the actual conjugate network
    conjugateNetwork.recreateFromReferenceNetwork(conjugateVirtualNetwork);
    return conjugateNetwork;
  }
}
