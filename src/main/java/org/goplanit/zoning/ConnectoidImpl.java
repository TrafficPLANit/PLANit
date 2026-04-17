package org.goplanit.zoning;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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


  /** the zones and their properties accessible from this connectoid */
  protected TreeMap<Long,T> accessZoneEntries = new TreeMap<>();

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
   * recreate the id mapping for the registered access zones
   */
  protected void recreateAccessZoneIdMapping() {
    var accessZoneEntriesCloned = new TreeMap<>(getAccessZoneEntries());
    this.accessZoneEntries = new TreeMap<>();
    accessZoneEntriesCloned.forEach( (k, v) ->
        this.accessZoneEntries.put(
            v.getAccessZone().getId(), v));
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

    var entry = createAccessZoneEntry(accessZone);
    entry.setLengthKm(length);
    entry.setType(type);
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

    this.accessZoneEntries.clear();
    other.accessZoneEntries.forEach( (k, v) ->
        accessZoneEntries.put( k, (T) (deepCopy ? v.deepClone() : v.shallowClone())));

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
  public Map<Long, T> getAccessZoneEntries() {
    return accessZoneEntries;
  }

  @Override
  public T getAccessZoneEntry(Zone accessZone) {
    return getAccessZoneEntries().get(accessZone.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getFirstAccessZoneEntry() {
    return getAccessZoneEntries().values().iterator().next();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfAccessZoneEntries() {
    return accessZoneEntries.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAccessZoneEntry(Zone accessZone) {
    if (accessZone == null) {
      return false;
    }
    return accessZoneEntries.containsKey(accessZone.getId());
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
  public Iterator<Zone> iterator() {
    Iterator<Zone> it = new Iterator<>() {

      private final Iterator<? extends T> iterator = getAccessZoneEntries().values().iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Zone next() {
        return iterator.next().getAccessZone();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
    return it;
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
  public abstract ConnectoidImpl<T> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidImpl<T> deepClone();

}
