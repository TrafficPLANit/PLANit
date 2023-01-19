package org.goplanit.network.layers;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

/**
 * Implementation of container and factory to manager layers. In this network type, all layers are of the Macroscopic physical network type
 * 
 * @author markr
 *
 */
public class MacroscopicNetworkLayersImpl extends UntypedPhysicalNetworkLayersImpl<MacroscopicNetworkLayer> implements MacroscopicNetworkLayers {

  /** factory to use for creating layer instances */
  protected final MacroscopicNetworkLayerFactory factory;

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public MacroscopicNetworkLayersImpl(IdGroupingToken idToken) {
    super(idToken);
    this.factory = new MacroscopicNetworkLayerFactoryImpl(getIdToken(), this);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public MacroscopicNetworkLayersImpl(MacroscopicNetworkLayersImpl other) {
    super(other);
    this.factory = other.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayersImpl clone() {
    return new MacroscopicNetworkLayersImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerFactory getFactory() {
    return factory;
  }

}
