package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.DirectedConnectoidFactory;
import org.goplanit.utils.zoning.DirectedConnectoids;
import org.goplanit.utils.zoning.Zone;

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
  public DirectedConnectoid registerNew(boolean downstreamAccessNode, LinkSegment accessLinkSegment) {
    return registerNew(downstreamAccessNode, accessLinkSegment, null, Connectoid.DEFAULT_LENGTH_KM);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(final boolean downstreamAccessNode, LinkSegment accessLinkSegment, Zone parentZone, double length) {
    DirectedConnectoid newConnectoid =
        new DirectedConnectoidImpl(getIdGroupingToken(), downstreamAccessNode, accessLinkSegment, parentZone, length);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

}
