package org.goplanit.network.virtual.physical.conjugate;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLink;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLinkFactory;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidLinks;

import java.util.function.BiConsumer;

/**
 * 
 * Conjugate connectoid edge container implementation
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidLinksImpl
    extends ManagedGraphEntitiesImpl<ConjugateConnectoidLink> implements ConjugateConnectoidLinks {

  /** factory to use */
  private final ConjugateConnectoidLinkFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidLinksImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidLink::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = new ConjugateConnectoidLinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId               to use for creating ids for instances
   * @param factory               the factory to use
   */
  public ConjugateConnectoidLinksImpl(final IdGroupingToken groupId, ConjugateConnectoidLinkFactory factory) {
    super(ConjugateConnectoidLink::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConjugateConnectoidLinksImpl(
      ConjugateConnectoidLinksImpl other, boolean deepCopy, BiConsumer<ConjugateConnectoidLink, ConjugateConnectoidLink> mapper) {
    super(other, deepCopy, mapper);
    this.factory =
            new ConjugateConnectoidLinkFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidLinkFactory getFactory() {
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
  public ConjugateConnectoidLinksImpl shallowClone() {
    return new ConjugateConnectoidLinksImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidLinksImpl deepClone() {
    return new ConjugateConnectoidLinksImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidLinksImpl deepCloneWithMapping(
      BiConsumer<ConjugateConnectoidLink, ConjugateConnectoidLink> mapper) {
    return new ConjugateConnectoidLinksImpl(this, true, mapper);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
