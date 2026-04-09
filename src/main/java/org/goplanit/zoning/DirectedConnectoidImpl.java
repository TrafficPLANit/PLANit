package org.goplanit.zoning;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.Zone;

import java.util.logging.Logger;


/**
 * Undirected connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, it comprises
 * one or more combinations of zone-access segments when constructing the transport network internally based on
 * the referenced access node. It may also explicitly allow modes (and thus excluding other modes). If no modes
 * are attached to an access segment zone combination it is assumed all modes are allowed
 *
 * @author markr
 *
 */
public class DirectedConnectoidImpl
    extends ConnectoidImpl<DirectedConnectoidAccessZoneEntry>
    implements DirectedConnectoid {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedConnectoidImpl.class.getCanonicalName());

  // Protected

  /** unique id across directed connectoids */
  protected long directedConnectoidId;

  /** the node access for all access edge segments is either up or downstream */
  protected Boolean nodeAccessDownstream = null;

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
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken) {
    super(idToken);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessNode        the access node
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken,
      Node accessNode) {
    super(idToken);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
    setAccessVertex(accessNode);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessZone        the access zone
   * @param accessVertex      the access vertex
   * @param accessSegment     initial access segment
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken, Zone accessZone, DirectedVertex accessVertex, EdgeSegment accessSegment) {
    super(idToken, accessZone, accessVertex);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
    getAccessZoneEntry(accessZone).addAccessLinkSegment(accessSegment);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessZone        the access zone
   * @param accessVertex      the access vertex
   * @param accessSegment     initial access segment
   * @param lengthKm for zone connectoid combination
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken,
      Zone accessZone,
      DirectedVertex accessVertex,
      EdgeSegment accessSegment,
      double lengthKm) {
    super(idToken, accessZone, accessVertex, lengthKm);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
    getAccessZoneEntry(accessZone).addAccessLinkSegment(accessSegment);
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
    setNodeAccessDownstream(other.isAccessNodeAlwaysDownstream());
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
  public DirectedConnectoidAccessZoneEntry createAccessZoneEntry(Zone accessZone){
    if (accessZone == null) {
      LOGGER.warning(String.format(
          "Unable to add access zone to directed connectoid %s, it is null", getIdsAsString()));
    }
    return getAccessZoneEntries().put(
        accessZone.getId(), new DirectedConnectoidAccessZoneEntryImpl(this, accessZone));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAccessNodeAlwaysDownstream() {
    return nodeAccessDownstream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeAccessDownstream(boolean nodeAccessDownstream) {
    if(this.nodeAccessDownstream != null && hasAccessLinkSegments()){
      LOGGER.warning("Unable to change node access direction as it is set and access link segments are still " +
          "present");
      return;
    }
    this.nodeAccessDownstream = nodeAccessDownstream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Zone accessZone, Mode mode) {
    if (!hasAccessZoneEntry(accessZone)) {
      LOGGER.warning(String.format("unknown access zone %s (id:%d) for connectoid %s (id:%d) when checking if mode " +
          "is allowed", accessZone.getXmlId(), accessZone.getId(), getXmlId(), getId()));
      return false;
    }
    return accessZoneEntries.get(accessZone.getId()).isModeAllowed(mode);
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
