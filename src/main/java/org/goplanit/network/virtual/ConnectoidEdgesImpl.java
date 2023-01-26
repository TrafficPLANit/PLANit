package org.goplanit.network.virtual;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidEdgeFactory;
import org.goplanit.utils.network.virtual.ConnectoidEdges;

/**
 * 
 * Connectoid edge container implementation
 * 
 * @author markr
 *
 */
public class ConnectoidEdgesImpl extends ManagedIdEntitiesImpl<ConnectoidEdge> implements ConnectoidEdges {

  /** factory to use */
  private final ConnectoidEdgeFactory connectoidEdgeFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidEdgesImpl(final IdGroupingToken groupId) {
    super(ConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.connectoidEdgeFactory = new ConnectoidEdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId               to use for creating ids for instances
   * @param connectoidEdgeFactory the factory to use
   */
  public ConnectoidEdgesImpl(final IdGroupingToken groupId, ConnectoidEdgeFactory connectoidEdgeFactory) {
    super(ConnectoidEdge::getId, ConnectoidEdge.EDGE_ID_CLASS);
    this.connectoidEdgeFactory = connectoidEdgeFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ConnectoidEdgesImpl(ConnectoidEdgesImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.connectoidEdgeFactory =
            new ConnectoidEdgeFactoryImpl(other.connectoidEdgeFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdgeFactory getFactory() {
    return connectoidEdgeFactory;
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
  public ConnectoidEdgesImpl clone() {
    return new ConnectoidEdgesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidEdgesImpl deepClone() {
    return new ConnectoidEdgesImpl(this, true);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
