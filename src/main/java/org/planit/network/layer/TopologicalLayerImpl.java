package org.planit.network.layer;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.TopologicalLayer;

/**
 * An transport layer that is of a topological nature, i.e., it has node, links, etc.
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayerImpl extends TransportLayerImpl implements TopologicalLayer {

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
   * @param topologicalLayerImpl to copy
   */
  public TopologicalLayerImpl(TopologicalLayerImpl topologicalLayerImpl) {
    super(topologicalLayerImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayerImpl clone();

}
