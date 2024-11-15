package org.goplanit.network.layers;

import org.goplanit.network.layer.macroscopic.ConjugateMacroscopicNetworkLayerFactoryImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayers;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

import java.util.function.BiConsumer;

/**
 * Implementation of container and factory to manager conjugate layers. In this network type, all layers are of the
 * Macroscopic conjugate physical network layer type
 * 
 * @author markr
 *
 */
public class ConjugateMacroscopicNetworkLayersImpl extends UntypedPhysicalNetworkLayersImpl<ConjugateMacroscopicNetworkLayer> implements ConjugateMacroscopicNetworkLayers {

  /** factory to use for creating conjugate layer instances */
  protected final ConjugateMacroscopicNetworkLayerFactory factory;

  /** reference layers for this container */
  protected final MacroscopicNetworkLayers referenceLayers;

  /**
   * Constructor
   *
   * @param idToken for id generation
   */
  public ConjugateMacroscopicNetworkLayersImpl(IdGroupingToken idToken, MacroscopicNetworkLayers referenceLayers) {
    super(idToken);
    this.factory = new ConjugateMacroscopicNetworkLayerFactoryImpl(getIdToken(), this);
    this.referenceLayers = referenceLayers;
  }

  /**
   * Constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConjugateMacroscopicNetworkLayersImpl(
          ConjugateMacroscopicNetworkLayersImpl other,
          boolean deepCopy,
          BiConsumer<ConjugateMacroscopicNetworkLayer, ConjugateMacroscopicNetworkLayer> mapper) {
    super(other, deepCopy, mapper);
    this.referenceLayers = other.referenceLayers; // not owned
    this.factory =
            new ConjugateMacroscopicNetworkLayerFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayersImpl shallowClone() {
    return new ConjugateMacroscopicNetworkLayersImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayersImpl deepClone() {
    return new ConjugateMacroscopicNetworkLayersImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayersImpl deepCloneWithMapping(
          BiConsumer<ConjugateMacroscopicNetworkLayer, ConjugateMacroscopicNetworkLayer> mapper) {
    return new ConjugateMacroscopicNetworkLayersImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayers getReferenceMacroscopicLayers() {
    return referenceLayers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayerFactory getFactory() {
    return this.factory;
  }

}
