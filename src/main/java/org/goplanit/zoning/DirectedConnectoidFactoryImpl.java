package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.*;

import static org.goplanit.utils.zoning.ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM;

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
      Zone accessZone,
      boolean downstreamAccessNode,
      LinkSegment accessLinkSegment,
      double length,
      ZoneConnectoidType type) {

    var accessNode = downstreamAccessNode ? accessLinkSegment.getDownstreamNode() : accessLinkSegment.getUpstreamNode();
    DirectedConnectoid newConnectoid =
        new DirectedConnectoidImpl(
            getIdGroupingToken(), accessNode, accessZone, accessLinkSegment, length, type);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid registerNew(
      Zone accessZone, boolean downstreamAccessNode, LinkSegment accessLinkSegment, ZoneConnectoidType type) {
    return registerNew(accessZone, downstreamAccessNode, accessLinkSegment, DEFAULT_LENGTH_KM.get(), type);
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
