package org.goplanit.zoning;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoidFactory;
import org.goplanit.utils.zoning.UndirectedConnectoids;
import org.goplanit.utils.zoning.Zone;

/**
 * Factory for creating new undirected connectoids on container
 * 
 * @author markr
 */
public class UndirectedConnectoidFactoryImpl extends ManagedIdEntityFactoryImpl<UndirectedConnectoid> implements UndirectedConnectoidFactory {

  /** container to use */
  protected final UndirectedConnectoids undirectedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId               to use
   * @param undirectedConnectoids to use
   */
  protected UndirectedConnectoidFactoryImpl(final IdGroupingToken groupId, final UndirectedConnectoids undirectedConnectoids) {
    super(groupId);
    this.undirectedConnectoids = undirectedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode, Zone parentZone, double length) throws PlanItException {
    UndirectedConnectoid newConnectoid = registerNew(accessNode);
    newConnectoid.addAccessZone(parentZone);
    newConnectoid.setLength(parentZone, length);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode, Zone parentZone) throws PlanItException {
    return registerNew(accessNode, parentZone, Connectoid.DEFAULT_LENGTH_KM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode) throws PlanItException {
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(getIdGroupingToken(), accessNode);
    undirectedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

}
