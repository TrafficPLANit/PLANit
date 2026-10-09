package org.goplanit.network.virtual.graph.conjugate;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.conjugate.ConjugateConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.graph.conjugate.ConjugateConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.graph.conjugate.ConjugateConnectoidEdges;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;

import java.util.function.BiConsumer;

/**
 * 
 * Conjugate connectoid edge container implementation
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidEdgesImpl
    extends ManagedGraphEntitiesImpl<ConjugateConnectoidDirectedEdge> implements ConjugateConnectoidEdges {

  /** factory to use */
  private final ConjugateConnectoidEdgeFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidDirectedEdge::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = new ConjugateConnectoidEdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId               to use for creating ids for instances
   * @param factory               the factory to use
   */
  public ConjugateConnectoidEdgesImpl(final IdGroupingToken groupId, ConjugateConnectoidEdgeFactory factory) {
    super(ConjugateConnectoidDirectedEdge::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConjugateConnectoidEdgesImpl(ConjugateConnectoidEdgesImpl other, boolean deepCopy, BiConsumer<ConjugateConnectoidDirectedEdge, ConjugateConnectoidDirectedEdge> mapper) {
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
  public ConjugateConnectoidEdgesImpl deepCloneWithMapping(BiConsumer<ConjugateConnectoidDirectedEdge, ConjugateConnectoidDirectedEdge> mapper) {
    return new ConjugateConnectoidEdgesImpl(this, true, mapper);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
