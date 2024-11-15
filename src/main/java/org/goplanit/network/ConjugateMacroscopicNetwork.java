package org.goplanit.network;

import org.goplanit.network.layers.ConjugateMacroscopicNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayers;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

import java.util.logging.Logger;

/**
 * Conjugate macroscopic Network which stores one or more macroscopicconjugate  network infrastructure layers that
 * together form the complete conjugate (intermodal) physical network based on an underlying "regular" macroscopic
 * network
 *
 * @author markr
 *
 */
public class ConjugateMacroscopicNetwork extends
        UntypedPhysicalNetwork<ConjugateMacroscopicNetworkLayer, ConjugateMacroscopicNetworkLayers> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateMacroscopicNetwork.class.getCanonicalName());

  /**
   * Tries to initialise and create/register conjugate layers via reference layers
   *
   * @param referenceLayers to use
   */
  private void createAndRegisterConjugateLayers(MacroscopicNetworkLayers referenceLayers) {
    if (!getTransportLayers().isEmpty()) {
      LOGGER.warning("unable to initialise conjugate layers based on reference layers, since conjugate network " +
              "already has layers defined");
      return;
    }
    if(referenceLayers==null || referenceLayers.isEmpty()){
      LOGGER.warning("unable to initialise conjugate layers given reference layers or null or empty");
      return;
    }

    // create conjugate version and register on conjugate container
    for(var referenceLayer : referenceLayers){
      var conjugateLayer = referenceLayer.createConjugate(getNetworkGroupingTokenId(), null);
      getTransportLayers().register(conjugateLayer);
    }

  }

  // Protected

  /** reference network this conjugate network is based on */
  protected MacroscopicNetwork referenceNetwork;

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConjugateMacroscopicNetworkLayersImpl createLayersContainer(IdGroupingToken networkIdToken) {
    return new ConjugateMacroscopicNetworkLayersImpl(networkIdToken, referenceNetwork.getTransportLayers());
  }

  /**
   * Update/recreate the conjugate network based on current state of the reference network.
   */
  protected void recreateFromReferenceNetwork() {
    var conjugateLayers = getTransportLayers();
    conjugateLayers.reset();
    createAndRegisterConjugateLayers(referenceNetwork.getTransportLayers());
  }

  // Public

  /**
   * Constructor
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  protected ConjugateMacroscopicNetwork(final IdGroupingToken tokenId, MacroscopicNetwork referenceNetwork) {
    super(tokenId);
    this.referenceNetwork = referenceNetwork;
  }

  /**
   * Copy constructor.
   *
   * @param other to clone
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param modeMapper to use for tracking mapping between original and copied modes
   * @param layerMapper to use for tracking mapping between original and copied layers
   */
  protected ConjugateMacroscopicNetwork(
          final ConjugateMacroscopicNetwork other,
          boolean deepCopy,
          ManagedIdDeepCopyMapper<Mode> modeMapper,
          ManagedIdDeepCopyMapper<ConjugateMacroscopicNetworkLayer> layerMapper) {
    super(other, deepCopy, modeMapper, layerMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetwork shallowClone() {
    return new ConjugateMacroscopicNetwork(
            this, false, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetwork deepClone() {
    return new ConjugateMacroscopicNetwork(
            this, true, new ManagedIdDeepCopyMapper<>(), new ManagedIdDeepCopyMapper<>());
  }

}
