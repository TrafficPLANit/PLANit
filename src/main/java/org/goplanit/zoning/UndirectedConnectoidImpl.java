package org.goplanit.zoning;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.Zone;

/**
 * Undirected connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, each connection will yield a connectoid edge and two connectoid segments when
 * constructing the transport network internally based on the referenced node
 *
 * @author markr
 *
 */
public class UndirectedConnectoidImpl extends
    ConnectoidImpl<ConnectoidAccessZoneEntry>
    implements UndirectedConnectoid {

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
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessVertex the node in the network (layer) the connectoid connects with
   * @param accessZone   for the connectoid
   * @param length       for the connection
   */
  protected UndirectedConnectoidImpl(
      final IdGroupingToken idToken, final DirectedVertex accessVertex, final Zone accessZone, double length) {
    super(idToken, accessZone, accessVertex, length);
    setUndirectedConnectoidId(generateUndirectedConnectoidId(idToken));
    setAccessVertex(accessVertex);
  }

  /**
   * Constructor
   *
   * @param idToken      contiguous id generation within this group for instances of this class
   * @param accessZone   for the connectoid
   * @param accessVertex the node in the network (layer) the connectoid connects with
   */
  public UndirectedConnectoidImpl(
      final IdGroupingToken idToken, final Zone accessZone, final DirectedVertex accessVertex) {
    super(idToken, accessZone, accessVertex);
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
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  protected UndirectedConnectoidImpl(final UndirectedConnectoidImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setAccessVertex(other.getAccessVertex());
    setUndirectedConnectoidId(other.getUndirectedConnectoidId());
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
  public ConnectoidAccessZoneEntry createAccessZoneEntry(Zone accessZone){
    if (accessZone == null) {
      LOGGER.warning(String.format("unable to add access zone to undirected connectoid %s, it is null", getIdsAsString()));
    }
    return getAccessZoneEntries().put(accessZone.getId(), new ConnectoidAccessZoneEntryImpl(accessZone));
  }

  @Override
  public ConnectoidAccessZoneEntry getAccessZoneEntry(Zone accessZone) {
    return null;
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
  public UndirectedConnectoidImpl shallowClone() {
    return new UndirectedConnectoidImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoidImpl deepClone() {
    return new UndirectedConnectoidImpl(this, true);
  }

}
