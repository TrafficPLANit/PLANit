package org.planit.network.physical;

import org.planit.network.TopologicalLayersImpl;
import org.planit.network.layer.physical.LinkSegmentsImpl;
import org.planit.network.layer.physical.LinksImpl;
import org.planit.network.layer.physical.NodesImpl;
import org.planit.network.layer.physical.PhysicalLayerImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.PhysicalLayer;

/**
 * Implementation of container and factory to manage physical network layers.
 * 
 * @author markr
 *
 */
public class PhysicalNetworkLayers extends TopologicalLayersImpl<PhysicalLayer> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public PhysicalNetworkLayers(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public PhysicalNetworkLayers(PhysicalNetworkLayers other) {
    super(other);
  }

  /**
   * Create and register a new service network layer
   * 
   * @return created ServiceNetworkLayer
   */
  @Override
  public PhysicalLayer createAndRegisterNew() {
    final PhysicalLayer serviceNetworkLayer = new PhysicalLayerImpl(getIdToken(), new NodesImpl(getIdToken()), new LinksImpl(getIdToken()), new LinkSegmentsImpl(getIdToken()));
    register(serviceNetworkLayer);
    return serviceNetworkLayer;
  }

  /**
   * {@inheritDoc}}
   */
  @Override
  public PhysicalNetworkLayers clone() {
    return new PhysicalNetworkLayers(this);
  }

}
