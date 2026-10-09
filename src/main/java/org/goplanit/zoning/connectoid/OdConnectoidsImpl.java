package org.goplanit.zoning.connectoid;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;
import org.goplanit.utils.zoning.connectoid.OdConnectoidFactory;
import org.goplanit.utils.zoning.connectoid.OdConnectoids;

import java.util.function.BiConsumer;

/**
 * Implementation of Connectoids class
 * 
 * @author markr
 *
 */
public class OdConnectoidsImpl extends ConnectoidsImpl<OdConnectoid> implements OdConnectoids {

  /** factory to use */
  private final OdConnectoidFactory undirectedConnectoidFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public OdConnectoidsImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.undirectedConnectoidFactory = new OdConnectoidFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                     to use for creating ids for instances
   * @param undirectedConnectoidFactory the factory to use
   */
  public OdConnectoidsImpl(
      final IdGroupingToken groupId, OdConnectoidFactory undirectedConnectoidFactory) {
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
  public OdConnectoidsImpl(
      OdConnectoidsImpl other,
      boolean deepCopy,
      BiConsumer<OdConnectoid, OdConnectoid> mapper) {

    super(other, deepCopy, mapper);
    this.undirectedConnectoidFactory =
            new OdConnectoidFactoryImpl(
                other.undirectedConnectoidFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidFactory getFactory() {
    return undirectedConnectoidFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional undirected connectoid id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), OdConnectoid.OD_CONNECTOID_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidsImpl shallowClone() {
    return new OdConnectoidsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidsImpl deepClone() {
    return new OdConnectoidsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdConnectoidsImpl deepCloneWithMapping(BiConsumer<OdConnectoid, OdConnectoid> mapper) {
    return new OdConnectoidsImpl(this, true, mapper);
  }
}
