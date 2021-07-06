package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.DirectedConnectoidFactory;
import org.planit.utils.zoning.DirectedConnectoids;
import org.planit.utils.zoning.Zone;

/**
 * Factory for creating directed connectoids (on container)
 * 
 * @author markr
 */
public class DirectedConnectoidFactoryImpl extends ManagedIdEntityFactoryImpl<DirectedConnectoid> implements DirectedConnectoidFactory {

  /** container to use */
  protected final DirectedConnectoids directedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected DirectedConnectoidFactoryImpl(final IdGroupingToken groupId, final DirectedConnectoids directedConnectoids) {
    super(groupId);
    this.directedConnectoids = directedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(LinkSegment accessLinkSegment, Zone parentZone, double length) throws PlanItException {
    DirectedConnectoid newConnectoid = registerNew(accessLinkSegment);
    newConnectoid.addAccessZone(parentZone);
    newConnectoid.setLength(parentZone, length);
    directedConnectoids.register(newConnectoid);
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
    DirectedConnectoid newConnectoid = new DirectedConnectoidImpl(getIdGroupingToken(), accessLinkSegment);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerUniqueCopyOf(ManagedId directedConnectoid) {
    DirectedConnectoid copy = createUniqueCopyOf(directedConnectoid);
    directedConnectoids.register(copy);
    return copy;
  }
}
