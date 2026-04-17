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
import org.goplanit.utils.zoning.ZoneConnectoidType;

import java.util.Optional;
import java.util.logging.Logger;

import static org.goplanit.utils.zoning.ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM;


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
    super(idToken, accessNode);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessVertex      the access vertex
   * @param accessZone        the access zone
   * @param accessSegment     initial access segment
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken, DirectedVertex accessVertex, Zone accessZone, EdgeSegment accessSegment) {
    this(idToken, accessVertex, accessZone, accessSegment, DEFAULT_LENGTH_KM.get());
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessVertex      the access vertex
   * @param accessZone        the access zone
   * @param accessSegment     initial access segment
   * @param lengthKm for zone connectoid combination
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessVertex,
      Zone accessZone,
      EdgeSegment accessSegment,
      double lengthKm) {
    this(idToken, accessVertex, accessZone, accessSegment, lengthKm, ZoneConnectoidType.NONE);
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessVertex      the access vertex
   * @param accessZone        the access zone for the initial access entry
   * @param accessSegment     initial access segment for access zone entry
   * @param lengthKm          length for initial access zone entry
   * @param type              type for initial access zone entry
   */
  protected DirectedConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessVertex,
      Zone accessZone,
      EdgeSegment accessSegment,
      double lengthKm,
      ZoneConnectoidType type) {
    super(idToken, accessVertex, accessZone, lengthKm, type);
    setDirectedConnectoidId(generateDirectedConnectoidId(idToken));

    var initialEntry = getAccessZoneEntry(accessZone);
    initialEntry.addAccessLinkSegment(accessSegment);
    initialEntry.setType(type);
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
    if(hasAccessZoneEntry(accessZone)){
      LOGGER.warning(String.format("Cannot create access zone entry for connectoid (%s) as one already exists for zone" +
              "(%d)",
          getIdsAsString(), accessZone.getIdsAsString()));
      return null;
    }
    var newEntry = new DirectedConnectoidAccessZoneEntryImpl(this, accessZone);
    getAccessZoneEntries().put(accessZone.getId(), newEntry);
    return newEntry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAccessNodeDownstreamOfSegments() {
    if(!hasAccessZoneEntries()){
      LOGGER.warning(String.format("Unable to verify access node directionality as no access zones are " +
          "registered for connectoid (%s)", getIdsAsString()));
      return false;
    }
    // assumes when an entry is present it has access segments
    if(!getFirstAccessZoneEntry().hasAccessLinkSegments()){
      LOGGER.warning(String.format("Unable to verify access node directionality as no access segments are " +
          "registered for connectoid (%s)", getIdsAsString()));
      return false;
    }
    // all access segments are vetted upon addition, so we can pick any to verify
    return getFirstAccessZoneEntry().getFirstAccessLinkSegment().getDownstreamVertex().equals(getAccessVertex());
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
