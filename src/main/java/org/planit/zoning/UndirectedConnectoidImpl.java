package org.planit.zoning;

import java.util.logging.Logger;

import org.planit.utils.id.IdGenerator;
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
  
  /** unique id across undirected connectoids */
  protected long undirectedConnectoidId;

  /**
   * the access point to an infrastructure layer
   */
  protected Node accessNode;
  
  /**
   * Generate undirected connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of undirected connectoid
   */
  protected static long generateUndirectedConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, UndirectedConnectoid.class);
  }  
  
  /** set the directed connectoid id
   * @param directedConnectoidId to use
   */
  protected void setUndirectedConnectoidId(long undirectedConnectoidId) {
    this.undirectedConnectoidId = undirectedConnectoidId;
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
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessNode the node in the network (layer) the connectoid connects with
   * @param accessZone for the connectoid
   * @param length     for the connection
   */
  protected UndirectedConnectoidImpl(final IdGroupingToken idToken, final Node accessNode, Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setUndirectedConnectoidId(generateUndirectedConnectoidId(idToken));
    setAccessNode(accessNode);
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessNode the node in the network (layer) the connectoid connects with
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
  
  /** collect the undirected connectoid's unique id
   * 
   * @return undirected connectoid id
   */
  public long getUndirectedConnectoidId() {
    return undirectedConnectoidId;
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getAccessNode() {
    return accessNode;
  }

}
