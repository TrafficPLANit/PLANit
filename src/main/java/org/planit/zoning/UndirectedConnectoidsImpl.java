package org.planit.zoning;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoidFactory;
import org.planit.utils.zoning.UndirectedConnectoids;

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
   * Copy constructor
   * 
   * @param other to copy
   */
  public UndirectedConnectoidsImpl(UndirectedConnectoidsImpl other) {
    super(other);
    this.undirectedConnectoidFactory = other.undirectedConnectoidFactory;
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
  public void recreateIds(boolean reset) {
    if (reset == true) {
      IdGenerator.reset(getFactory().getIdGroupingToken(), UndirectedConnectoid.UNDIRECTED_CONNECTOID_ID_CLASS);
    }

    super.recreateIds(reset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidsImpl clone() {
    return new UndirectedConnectoidsImpl(this);
  }
}
