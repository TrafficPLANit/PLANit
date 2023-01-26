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
   * Copy constructor, also creates a new factory with reference to this container
   *
   * @param edgesImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected EdgesImpl(EdgesImpl edgesImpl, boolean deepCopy) {
    super(edgesImpl, deepCopy);
    this.edgeFactory = new EdgeFactoryImpl(edgesImpl.edgeFactory.getIdGroupingToken(), this);
  }

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
    return new EdgesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GraphEntitiesImpl<Edge> deepClone() {
    return new EdgesImpl(this, true);
  }

}
