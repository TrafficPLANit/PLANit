package org.goplanit.network.layer.macroscopic;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ConjugateMacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.ConjugateMacroscopicNetworkLayers;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayerFactory;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;

import java.util.Arrays;

/**
 * Factory for creating macroscopic conjugate network layer instances
 * 
 * @author markr
 */
public class ConjugateMacroscopicNetworkLayerFactoryImpl extends ManagedIdEntityFactoryImpl<ConjugateMacroscopicNetworkLayer> implements ConjugateMacroscopicNetworkLayerFactory {

  /** container to register instances on */
  private final ConjugateMacroscopicNetworkLayers container;

  /**
   * Constructor
   *
   * @param groupIdToken to use
   * @param container    to use for conjugate network layers
   */
  public ConjugateMacroscopicNetworkLayerFactoryImpl(
          IdGroupingToken groupIdToken, ConjugateMacroscopicNetworkLayers container) {
    super(groupIdToken);
    this.container = container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateMacroscopicNetworkLayer registerNew(MacroscopicNetworkLayer referenceLayer) {
    ConjugateMacroscopicNetworkLayer newLayer =
            new ConjugateMacroscopicNetworkLayerImpl(this.getIdGroupingToken(), referenceLayer);
    container.register(newLayer);
    return newLayer;
  }

}
