package org.goplanit.zoning;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.ConnectoidType;
import org.goplanit.utils.zoning.Zone;

/**
 * connectoid connecting one or more (transfer/OD) zone(s) to the physical road network, the type of connectoid depends on the implementing class
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
   * Stores access properties for each zone
   * 
   * @author markr
   *
   */
  protected class AccessZoneProperties {

    /** access zone for these properties */
    public final Zone accessZone;

    /** length to connectoid */
    public Double lengthKm = DEFAULT_LENGTH_KM;

    /** the explicitly allowed modes, when null all modes allowed */
    public TreeMap<Long, Mode> allowedModes = null;

    /**
     * constructor
     * 
     * @param accessZone to use
     */
    protected AccessZoneProperties(Zone accessZone) {
      this.accessZone = accessZone;
    }

    /**
     * Copy constructor
     * 
     * @param other to copy
     */
    @SuppressWarnings("unchecked")
    public AccessZoneProperties(AccessZoneProperties other) {
      this.accessZone = other.accessZone;
      this.lengthKm = other.lengthKm;

      /* shallow */
      if (other.allowedModes != null) {
        this.allowedModes = (TreeMap<Long, Mode>) other.allowedModes.clone();
      }
    }

    /**
     * add an allowed mode
     * 
     * @param mode to explicitly allow
     */
    void addAllowedMode(Mode mode) {
      if (allowedModes == null) {
        allowedModes = new TreeMap<>();
      }
      allowedModes.put(mode.getId(), mode);
    }
  }

  /**
   * name of the connectoid if any
   */
  protected String name = null;

  /** the type of connectoid to identify its purpose more easily */
  protected ConnectoidType type = DEFAULT_CONNECTOID_TYPE;

  /** the zones and their properties accessible from this connectoid */
  protected TreeMap<Long, AccessZoneProperties> accessZones = new TreeMap<Long, AccessZoneProperties>();

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
  public void recreateAccessZoneIdMapping() {
    Collection<AccessZoneProperties> accessZoneValues = accessZones.values();
    accessZones = new TreeMap<Long, AccessZoneProperties>();
    accessZoneValues.forEach(accessZoneValue -> accessZones.put(accessZoneValue.accessZone.getId(), accessZoneValue));
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
  protected ConnectoidImpl(IdGroupingToken idToken) {
    super(generateId(idToken));
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessZone for the connectoid
   * @param length     for the connection
   */
  protected ConnectoidImpl(final IdGroupingToken idToken, Zone accessZone, double length) {
    this(idToken);
    addAccessZone(accessZone);
    setLength(accessZone, length);
  }

  /**
   * Constructor
   *
   * @param idToken    contiguous id generation within this group for instances of this class
   * @param accessZone for the connectoid
   */
  protected ConnectoidImpl(final IdGroupingToken idToken, Zone accessZone) {
    this(idToken);
    addAccessZone(accessZone);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConnectoidImpl(ConnectoidImpl other, boolean deepCopy) {
    super(other);
    this.name = other.name;
    this.type = other.type;

    this.accessZones = new TreeMap<>();
    other.accessZones.forEach( (k,v) ->
            accessZones.put( k, deepCopy ? new AccessZoneProperties(v) : v));
  }

  // Public

  // Getters-Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public void setType(ConnectoidType type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidType getType() {
    return type;
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
   * {@inheritDoc}
   */
  @Override
  public Collection<Zone> getAccessZones() {
    return accessZones.values().stream().map((amp) -> {
      return amp.accessZone;
    }).collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getFirstAccessZone() {
    return iterator().next();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getNumberOfAccessZones() {
    return accessZones.size();
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public Optional<Double> getLengthKm(Zone accessZone) {
    if (!hasAccessZone(accessZone)) {
      LOGGER.warning(
          String.format("unknown access zone %s (id:%d) for connectoid %s (id:%d) when collecting length", accessZone.getXmlId(), accessZone.getId(), getXmlId(), getId()));
      return Optional.empty();
    }
    return Optional.of(accessZones.get(accessZone.getId()).lengthKm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone addAccessZone(Zone accessZone) {
    if (accessZone == null) {
      LOGGER.warning(String.format("unable to add access zone to connectoid %s, it is null", getXmlId()));
    }
    AccessZoneProperties duplicate = accessZones.put(accessZone.getId(), new AccessZoneProperties(accessZone));
    return duplicate != null ? duplicate.accessZone : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Zone accessZone, Mode mode) {
    if (!hasAccessZone(accessZone)) {
      LOGGER.warning(String.format("unknown access zone %s (id:%d) for connectoid %s (id:%d) when checking if mode is allowed", accessZone.getXmlId(), accessZone.getId(),
          getXmlId(), getId()));
      return false;
    }
    Map<Long, Mode> allowedModes = accessZones.get(accessZone.getId()).allowedModes;
    /* when allowed modes are null --> all modes are allowed, otherwise, only explicitly allowed modes */
    return allowedModes != null ? allowedModes.containsKey(mode.getId()) : true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAccessZone(Zone accessZone) {
    if (accessZone == null) {
      return false;
    }
    return accessZones.containsKey(accessZone.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLength(Zone accessZone, double length) {
    if (hasAccessZone(accessZone)) {
      accessZones.get(accessZone.getId()).lengthKm = length;
    } else {
      LOGGER.warning(String.format("unknown access zone %s (id:%d) for connectoid %s (id:%d) when setting length", accessZone.getXmlId(), accessZone.getId(), getXmlId(), getId()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addAllowedMode(Zone accessZone, Mode allowedMode) {
    if (hasAccessZone(accessZone)) {
      final AccessZoneProperties accessZoneProperties = accessZones.get(accessZone.getId());
      accessZoneProperties.addAllowedMode(allowedMode);
    } else {
      LOGGER.warning(String.format("unknown access zone %s (id:%d) for connectoid %s (id:%d) when adding allowed mode %s (id: %d)", accessZone.getXmlId(), accessZone.getId(),
          getXmlId(), getId(), allowedMode.getXmlId(), allowedMode.getId()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Mode> getExplicitlyAllowedModes(Zone accessZone) {
    if (hasAccessZone(accessZone)) {
      final AccessZoneProperties accessZoneProperties = accessZones.get(accessZone.getId());
      if (accessZoneProperties.allowedModes != null && !accessZoneProperties.allowedModes.isEmpty()) {
        return Collections.unmodifiableCollection(accessZoneProperties.allowedModes.values());
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Zone> iterator() {
    Iterator<Zone> it = new Iterator<Zone>() {

      private Iterator<AccessZoneProperties> iterator = accessZones.values().iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Zone next() {
        return iterator.next().accessZone;
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
  public abstract ConnectoidImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConnectoidImpl deepClone();

}
