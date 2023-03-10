package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoidFactory;
import org.goplanit.utils.zoning.UndirectedConnectoids;

import java.util.function.BiConsumer;

/**
 * Implementation of Connectoids class
 * 
 * @author markr
 *
 */
public class UndirectedConnectoidsImpl extends ConnectoidsImpl<UndirectedConnectoid> implements UndirectedConnectoids {

  /** factory to use */
  private final UndirectedConnectoidFactory undirectedConnectoidFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public UndirectedConnectoidsImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.undirectedConnectoidFactory = new UndirectedConnectoidFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                     to use for creating ids for instances
   * @param undirectedConnectoidFactory the factory to use
   */
  public UndirectedConnectoidsImpl(final IdGroupingToken groupId, UndirectedConnectoidFactory undirectedConnectoidFactory) {
    super(groupId);
    this.undirectedConnectoidFactory = undirectedConnectoidFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public UndirectedConnectoidsImpl(UndirectedConnectoidsImpl other, boolean deepCopy, BiConsumer<UndirectedConnectoid, UndirectedConnectoid> mapper) {
    super(other, deepCopy, mapper);
    this.undirectedConnectoidFactory =
            new UndirectedConnectoidFactoryImpl(other.undirectedConnectoidFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidFactory getFactory() {
    return undirectedConnectoidFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional undirected connectoid id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), UndirectedConnectoid.UNDIRECTED_CONNECTOID_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidsImpl shallowClone() {
    return new UndirectedConnectoidsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidsImpl deepClone() {
    return new UndirectedConnectoidsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidsImpl deepCloneWithMapping(BiConsumer<UndirectedConnectoid, UndirectedConnectoid> mapper) {
    return new UndirectedConnectoidsImpl(this, true, mapper);
  }
}
