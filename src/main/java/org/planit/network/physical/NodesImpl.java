package org.planit.network.physical;

import org.planit.graph.VerticesWrapper;
import org.planit.utils.graph.Vertices;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;

/**
 * 
 * Nodes implementation wrapper that simply utilises passed in vertices of the desired generic type to delegate registration and creation of its nodes on
 * 
 * @author markr
 *
 * @param <N> concrete class of nodes that are being created
 */
public class NodesImpl<N extends Node> extends VerticesWrapper<N> implements Nodes<N> {

  /**
   * Constructor
   * 
   * @param nodes the container instance to use to create and register nodes on
   */
  public NodesImpl(final Vertices<N> nodes) {
    super(nodes);
  }

}
