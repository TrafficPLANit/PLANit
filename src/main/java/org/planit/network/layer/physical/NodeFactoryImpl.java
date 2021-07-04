package org.planit.network.layer.physical;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.NodeFactory;
import org.planit.utils.network.layer.physical.Nodes;

/**
 * Factory for creating nodes on nodes container
 * 
 * @author markr
 */
public class NodeFactoryImpl extends GraphEntityFactoryImpl<Node> implements NodeFactory {

  /**
   * Constructor
   * 
   * @param groupId  to use
   * @param vertices to use
   */
  protected NodeFactoryImpl(final IdGroupingToken groupId, final Nodes vertices) {
    super(groupId, vertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createNew() {
    return new NodeImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node registerNew() {
    final Node newVertex = createNew();
    getGraphEntities().register(newVertex);
    return newVertex;
  }

}
