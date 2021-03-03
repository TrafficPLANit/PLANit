package org.planit.zoning;

import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.Zone;

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

  /**
   * the access point to an infrastructure layer
   */
  protected LinkSegment accessEdgeSegment;

  /** the node access given an access edge segment is either up or downstream */
  protected boolean nodeAccessDownstream = DEFAULT_NODE_ACCESS_DOWNSTREAM;

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
   * @param accessLinkSegment the link segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   * @param accessZone        for the connectoid
   * @param length            for the connection (not of the edge segment, but to access the zone)
   */
  protected DirectedConnectoidImpl(final IdGroupingToken idToken, final LinkSegment accessLinkSegment, final Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setAccessLinkSegment(accessLinkSegment);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessEdgeSegment the edge segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   */
  public DirectedConnectoidImpl(final IdGroupingToken idToken, final LinkSegment accessEdgeSegment) {
    super(idToken);
    setAccessLinkSegment(accessEdgeSegment);
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected DirectedConnectoidImpl(final DirectedConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setAccessLinkSegment(connectoidImpl.getAccessLinkSegment());
  }

  // Public

  // Getters-Setters

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
  public void replaceAccessLinkSegment(EdgeSegment exitEdgeSegment) {
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

}
