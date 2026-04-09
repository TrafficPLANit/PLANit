package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
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
public class DirectedConnectoidFactoryImpl extends
    ManagedIdEntityFactoryImpl<DirectedConnectoid> implements DirectedConnectoidFactory {

  /** container to use */
  protected final DirectedConnectoids directedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected DirectedConnectoidFactoryImpl(
      final IdGroupingToken groupId, final DirectedConnectoids directedConnectoids) {
    super(groupId);
    this.directedConnectoids = directedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(
      Zone accessZone, boolean downstreamAccessNode, LinkSegment accessLinkSegment, double length) {
    var accessNode = downstreamAccessNode ? accessLinkSegment.getDownstreamNode() : accessLinkSegment.getUpstreamNode();
    DirectedConnectoid newConnectoid =
        new DirectedConnectoidImpl(getIdGroupingToken(), accessZone, accessNode, accessLinkSegment, length);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(Zone accessZone, boolean downstreamAccessNode, LinkSegment accessLinkSegment) {
    var accessNode = downstreamAccessNode ? accessLinkSegment.getDownstreamNode() : accessLinkSegment.getUpstreamNode();
    DirectedConnectoid newConnectoid =
        new DirectedConnectoidImpl(getIdGroupingToken(), accessZone, accessNode, accessLinkSegment);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(Node accessNode) {
    DirectedConnectoid newConnectoid =
        new DirectedConnectoidImpl(getIdGroupingToken(), accessNode);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }


}
