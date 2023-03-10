package org.goplanit.network.layers;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.network.layer.TopologicalLayer;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.layers.TopologicalLayers;

import java.util.function.BiConsumer;

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
   * @param mapper apply to each mapping from original to copy
   */
  public TopologicalLayersImpl(TopologicalLayersImpl<T> other, boolean deepCopy, BiConsumer<T, T> mapper) {
    super(other, deepCopy, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayersImpl<T> deepCloneWithMapping(BiConsumer<T, T> mapper);

}
