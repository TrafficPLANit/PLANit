package org.goplanit.network.virtual;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdge;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.ConjugateConnectoidEdges;
import org.goplanit.utils.network.virtual.ConnectoidEdge;

import java.util.function.BiConsumer;

/**
 * 
 * Conjugate connectoid edge container implementation
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidEdgesImpl extends ManagedGraphEntitiesImpl<ConjugateConnectoidEdge> implements ConjugateConnectoidEdges {

  /** factory to use */
  private final ConjugateConnectoidEdgeFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.factory = new ConjugateConnectoidEdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId               to use for creating ids for instances
   * @param factory               the factory to use
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId, ConjugateConnectoidEdgeFactory factory) {
    super(ConjugateConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConjugateConnectoidEdgesImpl(ConjugateConnectoidEdgesImpl other, boolean deepCopy, BiConsumer<ConjugateConnectoidEdge,ConjugateConnectoidEdge> mapper) {
    super(other, deepCopy, mapper);
    this.factory =
            new ConjugateConnectoidEdgeFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgeFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional connectoid edge id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), ConnectoidEdge.CONNECTOID_EDGE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgesImpl shallowClone() {
    return new ConjugateConnectoidEdgesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgesImpl deepClone() {
    return new ConjugateConnectoidEdgesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidEdgesImpl deepCloneWithMapping(BiConsumer<ConjugateConnectoidEdge,ConjugateConnectoidEdge> mapper) {
    return new ConjugateConnectoidEdgesImpl(this, true, mapper);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
