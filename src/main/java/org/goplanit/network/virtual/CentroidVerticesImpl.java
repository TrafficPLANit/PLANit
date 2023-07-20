package org.goplanit.network.virtual;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeFactory;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.CentroidVertexFactory;
import org.goplanit.utils.network.virtual.CentroidVertices;

import java.util.function.BiConsumer;

/**
 * 
 * Centroid vertices managed container implementation
 * 
 * @author markr
 *
 */
public class CentroidVerticesImpl extends ManagedGraphEntitiesImpl<CentroidVertex> implements CentroidVertices {

  /** factory to use */
  private final CentroidVertexFactory centroidVertexFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public CentroidVerticesImpl(final IdGroupingToken groupId) {
    super(CentroidVertex::getId, CentroidVertex.VERTEX_ID_CLASS);
    this.centroidVertexFactory = new CentroidVertexFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId     to use for creating ids for instances
   * @param centroidVertexFactory the factory to use
   */
  public CentroidVerticesImpl(final IdGroupingToken groupId, CentroidVertexFactory centroidVertexFactory) {
    super(CentroidVertex::getId, CentroidVertex.VERTEX_ID_CLASS);
    this.centroidVertexFactory = centroidVertexFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep cpy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public CentroidVerticesImpl(CentroidVerticesImpl other, boolean deepCopy, BiConsumer<CentroidVertex,CentroidVertex> mapper) {
    super(other, deepCopy, mapper);
    this.centroidVertexFactory = new CentroidVertexFactoryImpl(other.centroidVertexFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertexFactory getFactory() {
    return centroidVertexFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVerticesImpl shallowClone() {
    return new CentroidVerticesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVerticesImpl deepClone() {
    return new CentroidVerticesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVerticesImpl deepCloneWithMapping(BiConsumer<CentroidVertex,CentroidVertex> mapper) {
    return new CentroidVerticesImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }
}
