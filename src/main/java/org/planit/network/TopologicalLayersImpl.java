package org.planit.network;

import org.planit.utils.id.IdGroupingToken;

/**
 * implementation of the InfrastructureLayers interface, without the createNew() method, but now with base layer class of TopologicalLayer
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayersImpl<T extends TopologicalLayer> extends InfrastructureLayersImpl<T> implements TopologicalLayers<T> {

  /** Constructor
   * 
   * @param groupingId for id generation
   */
  public TopologicalLayersImpl(IdGroupingToken groupingId) {
    super(groupingId);
  }    

}
