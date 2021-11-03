package org.goplanit.network.layers;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.ServiceNetworkLayerFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layers.ServiceNetworkLayerFactory;
import org.goplanit.utils.network.layers.ServiceNetworkLayers;

/**
 * Implementation of container and factory to manage service network layers. In this network type, all layers are of the ServiceNetworkLayer type
 * 
 * @author markr
 *
 */
public class ServiceNetworkLayersImpl extends TopologicalLayersImpl<ServiceNetworkLayer> implements ServiceNetworkLayers {

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
   * Copy constructor
   * 
   * @param serviceNetworkLayersImpl to copy
   */
  public ServiceNetworkLayersImpl(ServiceNetworkLayersImpl serviceNetworkLayersImpl) {
    super(serviceNetworkLayersImpl);
    this.parentNetwork = serviceNetworkLayersImpl.parentNetwork;
    this.factory = serviceNetworkLayersImpl.factory;
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
    return new ServiceNetworkLayersImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayerFactory getFactory() {
    return factory;
  }

}
