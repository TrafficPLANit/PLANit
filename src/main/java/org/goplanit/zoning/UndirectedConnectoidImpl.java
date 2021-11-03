package org.goplanit.zoning;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.Zone;

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
  protected DirectedVertex accessVertex;

  /**
   * Generate undirected connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of undirected connectoid
   */
  protected static long generateUndirectedConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, UndirectedConnectoid.UNDIRECTED_CONNECTOID_ID_CLASS);
  }

  /**
   * Set the undirected connectoid id
   * 
   * @param undirectedConnectoidId to use
   */
  protected void setUndirectedConnectoidId(long undirectedConnectoidId) {
    this.undirectedConnectoidId = undirectedConnectoidId;
  }

  /**
   * Set the accessVertex
   * 
   * @param accessVertex to use
   */
  protected void setAccessVertex(final DirectedVertex accessVertex) {
    this.accessVertex = accessVertex;
  }

  /**
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessVertex the node in the network (layer) the connectoid connects with
   * @param accessZone   for the connectoid
   * @param length       for the connection
   */
  protected UndirectedConnectoidImpl(final IdGroupingToken idToken, final DirectedVertex accessVertex, final Zone accessZone, double length) {
    super(idToken, accessZone, length);
    setUndirectedConnectoidId(generateUndirectedConnectoidId(idToken));
    setAccessVertex(accessVertex);
  }

  /**
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessVertex the node in the network (layer) the connectoid connects with
   * @param accessZone   for the connectoid
   */
  public UndirectedConnectoidImpl(final IdGroupingToken idToken, final DirectedVertex accessVertex, final Zone accessZone) {
    super(idToken, accessZone);
    setUndirectedConnectoidId(generateUndirectedConnectoidId(idToken));
    setAccessVertex(accessVertex);
  }

  /**
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessVertex the node in the network (layer) the connectoid connects with
   */
  public UndirectedConnectoidImpl(final IdGroupingToken idToken, final DirectedVertex accessVertex) {
    super(idToken);
    setUndirectedConnectoidId(generateUndirectedConnectoidId(idToken));
    setAccessVertex(accessVertex);
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected UndirectedConnectoidImpl(final UndirectedConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    setAccessVertex(connectoidImpl.getAccessVertex());
    setUndirectedConnectoidId(connectoidImpl.getUndirectedConnectoidId());
  }

  // Public

  // Getters-Setters

  /**
   * collect the undirected connectoid's unique id
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
  public DirectedVertex getAccessVertex() {
    return accessVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setUndirectedConnectoidId(generateUndirectedConnectoidId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidImpl clone() {
    return new UndirectedConnectoidImpl(this);
  }

}
