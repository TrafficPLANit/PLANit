package org.goplanit.zoning;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.TransferConnectoid;
import org.goplanit.utils.zoning.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.ZoneConnectoidType;

import java.util.TreeMap;
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
public class TransferConnectoidImpl extends ConnectoidImpl implements TransferConnectoid {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TransferConnectoidImpl.class.getCanonicalName());

  // Protected

  /** unique id across directed connectoids */
  protected long transferConnectoidId;

  /**
   * Generate directed connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of directed connectoid
   */
  protected static long generateTransferConnectoidId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, TransferConnectoid.TRANSFER_CONNECTOID_ID_CLASS);
  }

  /**
   * set the transfer connectoid id
   * 
   * @param transferConnectoidId to use
   */
  protected void setTransferConnectoidId(long transferConnectoidId) {
    this.transferConnectoidId = transferConnectoidId;
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessNode        the access node
   */
  protected TransferConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessNode) {
    super(idToken, accessNode);
    setTransferConnectoidId(generateTransferConnectoidId(idToken));
  }

  /**
   * Constructor
   *
   * @param idToken           contiguous id generation within this group for instances of this class
   * @param accessVertex      the access vertex
   * @param accessZone        the access zone
   * @param accessSegment     initial access segment
   */
  protected TransferConnectoidImpl(
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
  protected TransferConnectoidImpl(
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
  protected TransferConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessVertex,
      Zone accessZone,
      EdgeSegment accessSegment,
      double lengthKm,
      ZoneConnectoidType type) {
    super(idToken, accessVertex);
    setTransferConnectoidId(generateTransferConnectoidId(idToken));
    var initialEntry = this.createDirectedAccessZoneEntry(accessZone, type);
    initialEntry.addAccessLinkSegment(accessSegment);
    initialEntry.setLengthKm(lengthKm);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TransferConnectoidImpl(final TransferConnectoidImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setTransferConnectoidId(other.getTransferConnectoidId());
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidAccessZoneEntry createDirectedAccessZoneEntry(Zone accessZone, ZoneConnectoidType type){
    if (accessZone == null) {
      LOGGER.warning(String.format(
          "Unable to add access zone to directed connectoid %s, it is null", getIdsAsString()));
      return null;
    }
    if(hasAccessZoneEntry(accessZone, type)){
      LOGGER.warning(String.format("Cannot create access zone entry for connectoid (%s) as " +
              "one already exists for zone (%s) and type %s",
          getIdsAsString(), accessZone.getIdsAsString(), type));
      return null;
    }
    var newEntry = new DirectedConnectoidAccessZoneEntryImpl(this, accessZone, type);
    getAccessZoneEntriesByType().putIfAbsent(accessZone.getId(),new TreeMap<>());
    this.accessZoneEntriesByZoneAndType.get(accessZone.getId()).put(type, newEntry);
    return newEntry;
  }

  /**
   * the directed connectoid unique id
   * 
   * @return directed connectoid id
   */
  public long getTransferConnectoidId() {
    return transferConnectoidId;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoidAccessZoneEntry createDirectedAccessZoneEntry(
      Zone accessZone, ZoneConnectoidType type, EdgeSegment accessSegment){
    var accessEntry = createDirectedAccessZoneEntry(accessZone, type);
    if(accessEntry != null) {
      accessEntry.addAccessLinkSegment(accessSegment);
    }
    return accessEntry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setTransferConnectoidId(generateTransferConnectoidId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidImpl shallowClone() {
    return new TransferConnectoidImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferConnectoidImpl deepClone() {
    return new TransferConnectoidImpl(this, true);
  }

}
