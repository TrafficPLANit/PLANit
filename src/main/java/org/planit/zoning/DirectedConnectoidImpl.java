package org.planit.zoning;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
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
  protected EdgeSegment accessEdgeSegment;

  /**
   * Set the accessEdgeSegment
   * 
   * @param accessEdgeSegment to use
   */
  protected void setAccessEdgeSegment(EdgeSegment accessEdgeSegment) {
    this.accessEdgeSegment = accessEdgeSegment;
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessEdgeSegment the edge segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   * @param accessZone        for the connectoid
   * @param length            for the connection (not of the edge segment, but to access the zone)
   * @throws PlanItException thrown if there is an error
   */
  protected DirectedConnectoidImpl(final IdGroupingToken idToken, final EdgeSegment accessEdgeSegment, Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setAccessEdgeSegment(accessEdgeSegment);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessEdgeSegment the edge segment in the network (layer) the connectoid connects with (possibly via its downstream node)
   * @throws PlanItException thrown if there is an error
   */
  public DirectedConnectoidImpl(IdGroupingToken idToken, EdgeSegment accessEdgeSegment) {
    super(idToken);
    setAccessEdgeSegment(accessEdgeSegment);
  }

  /**
   * Copy constructor
   * 
   * @param DirectedConnectoidImpl to copy
   */
  protected DirectedConnectoidImpl(DirectedConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setAccessEdgeSegment(connectoidImpl.getAccessEdgeSegment());
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getAccessEdgeSegment() {
    return accessEdgeSegment;
  }

}
