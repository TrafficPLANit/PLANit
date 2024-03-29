package org.goplanit.zoning;

import org.goplanit.path.ManagedDirectedPathsImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.DirectedConnectoidFactory;
import org.goplanit.utils.zoning.DirectedConnectoids;

import java.util.function.BiConsumer;

/**
 * Implementation of directed connectoids class
 * 
 * @author markr
 *
 */
public class DirectedConnectoidsImpl extends ConnectoidsImpl<DirectedConnectoid> implements DirectedConnectoids {

  /** factory to use */
  private final DirectedConnectoidFactory directedConnectoidFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public DirectedConnectoidsImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.directedConnectoidFactory = new DirectedConnectoidFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                   to use for creating ids for instances
   * @param directedConnectoidFactory the factory to use
   */
  public DirectedConnectoidsImpl(final IdGroupingToken groupId, DirectedConnectoidFactory directedConnectoidFactory) {
    super(groupId);
    this.directedConnectoidFactory = directedConnectoidFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public DirectedConnectoidsImpl(
      DirectedConnectoidsImpl other, boolean deepCopy, BiConsumer<DirectedConnectoid, DirectedConnectoid> mapper) {
    super(other, deepCopy, mapper);
    this.directedConnectoidFactory =
            new DirectedConnectoidFactoryImpl(other.directedConnectoidFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidFactory getFactory() {
    return directedConnectoidFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional directed connectoid id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), DirectedConnectoid.DIRECTED_CONNECTOID_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidsImpl shallowClone() {
    return new DirectedConnectoidsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidsImpl deepClone() {
    return new DirectedConnectoidsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidsImpl deepCloneWithMapping(BiConsumer<DirectedConnectoid, DirectedConnectoid> mapper) {
    return new DirectedConnectoidsImpl(this, true, mapper);
  }

}
