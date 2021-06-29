package org.planit.network.physical;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.layer.physical.PhysicalLayerBuilderImpl;
import org.planit.network.layer.physical.PhysicalLayerImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.PhysicalLayer;

/**
 * Implementation of container and factory to manage physical network layers.
 * 
 * @author markr
 *
 */
public class PhysicalNetworkLayers<N extends Node, L extends Link, LS extends LinkSegment> extends TopologicalLayersImpl<PhysicalLayer<N, L, LS>> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public PhysicalNetworkLayers(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * Create and register a new service network layer
   * 
   * @return created ServiceNetworkLayer
   */
  @Override
  public PhysicalLayer createAndRegisterNew() {
    final PhysicalLayer serviceNetworkLayer = new PhysicalLayerImpl(getIdToken(), new PhysicalLayerBuilderImpl(getIdToken()));
    register(serviceNetworkLayer);
    return serviceNetworkLayer;
  }

}
