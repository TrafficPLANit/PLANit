package org.planit.network.layer;

import java.util.Arrays;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layers.MacroscopicNetworkLayerFactory;
import org.planit.utils.network.layers.MacroscopicNetworkLayers;

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
   */
  public MacroscopicNetworkLayerFactoryImpl(IdGroupingToken groupIdToken, MacroscopicNetworkLayers container) {
    super(groupIdToken);
    this.container = container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetworkLayer registerUniqueCopyOf(ManagedId entityToCopy) {
    MacroscopicNetworkLayer copy = createUniqueCopyOf(entityToCopy);
    container.register(copy);
    return copy;
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
