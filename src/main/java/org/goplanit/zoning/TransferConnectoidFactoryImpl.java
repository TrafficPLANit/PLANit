package org.goplanit.zoning;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.*;

import static org.goplanit.utils.zoning.ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM;

/**
 * Factory for creating directed connectoids (on container)
 * 
 * @author markr
 */
public class TransferConnectoidFactoryImpl extends
    ManagedIdEntityFactoryImpl<TransferConnectoid> implements TransferConnectoidFactory {

  /** container to use */
  protected final TransferConnectoids directedConnectoids;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected TransferConnectoidFactoryImpl(
      final IdGroupingToken groupId, final TransferConnectoids directedConnectoids) {
    super(groupId);
    this.directedConnectoids = directedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoid registerNewWithDirectedEntry(
      Zone accessZone,
      boolean downstreamAccessNode,
      LinkSegment accessLinkSegment,
      double length,
      ZoneConnectoidType type) {

    var accessNode = downstreamAccessNode ? accessLinkSegment.getDownstreamNode() : accessLinkSegment.getUpstreamNode();
    TransferConnectoid newConnectoid =
        new TransferConnectoidImpl(
            getIdGroupingToken(), accessNode, accessZone, accessLinkSegment, length, type);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoid registerNewWithDirectedEntry(
      Zone accessZone, boolean downstreamAccessNode, LinkSegment accessLinkSegment, ZoneConnectoidType type) {
    return registerNewWithDirectedEntry(accessZone, downstreamAccessNode, accessLinkSegment, DEFAULT_LENGTH_KM.get(), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoid registerNew(DirectedVertex accessNode) {
    TransferConnectoid newConnectoid =
        new TransferConnectoidImpl(getIdGroupingToken(), accessNode);
    directedConnectoids.register(newConnectoid);
    return newConnectoid;
  }


}
