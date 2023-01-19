package org.goplanit.network.layer.macroscopic;

import java.util.Arrays;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

/**
 * Factory for creating macroscopic network layer instances
 * 
 * @author markr
 */
public class MacroscopicNetworkLayerFactoryImpl extends ManagedIdEntityFactoryImpl<MacroscopicNetworkLayer> implements MacroscopicNetworkLayerFactory {

  /** container to register instances on */
  private final MacroscopicNetworkLayers container;

  /**
   * Constructor
   *
   * @param groupIdToken to use
   * @param container    to use for network layers
   */
  public MacroscopicNetworkLayerFactoryImpl(IdGroupingToken groupIdToken, MacroscopicNetworkLayers container) {
    super(groupIdToken);
    this.container = container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer registerNew() {
    MacroscopicNetworkLayer newLayer = new MacroscopicNetworkLayerImpl(this.getIdGroupingToken());
    container.register(newLayer);
    return newLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer registerNew(Mode... supportedModes) {
    MacroscopicNetworkLayer newLayer = registerNew();
    newLayer.registerSupportedModes(Arrays.asList(supportedModes));
    return newLayer;
  }

}
