package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoidFactory;
import org.planit.utils.zoning.UndirectedConnectoids;
import org.planit.utils.zoning.Zone;

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
