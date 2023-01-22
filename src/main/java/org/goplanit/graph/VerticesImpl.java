package org.goplanit.graph;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.VertexFactory;
import org.goplanit.utils.graph.Vertices;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * Vertices implementation container and factory access
 * 
 * @author markr
 *
 */
public class VerticesImpl extends GraphEntitiesImpl<Vertex> implements Vertices {

  /** factory to create vertex instances */
  private final VertexFactory vertexFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public VerticesImpl(final IdGroupingToken groupId) {
    super(Vertex::getId);
    this.vertexFactory = new VertexFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId       to use
   * @param vertexFactory to use
   */
  public VerticesImpl(final IdGroupingToken groupId, final VertexFactory vertexFactory) {
    super(Vertex::getId);
    this.vertexFactory = vertexFactory;
  }

  /**
   * Copy constructor, also creates a new factory with reference to this container
   * 
   * @param verticesImpl to copy
   */
  public VerticesImpl(VerticesImpl verticesImpl) {
    super(verticesImpl);
    this.vertexFactory = new VertexFactoryImpl(verticesImpl.vertexFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexFactory getFactory() {
    return vertexFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VerticesImpl clone() {
    return new VerticesImpl(this);
  }

}
