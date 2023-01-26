package org.goplanit.network.layers;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.TopologicalLayer;
import org.goplanit.utils.network.layers.TopologicalLayers;

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TopologicalLayersImpl(TopologicalLayersImpl<T> other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> deepClone();

}
