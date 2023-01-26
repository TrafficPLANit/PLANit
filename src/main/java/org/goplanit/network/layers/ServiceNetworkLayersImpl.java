package org.goplanit.network.layers;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.service.ServiceNetworkLayerFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.RoutedServiceLayer;
import org.goplanit.utils.network.layers.ServiceNetworkLayerFactory;
import org.goplanit.utils.network.layers.ServiceNetworkLayers;

/**
 * Implementation of container and factory to manage service network layers. In this network type, all layers are of the ServiceNetworkLayer type
 * 
 * @author markr
 *
 */
public class ServiceNetworkLayersImpl extends TopologicalLayersImpl<RoutedServiceLayer> implements ServiceNetworkLayers {

  /** the parent network */
  private final MacroscopicNetwork parentNetwork;

  /** factory to use for creating layer instances */
  protected final ServiceNetworkLayerFactory factory;

  /**
   * Constructor
   * 
   * @param idToken       for id generation
   * @param parentNetwork the layers are built upon
   */
  public ServiceNetworkLayersImpl(IdGroupingToken idToken, MacroscopicNetwork parentNetwork) {
    super(idToken);
    this.parentNetwork = parentNetwork;
    this.factory = new ServiceNetworkLayerFactoryImpl(idToken, this);
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ServiceNetworkLayersImpl(ServiceNetworkLayersImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.parentNetwork = other.parentNetwork;
    this.factory = new ServiceNetworkLayerFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * The parent network
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayersImpl clone() {
    return new ServiceNetworkLayersImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayersImpl deepClone() {
    return new ServiceNetworkLayersImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayerFactory getFactory() {
    return factory;
  }

}
