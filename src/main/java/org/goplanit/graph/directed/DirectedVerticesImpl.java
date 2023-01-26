package org.goplanit.graph.directed;

import org.goplanit.graph.GraphEntitiesImpl;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.DirectedVertexFactory;
import org.goplanit.utils.graph.directed.DirectedVertices;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * DirectedVertices implementation container and factory access
 * 
 * @author markr
 *
 */
public class DirectedVerticesImpl extends GraphEntitiesImpl<DirectedVertex> implements DirectedVertices {

  /** factory to create vertex instances */
  private final DirectedVertexFactory directedVertexFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public DirectedVerticesImpl(final IdGroupingToken groupId) {
    super(DirectedVertex::getId);
    this.directedVertexFactory = new DirectedVertexFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId               to use
   * @param directedVertexFactory to use
   */
  public DirectedVerticesImpl(final IdGroupingToken groupId, final DirectedVertexFactory directedVertexFactory) {
    super(Vertex::getId);
    this.directedVertexFactory = directedVertexFactory;
  }

  /**
   * Copy constructor, also creates a new factory with reference to this container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public DirectedVerticesImpl(DirectedVerticesImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.directedVertexFactory =
            new DirectedVertexFactoryImpl(other.directedVertexFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertexFactory getFactory() {
    return directedVertexFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVerticesImpl clone() {
    return new DirectedVerticesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVerticesImpl deepClone() {
    return new DirectedVerticesImpl(this, true);
  }

}
