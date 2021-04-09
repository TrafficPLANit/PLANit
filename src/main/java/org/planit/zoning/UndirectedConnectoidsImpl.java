package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
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
  
  /** the zoning builder to use */
  protected final ZoningBuilder zoningBuilder;

  /**
   * Constructor
   * 
   * @param zoningBuilder to use
   */
  public UndirectedConnectoidsImpl(ZoningBuilder zoningBuilder) {
    super();
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid register(UndirectedConnectoid connectoid) {
    return register(connectoid.getId(), connectoid);
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
    UndirectedConnectoid newConnectoid = zoningBuilder.createUndirectedConnectoid(accessNode);
    register(newConnectoid);
    return newConnectoid;
  }

}
