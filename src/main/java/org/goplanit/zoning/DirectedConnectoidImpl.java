package org.goplanit.zoning;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.Zone;

/**
 * Undirected connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, each connection will yield a connectoid edge and two connectoid segments when
 * constructing the transport network internally based on the referenced node
 *
 * @author markr
 *
 */
public class DirectedConnectoidImpl extends ConnectoidImpl implements DirectedConnectoid {

  /** generated UID */

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedConnectoidImpl.class.getCanonicalName());

  // Protected

  /** unique id across directed connectoids */
  protected long directedConnectoidId;

  /**
   * the access point to an infrastructure layer
   */
  protected LinkSegment accessEdgeSegment;

  /** the node access given an access edge segment is either up or downstream */
  protected boolean nodeAccessDownstream = DEFAULT_NODE_ACCESS_DOWNSTREAM;

  /**
   * Generate directed connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of directed connectoid
   */
  protected static long generateDirectedConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, DirectedConnectoid.DIRECTED_CONNECTOID_ID_CLASS);
  }

  /**
   * set the directed connectoid id
   * 
   * @param directedConnectoidId to use
   */
  protected void setDirectedConnectoidId(long directedConnectoidId) {
    this.directedConnectoidId = directedConnectoidId;
  }

  /**
   * Set the accessEdgeSegment
   * 
   * @param accessEdgeSegment to use
   */
  protected void setAccessLinkSegment(LinkSegment accessEdgeSegment) {
    this.accessEdgeSegment = accessEdgeSegment;
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param downstreamAccessNode when true access node is chosen as the downstream node of the segment, when false, upstream node is chosen
   * @param accessLinkSegment the link segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   * @param accessZone        for the connectoid
   * @param length            for the connection (not of the edge segment, but to access the zone)
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken, final boolean downstreamAccessNode, final LinkSegment accessLinkSegment, final Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
    setAccessLinkSegment(accessLinkSegment);
    setNodeAccessDownstream(downstreamAccessNode);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param downstreamAccessNode when true access node is chosen as the downstream node of the segment, when false, upstream node is chosen
   * @param accessLinkSegment the link segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken, final boolean downstreamAccessNode, final LinkSegment accessLinkSegment) {
    super(idToken);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
    setAccessLinkSegment(accessLinkSegment);
    setNodeAccessDownstream(downstreamAccessNode);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected DirectedConnectoidImpl(final DirectedConnectoidImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setDirectedConnectoidId(other.getDirectedConnectoidId());
    setAccessLinkSegment(other.getAccessLinkSegment());
    setNodeAccessDownstream(other.isNodeAccessDownstream());
  }

  // Public

  // Getters-Setters

  /**
   * the directed connectoid unique id
   * 
   * @return directed connectoid id
   */
  public long getDirectedConnectoidId() {
    return directedConnectoidId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegment getAccessLinkSegment() {
    return accessEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void replaceAccessLinkSegment(LinkSegment accessEdgeSegment) {
    setAccessLinkSegment(accessEdgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNodeAccessDownstream() {
    return nodeAccessDownstream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeAccessDownstream(boolean nodeAccessDownstream) {
    this.nodeAccessDownstream = nodeAccessDownstream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getAccessVertex() {
    return isNodeAccessDownstream() ? getAccessLinkSegment().getDownstreamVertex() : getAccessLinkSegment().getUpstreamVertex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setDirectedConnectoidId(generateDirectedConnectoidId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidImpl shallowClone() {
    return new DirectedConnectoidImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidImpl deepClone() {
    return new DirectedConnectoidImpl(this, true);
  }

}
