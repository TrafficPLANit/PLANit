package org.planit.graph.directed;

import org.planit.graph.GraphEntitiesImpl;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.graph.directed.DirectedVertexFactory;
import org.planit.utils.graph.directed.DirectedVertices;
import org.planit.utils.id.IdGroupingToken;

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
    super(DirectedVertex::getId, DirectedVertex.VERTEX_ID_CLASS);
    this.directedVertexFactory = new DirectedVertexFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param graphBuilder          the graph builder to use to create vertices
   * @param directedVertexFactory to use
   */
  public DirectedVerticesImpl(final IdGroupingToken groupId, final DirectedVertexFactory directedVertexFactory) {
    super(Vertex::getId, DirectedVertex.VERTEX_ID_CLASS);
    this.directedVertexFactory = directedVertexFactory;
  }

  /**
   * Copy constructor
   * 
   * @param verticesImpl to copy
   */
  public DirectedVerticesImpl(DirectedVerticesImpl verticesImpl) {
    super(verticesImpl);
    this.directedVertexFactory = verticesImpl.directedVertexFactory;
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
    return new DirectedVerticesImpl(this);
  }

}
