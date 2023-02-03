package org.goplanit.network.layers;

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
   * Constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public MacroscopicNetworkLayersImpl(MacroscopicNetworkLayersImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.factory =
            new MacroscopicNetworkLayerFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayersImpl shallowClone() {
    return new MacroscopicNetworkLayersImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayersImpl deepClone() {
    return new MacroscopicNetworkLayersImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayerFactory getFactory() {
    return factory;
  }

}
