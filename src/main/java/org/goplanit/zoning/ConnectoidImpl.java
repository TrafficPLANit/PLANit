package org.goplanit.zoning;

import java.util.*;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.*;

import static org.goplanit.utils.zoning.ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM;

/**
 * connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, the type of
 * connectoid depends on the implementing class
 *
 * @author markr
 *
 */
public abstract class ConnectoidImpl<T extends ConnectoidAccessZoneEntry>
    extends ExternalIdAbleImpl implements Connectoid<T> {

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
  protected DirectedVertex accessVertex;


  /** the zones and their properties accessible from this connectoid by type */
  protected TreeMap<Long,Map<ZoneConnectoidType, T>> accessZoneEntriesByZoneAndType = new TreeMap<>();

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
    setAccessVertex(accessVertex);
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessVertex for the connectoid
   * @param accessZone zone for the zone connectoid combination
   * @param length    length for the zone connectoid combination
   * @param type      the type of the zone connectoid combination reflecting how it is envisaged to be used
   */
  protected ConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessVertex,
      Zone accessZone,
      double length,
      ZoneConnectoidType type) {
    this(idToken, accessVertex);

    var entry = createAccessZoneEntry(accessZone, type);
    entry.setLengthKm(length);
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessVertex to use
   * @param accessZone zone for the zone connectoid combination
   * @param type the type of the zone connectoid combination reflecting how it is envisaged to be used
   */
  protected ConnectoidImpl(
      final IdGroupingToken idToken,
      DirectedVertex accessVertex,
      Zone accessZone,
      ZoneConnectoidType type) {
    this(idToken, accessVertex, accessZone, DEFAULT_LENGTH_KM.get(), type);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  @SuppressWarnings("unchecked")
  protected ConnectoidImpl(ConnectoidImpl<T> other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.accessVertex = other.accessVertex;

    this.accessZoneEntriesByZoneAndType.clear();
    other.accessZoneEntriesByZoneAndType.forEach( (k, v) ->
        {
          var newV = new TreeMap<>(v);
          accessZoneEntriesByZoneAndType.put( k, newV);
          v.forEach( (type,entry) -> newV.put(
              type,  (T) (deepCopy ? entry.deepClone() : entry.shallowClone()) ));
        });
  }

  // Public

  // Getters-Setters

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
  public void setAccessVertex(final DirectedVertex accessVertex) {
    this.accessVertex = accessVertex;
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
  public T removeAccessZoneEntry(Zone accessZone, ZoneConnectoidType type){
    if(!hasAccessZoneEntry(accessZone)){
      return null;
    }
    return getAccessZoneEntriesByType(accessZone).remove(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Long, Map<ZoneConnectoidType, T>> getAccessZoneEntriesByType() {
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
  public Iterator<T> iterator() {
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
  public abstract ConnectoidImpl<T> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidImpl<T> deepClone();

}
