package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Zone;

/**
 * Represents a zone base class.
 * 
 * @author markr
 *
 */
public abstract class ZoneImpl extends ExternalIdAbleImpl implements Zone {

  /**
   * name of the zone
   */
  protected String name;

  /**
   * generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Centroid of the zone
   */
  protected Centroid centroid;

  /**
   * geometry of this zone
   */
  protected Geometry geometry = null;

  /**
   * Generate unique id for this zone
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id for this zone
   */
  protected static long generateZoneId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Zone.ZONE_ID_CLASS);
  }

  /**
   * set the centroid for this zone. It is assumed the centroid is correctly configured to be compatible with this zoneImpl
   * 
   * @param centroid to set
   */
  protected void setCentroid(Centroid centroid) {
    this.centroid = centroid;
  }

  /**
   * set the zone's unique internal id across all zones
   * 
   * @param id to set
   */
  protected void setId(long id) {
    super.setId(id);
  }

  // Public

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public ZoneImpl(final IdGroupingToken tokenId) {
    super(generateZoneId(tokenId));
  }

  /**
   * Constructor
   * 
   * @param other to copy
   */
  public ZoneImpl(final ZoneImpl other) {
    super(other);
    this.name = other.name;
    if (other.inputProperties != null) {
      this.inputProperties = new HashMap<String, Object>(other.inputProperties);
    }
    this.centroid = other.centroid;
    this.geometry = other.geometry;
  }

  /**
   * Returns the centroid of this zone
   * 
   * @return centroid of this zone
   */
  @Override
  public Centroid getCentroid() {
    return centroid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
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
  public void addInputProperty(final String key, final Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInputProperty(final String key) {
    if (inputProperties != null) {
      return inputProperties.get(key);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateZoneId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZoneImpl clone();

}
