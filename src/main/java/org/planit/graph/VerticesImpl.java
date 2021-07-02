package org.planit.graph;

import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.VertexFactory;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGroupingToken;

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
   * @param graphBuilder  the graph builder to use to create vertices
   * @param vertexFactory to use
   */
  public VerticesImpl(final IdGroupingToken groupId, final VertexFactory vertexFactory) {
    super(Vertex::getId);
    this.vertexFactory = vertexFactory;
  }

  /**
   * Copy constructor
   * 
   * @param verticesImpl to copy
   */
  public VerticesImpl(VerticesImpl verticesImpl) {
    super(verticesImpl);
    this.vertexFactory = verticesImpl.vertexFactory;
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
