package org.planit.network.layer.service;

import org.planit.graph.VerticesWrapper;
import org.planit.utils.graph.Vertices;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.network.layer.service.ServiceNodes;

/**
 * 
 * ServiceNodes implementation.
 * 
 * @author markr
 *
 * @param <N> concrete class of service nodes
 */
public class ServiceNodesImpl<N extends ServiceNode> extends VerticesWrapper<N> implements ServiceNodes<N> {

  /**
   * Constructor
   * 
   * @param nodes the container instance to use to create and register nodes on
   */
  public ServiceNodesImpl(final Vertices<N> nodes) {
    super(nodes);
  }

}
