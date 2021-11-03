package org.goplanit.graph;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeFactory;
import org.goplanit.utils.graph.Edges;
import org.goplanit.utils.id.IdGroupingToken;

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
    super(Edge::getId);
    this.edgeFactory = new EdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param edgeFactory to use
   */
  public EdgesImpl(EdgeFactory edgeFactory) {
    super(Edge::getId);
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
