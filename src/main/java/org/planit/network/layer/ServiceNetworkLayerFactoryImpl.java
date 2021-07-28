package org.planit.network.layer;

import java.util.logging.Logger;

import org.planit.network.layers.ServiceNetworkLayersImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layers.ServiceNetworkLayerFactory;

/**
 * Factory for creating service network layer instances
 * 
 * @author markr
 */
public class ServiceNetworkLayerFactoryImpl extends ManagedIdEntityFactoryImpl<ServiceNetworkLayer> implements ServiceNetworkLayerFactory {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkLayerFactoryImpl.class.getCanonicalName());

  /** container to register instances on */
  private final ServiceNetworkLayersImpl container;

  /**
   * Constructor
   * 
   * @param groupIdToken to use
   */
  public ServiceNetworkLayerFactoryImpl(IdGroupingToken groupIdToken, ServiceNetworkLayersImpl container) {
    super(groupIdToken);
    this.container = container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayer registerUniqueCopyOf(ManagedId entityToCopy) {
    ServiceNetworkLayer copy = createUniqueCopyOf(entityToCopy);
    container.register(copy);
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkLayer registerNew(final MacroscopicNetworkLayer parentLayer) {
    if (!container.getParentNetwork().getTransportLayers().contains(parentLayer.getId())) {
      LOGGER.warning("IGNORED, unable to create service layer, provided parent layer not present on parent network");
    }
    ServiceNetworkLayerImpl newLayer = new ServiceNetworkLayerImpl(this.getIdGroupingToken(), parentLayer);
    container.register(newLayer);
    return newLayer;
  }

}
