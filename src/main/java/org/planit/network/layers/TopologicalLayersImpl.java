package org.planit.network.layers;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TopologicalLayer;
import org.planit.utils.network.layers.TopologicalLayers;

/**
 * implementation of the transport layers interface, without the createNew() method, but now with base layer class of TopologicalLayer
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayersImpl<T extends TopologicalLayer> extends TransportLayersImpl<T> implements TopologicalLayers<T> {

  /**
   * Constructor
   * 
   * @param groupingId for id generation
   */
  public TopologicalLayersImpl(IdGroupingToken groupingId) {
    super(groupingId);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public TopologicalLayersImpl(TopologicalLayersImpl<T> other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> clone();

}
