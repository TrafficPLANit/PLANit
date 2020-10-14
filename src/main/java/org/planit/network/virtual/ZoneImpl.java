package org.planit.network.virtual;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Represents a demand generating zone in the network.
 * 
 * @author markr
 *
 */
public class ZoneImpl implements Zone {

  /**
   * Unique identifier for the zone
   */
  protected final long id;

  /**
   * External Id for this zone
   */
  protected final Object externalId;

  /**
   * generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Centroid of the zone
   */
  protected Centroid centroid = null;

  /**
   * Generate unique id for this zone
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id for this zone
   */
  protected static long generateZoneId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Zone.class);
  }

  // Public

  /**
   * Constructor
   * 
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param externalId the external ID of this zone
   * @param centroid   of the zone
   */
  public ZoneImpl(final IdGroupingToken groupId, final Object externalId, final Centroid centroid) {
    id = generateZoneId(groupId);
    this.externalId = externalId;
    this.centroid = centroid;
  }

  /**
   * Constructor
   * 
   * @param groupId    contiguous id generation within this group for instances of this class
   * @param externalId the external ID of this zone
   */
  public ZoneImpl(final IdGroupingToken groupId, final Object externalId) {
    id = generateZoneId(groupId);
    this.externalId = externalId;
    this.centroid = null;
  }

  /**
   * Returns the id of this zone
   * 
   * @return id of this zone
   */
  @Override
  public long getId() {
    return this.id;
  }

  /**
   * Add a property from the original input that is not part of the readily available members
   * 
   * @param key   property key
   * @param value property value
   */
  public void addInputProperty(String key, Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * Get input property by its key
   * 
   * @param key property key
   * @return property value
   */
  public Object getInputProperty(String key) {
    return inputProperties.get(key);
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
   * Set the centroid of this zone
   * 
   * @param centroid centroid for this zone
   */
  public void setCentroid(Centroid centroid) {
    this.centroid = centroid;
  }

  @Override
  public Object getExternalId() {
    return externalId;
  }

  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

}
