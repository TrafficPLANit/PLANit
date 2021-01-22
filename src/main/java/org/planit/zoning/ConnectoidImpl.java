package org.planit.zoning;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.zoning.Zone;

/**
 * connectoid connecting a zone to the physical road network, carrying two connectoid segments in one or two directions which may carry additional information for each particular
 * direction of the connectoid.
 *
 * @author markr
 *
 */
public class ConnectoidImpl extends ExternalIdAbleImpl implements Connectoid {

  // Protected

  /** generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 373775073620741347L;

  /**
   * the parent zone this connectoid provides access to
   */
  protected Zone parentZone;

  /**
   * the access point to an infrastructure layer
   */
  protected Node accessNode;

  /**
   * we can virtually assign a length to the access to work out an approximate cost depending on how costs on connectoid access points are computed
   */
  protected double length;

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
   * Set the parent zone
   * 
   * @param parentZone to use
   */
  protected void setParentZone(Zone parentZone) {
    this.parentZone = parentZone;
  }

  /**
   * Set the accessNode
   * 
   * @param accessNode to use
   */
  protected void setAccessNode(Node accessNode) {
    this.accessNode = accessNode;
  }

  /**
   * set length
   * 
   * @param length to use
   */
  protected void setLength(double length) {
    this.length = length;
  }

  /**
   * Constructor
   *
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param zone       the zone this connectoid provides access to
   * @param accessNode the node in the network (layer) the connectoid connects with
   * @param length     length of the current connectoid
   * @throws PlanItException thrown if there is an error
   */
  protected ConnectoidImpl(final IdGroupingToken groupId, final Zone zone, final Node accessNode, final double length) throws PlanItException {
    super(generateConnectoidId(groupId));
    setParentZone(zone);
    setAccessNode(accessNode);
    setLength(length);
  }

  /**
   * Constructor
   *
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param zone       the zone this connectoid provides access to
   * @param accessNode the node in the network (layer) the connectoid connects with
   * @param length     length of the current connectoid
   * @throws PlanItException thrown if there is an error
   */
  protected ConnectoidImpl(final IdGroupingToken groupId, final Zone zone, final Node accessNode) throws PlanItException {
    this(groupId, zone, accessNode, DEFAULT_LENGTH_KM);
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected ConnectoidImpl(ConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setParentZone(connectoidImpl.getParentZone());
    setAccessNode(connectoidImpl.getAccessNode());
    setLength(connectoidImpl.getLength());
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidImpl clone() {
    return new ConnectoidImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getParentZone() {
    return parentZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getAccessNode() {
    return accessNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getLength() {
    return length;
  }
}
