package org.goplanit.graph;

import org.goplanit.utils.graph.*;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;

import java.util.Map;
import java.util.function.BiConsumer;

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public VerticesImpl(VerticesImpl verticesImpl, boolean deepCopy, BiConsumer<Vertex, Vertex> biConsumer) {
    super(verticesImpl, deepCopy, biConsumer);
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
  public VerticesImpl shallowClone() {
    return new VerticesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VerticesImpl deepClone() {
    return new VerticesImpl(this, true, null);
  }

  @Override
  public VerticesImpl deepCloneWithMapping(BiConsumer<Vertex, Vertex> mapper) {
    return new VerticesImpl(this, true, mapper);
  }

}
