package org.planit.network;

import org.planit.utils.id.IdGroupingToken;

/**
 * An infrastructure layer that is of a topological nature, i.e., it has node, links, etc.
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayerImpl extends InfrastructureLayerImpl implements TopologicalLayer{

  /** Constructor
   * 
   * @param tokenId for id generation
   */
  public TopologicalLayerImpl(IdGroupingToken tokenId) {
    super(tokenId);
  }

}
