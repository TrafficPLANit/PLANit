package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.DirectedConnectoids;
import org.planit.utils.zoning.Zone;

/**
 * Implementation of Connectoids class
 * 
 * @author markr
 *
 */
public class DirectedConnectoidsImpl extends ConnectoidsImpl<DirectedConnectoid> implements DirectedConnectoids {

  /** the zoning builder to use */
  protected final ZoningBuilder zoningBuilder;
  
  /**
   * Constructor
   * 
   * @param zoningBuilder to use
   */
  public DirectedConnectoidsImpl(ZoningBuilder zoningBuilder) {
    super();
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * register a directed connectoid using the connectoid id unqiue across all connectoids
   *
   * @param connectoid to register
   */
  @Override
  public DirectedConnectoid register(DirectedConnectoid connectoid) {
    return register(connectoid.getId(), connectoid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(LinkSegment accessLinkSegment, Zone parentZone, double length) throws PlanItException {
    DirectedConnectoid newConnectoid = registerNew(accessLinkSegment);
    newConnectoid.addAccessZone(parentZone);
    newConnectoid.setLength(parentZone, length);
    register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(LinkSegment accessLinkSegment, Zone parentZone) throws PlanItException {
    return registerNew(accessLinkSegment, parentZone, Connectoid.DEFAULT_LENGTH_KM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(LinkSegment accessLinkSegment) throws PlanItException {
    DirectedConnectoid newConnectoid = zoningBuilder.createDirectedConnectoid(accessLinkSegment);
    register(newConnectoid);
    return newConnectoid;
  }

}