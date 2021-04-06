package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoids;
import org.planit.utils.zoning.Zone;

/**
 * Implementation of Connectoids class
 * 
 * @author markr
 *
 */
public class UndirectedConnectoidsImpl extends ConnectoidsImpl<UndirectedConnectoid> implements UndirectedConnectoids {

  /**
   * Constructor
   * 
   * @param idToken to use
   */
  public UndirectedConnectoidsImpl(IdGroupingToken idToken) {
    super(idToken);
  }


  /**
   * register using undirected connectoid id
   * @param connectoid to register
   */
  @Override
  public UndirectedConnectoid register(UndirectedConnectoid connectoid) {
    return register(connectoid.getUndirectedConnectoidId(), connectoid);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid registerNew(Node accessNode, Zone parentZone, double length) throws PlanItException {
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(idToken, accessNode, parentZone, length);
    register(newConnectoid);
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
    UndirectedConnectoid newConnectoid = new UndirectedConnectoidImpl(idToken, accessNode);
    register(newConnectoid);
    return newConnectoid;
  }
  
}
