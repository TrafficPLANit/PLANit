package org.planit.graph;

import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeFactory;
import org.planit.utils.graph.Edges;
import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of Edges interface
 * 
 * @author markr
 */
public class EdgesImpl extends GraphEntitiesImpl<Edge> implements Edges {

  /** factory to create edge instances */
  private final EdgeFactory edgeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public EdgesImpl(final IdGroupingToken groupId) {
    super(Edge::getId, Edge.EDGE_ID_CLASS);
    this.edgeFactory = new EdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param edgeFactory to use
   */
  public EdgesImpl(EdgeFactory edgeFactory) {
    super(Edge::getId, Edge.EDGE_ID_CLASS);
    this.edgeFactory = edgeFactory;
  }

  /**
   * Copy constructor
   * 
   * @param edgesImpl to copy
   */
  public EdgesImpl(EdgesImpl edgesImpl) {
    super(edgesImpl);
    this.edgeFactory = edgesImpl.edgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeFactory getFactory() {
    return edgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgesImpl clone() {
    return new EdgesImpl(this);
  }

}
