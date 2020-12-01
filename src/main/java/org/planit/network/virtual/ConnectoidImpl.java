package org.planit.network.virtual;

import org.planit.graph.DirectedEdgeImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * connectoid connecting a zone to the physical road network, carrying two connectoid segments in one or two directions which may carry additional information for each particular
 * direction of the connectoid.
 *
 * @author markr
 *
 */
public class ConnectoidImpl extends DirectedEdgeImpl implements Connectoid {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = 373775073620741347L;

  /**
   * unique internal identifier across connectoids
   */
  protected long connectoidId;

  /**
   * Generate connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of connectoid
   */
  protected static long generateConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Connectoid.class);
  }

  /**
   * Set the connectoid id
   * 
   * @param connectoidId to set
   */
  protected void setConnectoidId(long connectoidId) {
    this.connectoidId = connectoidId;
  }

  /**
   * Constructor
   *
   * @param groupId   contiguous id generation within this group for instances of this class
   * @param centroidA the centroid at one end of the connectoid
   * @param nodeB     the node at the other end of the connectoid
   * @param length    length of the current connectoid
   * @throws PlanItException thrown if there is an error
   */
  protected ConnectoidImpl(final IdGroupingToken groupId, final Centroid centroidA, final Node nodeB, final double length) throws PlanItException {
    super(groupId, centroidA, nodeB, length);
    setConnectoidId(generateConnectoidId(groupId));
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected ConnectoidImpl(ConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setConnectoidId(connectoidImpl.getConnectoidId());
  }

  // Public

  /**
   * Register connectoidSegment.
   *
   * If there already exists a connectoidSegment for that direction it is replaced and returned
   *
   * @param connectoidSegment connectoid segment to be registered
   * @param directionAB       direction of travel
   * @return replaced ConnectoidSegment
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public ConnectoidSegment registerConnectoidSegment(final ConnectoidSegment connectoidSegment, final boolean directionAB) throws PlanItException {
    return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
  }

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getConnectoidId() {
    return connectoidId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidImpl clone() {
    return new ConnectoidImpl(this);
  }
}
