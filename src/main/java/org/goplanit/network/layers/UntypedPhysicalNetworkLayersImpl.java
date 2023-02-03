package org.goplanit.network.layers;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.layers.UntypedPhysicalNetworkLayers;

/**
 * Implementation of container and factory to manage physical network layers.
 * 
 * @author markr
 *
 */
public abstract class UntypedPhysicalNetworkLayersImpl<L extends UntypedPhysicalLayer<?, ?, ?>> extends TopologicalLayersImpl<L> implements UntypedPhysicalNetworkLayers<L> {

  /**
   * Constructor
   * 
   * @param idToken for id generation
   */
  public UntypedPhysicalNetworkLayersImpl(IdGroupingToken idToken) {
    super(idToken);
  }

  /**
   * Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public UntypedPhysicalNetworkLayersImpl(UntypedPhysicalNetworkLayersImpl<L> other, boolean deepCopy) {
    super(other,deepCopy);
  }

  /**
   * {@inheritDoc}}
   */
  @Override
  public abstract UntypedPhysicalNetworkLayersImpl<L> shallowClone();

  /**
   * {@inheritDoc}}
   */
  @Override
  public abstract UntypedPhysicalNetworkLayersImpl<L> deepClone();

}
