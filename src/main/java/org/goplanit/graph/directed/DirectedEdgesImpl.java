package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntitiesImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedEdgeFactory;
import org.goplanit.utils.graph.directed.DirectedEdges;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Implementation of DirectedEdges interface
 * 
 * @author markr
 */
public class DirectedEdgesImpl extends GraphEntitiesImpl<DirectedEdge> implements DirectedEdges {

  /** factory to use */
  private final DirectedEdgeFactory directedEdgeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public DirectedEdgesImpl(final IdGroupingToken groupId) {
    super(DirectedEdge::getId);
    this.directedEdgeFactory = new DirectedEdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId             to use for creating ids for instances
   * @param directedEdgeFactory the factory to use
   */
  public DirectedEdgesImpl(final IdGroupingToken groupId, DirectedEdgeFactory directedEdgeFactory) {
    super(DirectedEdge::getId);
    this.directedEdgeFactory = directedEdgeFactory;
  }

  /**
   * Copy constructor, also creates a new factory with reference to this container
   * 
   * @param directedEdgesImpl to copy
   */
  public DirectedEdgesImpl(DirectedEdgesImpl directedEdgesImpl) {
    super(directedEdgesImpl);
    this.directedEdgeFactory =
            new DirectedEdgeFactoryImpl(directedEdgesImpl.directedEdgeFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeFactory getFactory() {
    return directedEdgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgesImpl clone() {
    return new DirectedEdgesImpl(this);
  }

}
