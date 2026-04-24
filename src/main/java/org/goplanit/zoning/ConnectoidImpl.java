package org.goplanit.zoning;

import java.util.*;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.*;

/**
 * connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, the type of
 * connectoid depends on the implementing class
 *
 * @author markr
 *
 */
public abstract class ConnectoidImpl extends ExternalIdAbleImpl implements Connectoid {

  /** generated UID */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 373775073620741347L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConnectoidImpl.class.getCanonicalName());

  // Protected

  /**
   * name of the connectoid if any
   */
  protected String name = null;

  /**
   * the access point to an infrastructure layer
   */
  protected DirectedVertex refVertex;


  /** the zones and their properties accessible from this connectoid by type */
  protected TreeMap<Long,Map<ZoneConnectoidType, ConnectoidAccessZoneEntry>> accessZoneEntriesByZoneAndType =
      new TreeMap<>();

  /**
   * Generate connectoid id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of connectoid
   */
  protected static long generateId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Connectoid.class);
  }

  /**
   * set the connectoid id
   * 
   * @param id to set as unique internal id across all connectoids
   */
  protected void setId(long id) {
    super.setId(id);
  }

  /**
   * Constructor
   *
   * @param idToken contiguous id generation within this group for instances of this class
   */
  public ConnectoidImpl(IdGroupingToken idToken) {
    super(generateId(idToken));
  }

  /**
   * Constructor
   *
   * @param idToken contiguous id generation within this group for instances of this class
   * @param accessVertex for the connectoid
   */
  public ConnectoidImpl(IdGroupingToken idToken, DirectedVertex accessVertex) {
    super(generateId(idToken));
    setReferenceVertex(accessVertex);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  @SuppressWarnings("unchecked")
  protected ConnectoidImpl(ConnectoidImpl other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.refVertex = other.refVertex;

    this.accessZoneEntriesByZoneAndType.clear();
    other.accessZoneEntriesByZoneAndType.forEach( (k, v) ->
        {
          var newV = new TreeMap<>(v);
          accessZoneEntriesByZoneAndType.put( k, newV);
          v.forEach( (type,entry) -> newV.put(
              type,  deepCopy ? entry.deepClone() : entry.shallowClone() ));
        });
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidAccessZoneEntry createUndirectedAccessZoneEntry(Zone accessZone, ZoneConnectoidType type){
    if (accessZone == null) {
      LOGGER.warning(String.format(
          "Unable to add access zone to undirected connectoid %s, it is null", getIdsAsString()));
      return null;
    }
    if(hasAccessZoneEntry(accessZone, type)){
      LOGGER.warning(String.format("Cannot create access zone entry for undirected connectoid (%s) as " +
              "one already exists for zone (%s)",
          getIdsAsString(), accessZone.getIdsAsString()));
      return null;
    }
    var newEntry = new ConnectoidAccessZoneEntryImpl(accessZone, type);
    getAccessZoneEntriesByType().putIfAbsent(accessZone.getId(),new TreeMap<>());
    getAccessZoneEntriesByType().get(accessZone.getId()).put(type, newEntry);
    return newEntry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return this.name;
  }

  /**
   * Set the accessVertex
   *
   * @param accessVertex to use
   */
  @Override
  public void setReferenceVertex(final DirectedVertex accessVertex) {
    this.refVertex = accessVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getReferenceVertex() {
    return refVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidAccessZoneEntry removeAccessZoneEntry(Zone accessZone, ZoneConnectoidType type){
    if(!hasAccessZoneEntry(accessZone)){
      return null;
    }
    return getAccessZoneEntriesByType(accessZone).remove(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Long, Map<ZoneConnectoidType, ConnectoidAccessZoneEntry>> getAccessZoneEntriesByType() {
    return accessZoneEntriesByZoneAndType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Zone accessZone, ZoneConnectoidType type, Mode mode) {
    if (!hasAccessZoneEntry(accessZone, type)) {
      LOGGER.warning(String.format("unknown access zone (%s) with type %s for connectoid (%s) " +
          "when checking if mode is allowed", accessZone.getIdsAsString(), type, getIdsAsString()));
      return false;
    }
    return getAccessZoneEntry(accessZone, type).isModeAllowed(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<ConnectoidAccessZoneEntry> iterator() {
    return getAccessZoneEntriesByType().values().stream().flatMap(
        e -> e.values().stream()).iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateAccessZoneIdMapping() {
    var accessZoneEntriesCloned = new TreeMap<>(this.accessZoneEntriesByZoneAndType);
    this.accessZoneEntriesByZoneAndType = new TreeMap<>();
    accessZoneEntriesCloned.forEach( (k, v) -> {
      this.accessZoneEntriesByZoneAndType.put(
          v.values().iterator().next().getAccessZone().getId(), v);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidImpl deepClone();

}
