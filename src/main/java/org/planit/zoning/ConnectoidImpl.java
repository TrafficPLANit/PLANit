package org.planit.zoning;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.ConnectoidType;
import org.planit.utils.zoning.Zone;

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

    public final Zone accessZone;

    public Double length = DEFAULT_LENGTH_KM;

    public HashMap<Long, Mode> allowedModes = null;

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
      this.length = other.length;
      /* shallow */
      if (other.allowedModes != null) {
        this.allowedModes = (HashMap<Long, Mode>) other.allowedModes.clone();
      }
    }

    void addAllowedMode(Mode mode) {
      if (allowedModes == null) {
        allowedModes = new HashMap<Long, Mode>();
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
  protected final HashMap<Long, AccessZoneProperties> accessZones = new HashMap<Long, AccessZoneProperties>();

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
  protected ConnectoidImpl(IdGroupingToken idToken) {
    super(generateId(idToken));
  }

  /**
   * Copy constructor
   * 
   * @param connectoidImpl to copy
   */
  protected ConnectoidImpl(ConnectoidImpl connectoidImpl) {
    super(connectoidImpl);
    for (AccessZoneProperties entry : connectoidImpl.accessZones.values()) {
      accessZones.put(entry.accessZone.getId(), new AccessZoneProperties(entry));
    }
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
  public Double getLength(Zone accessZone) {
    if (!hasAccessZone(accessZone)) {
      LOGGER.warning(String.format("unknown access zone %s for connectoid %s", accessZone.getXmlId(), getXmlId()));
      return null;
    }
    return accessZones.get(accessZone.getId()).length;
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
  public boolean isModeAllowed(Zone accessZone, Mode mode) throws PlanItException {
    if (!hasAccessZone(accessZone)) {
      LOGGER.warning(String.format("unknown access zone %s for connectoid %s", accessZone.getXmlId(), getXmlId()));
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
      accessZones.get(accessZone.getId()).length = length;
    } else {
      LOGGER.warning(String.format("unknown access zone %s for connectoid %s", accessZone.getXmlId(), getXmlId()));
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
      LOGGER.warning(String.format("unknown access zone %s for connectoid %s", accessZone.getXmlId(), getXmlId()));
    }
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

}
