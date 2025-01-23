package org.goplanit.network;

import org.goplanit.network.layers.ConjugateMacroscopicNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayers;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetwork;
import org.goplanit.utils.network.virtual.ConjugateVirtualNetworkLayer;

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
   * @param referenceLayers         to use
   @param conjugateVirtualNetwork (optional) when present, integrate conjugate physical network with virtual network
   */
  private void createAndRegisterConjugateLayers(
      MacroscopicNetworkLayers referenceLayers, ConjugateVirtualNetwork conjugateVirtualNetwork) {

    if (!getTransportLayers().isEmpty()) {
      LOGGER.warning("unable to initialise conjugate layers based on reference layers, since conjugate network " +
              "already has layers defined");
      return;
    }
    if(referenceLayers==null || referenceLayers.isEmpty()){
      LOGGER.warning("unable to initialise conjugate layers given reference layers or null or empty");
      return;
    }


    ConjugateVirtualNetworkLayer conjugateVirtualLayer =
        conjugateVirtualNetwork!=null ? conjugateVirtualNetwork.getLayer() : null;
    // create conjugate version and register on conjugate container
    for(var referenceLayer : referenceLayers){
      var conjugateLayer =
          referenceLayer.createAndRegisterConjugate(getTransportLayers().getFactory(), conjugateVirtualLayer);
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
   *
   * @param conjugateVirtualNetwork (optional) when present, integrate conjugate physical network with virtual network
   *                                and do not reset network grouping ids as we base it off continuation of ids used by
   *                                virtual layer
   */
  protected void recreateFromReferenceNetwork(ConjugateVirtualNetwork conjugateVirtualNetwork) {
    createAndRegisterConjugateLayers(referenceNetwork.getTransportLayers(), conjugateVirtualNetwork);
  }

  // Public

  /**
   * Constructor
   *
   * @param referenceNetwork original network the conjugate version will be based on
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  protected ConjugateMacroscopicNetwork(final IdGroupingToken tokenId, MacroscopicNetwork referenceNetwork) {
    this(tokenId, null, referenceNetwork);
  }

  /**
   * Constructor
   *
   * @param referenceNetwork original network the conjugate version will be based on
   * @param networkGroupingToken groupIdToken to use for the network managed ids id generation (if null,
   *                             an auto generated token will be created)
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  protected ConjugateMacroscopicNetwork(
          final IdGroupingToken tokenId, IdGroupingToken networkGroupingToken, MacroscopicNetwork referenceNetwork) {
    super(tokenId, networkGroupingToken, referenceNetwork.getCoordinateReferenceSystem());
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

  /**
   * For each conjugate entity, log the mapping to its original underlying entity where possible
   */
  public void logConjugateToOriginalMapping() {
    getTransportLayers().forEach(ConjugateMacroscopicNetworkLayer::logConjugateToOriginalMapping);
  }
}
