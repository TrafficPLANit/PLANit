package org.goplanit.network.virtual.physical;

import org.goplanit.utils.graph.ManagedGraphEntitiesImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinkFactory;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinks;

import java.util.function.BiConsumer;

/**
 * 
 * Connectoid link container implementation
 * 
 * @author markr
 *
 */
public class ConnectoidLinksImpl extends ManagedGraphEntitiesImpl<ConnectoidLink> implements ConnectoidLinks {

  /** factory to use */
  private final ConnectoidLinkFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidLinksImpl(final IdGroupingToken groupId) {
    super(ConnectoidDirectedEdge::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = new ConnectoidLinkFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId               to use for creating ids for instances
   * @param connectoidEdgeFactory the factory to use
   */
  public ConnectoidLinksImpl(final IdGroupingToken groupId, ConnectoidLinkFactory connectoidEdgeFactory) {
    super(ConnectoidLink::getId, ConnectoidDirectedEdge.EDGE_ID_CLASS);
    this.factory = connectoidEdgeFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public ConnectoidLinksImpl(
      ConnectoidLinksImpl other, boolean deepCopy, BiConsumer<ConnectoidLink, ConnectoidLink> mapper) {
    super(other, deepCopy, mapper);
    this.factory =
            new ConnectoidLinkFactoryImpl(other.factory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinkFactory getFactory() {
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
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinksImpl shallowClone() {
    return new ConnectoidLinksImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinksImpl deepClone() {
    return new ConnectoidLinksImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidLinksImpl deepCloneWithMapping(BiConsumer<ConnectoidLink, ConnectoidLink> mapper) {
    return new ConnectoidLinksImpl(this, true, mapper);
  }

}
