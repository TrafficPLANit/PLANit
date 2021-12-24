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
