package org.planit.zoning;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;

/**
 * Undirected connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, each connection will yield a connectoid edge and two connectoid segments when
 * constructing the transport network internally based on the referenced node
 *
 * @author markr
 *
 */
public class UndirectedConnectoidImpl extends ConnectoidImpl implements UndirectedConnectoid {

  /** generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 373775073620741347L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UndirectedConnectoidImpl.class.getCanonicalName());

  // Protected

  /**
   * the access point to an infrastructure layer
   */
  protected Node accessNode;

  /**
   * Set the accessNode
   * 
   * @param accessNode to use
   */
  protected void setAccessNode(Node accessNode) {
    this.accessNode = accessNode;
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessNode the node in the network (layer) the connectoid connects with
   * @param accessZone for the connectoid
   * @param length     for the connection
   * @throws PlanItException thrown if there is an error
   */
  protected UndirectedConnectoidImpl(final IdGroupingToken idToken, final Node accessNode, Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setAccessNode(accessNode);
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessNode the node in the network (layer) the connectoid connects with
   * @throws PlanItException thrown if there is an error
   */
  public UndirectedConnectoidImpl(IdGroupingToken idToken, Node accessNode) {
    super(idToken);
    setAccessNode(accessNode);
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected UndirectedConnectoidImpl(UndirectedConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setAccessNode(connectoidImpl.getAccessNode());
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getAccessNode() {
    return accessNode;
  }

}
