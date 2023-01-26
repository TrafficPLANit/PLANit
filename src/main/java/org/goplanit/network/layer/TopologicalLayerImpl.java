package org.goplanit.network.layer;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.TopologicalLayer;

/**
 * An transport layer that is of a topological nature, i.e., it has node, links, etc.
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayerImpl extends NetworkLayerImpl implements TopologicalLayer {

  /**
   * Constructor
   * 
   * @param tokenId for id generation
   */
  public TopologicalLayerImpl(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public TopologicalLayerImpl(TopologicalLayerImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayerImpl clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayerImpl deepClone();

}
