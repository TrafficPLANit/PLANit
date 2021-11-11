package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.DirectedConnectoidFactory;
import org.goplanit.utils.zoning.DirectedConnectoids;

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
   * Copy constructor
   * 
   * @param other to copy
   */
  public DirectedConnectoidsImpl(DirectedConnectoidsImpl other) {
    super(other);
    this.directedConnectoidFactory = other.directedConnectoidFactory;
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
  public DirectedConnectoidsImpl clone() {
    return new DirectedConnectoidsImpl(this);
  }

}