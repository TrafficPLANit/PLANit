package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Polygon;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Zone;

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
  protected String externalId;

  /**
   * xml Id for this zone
   */
  protected String xmlId;

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
  protected Polygon geometry = null;

  /**
   * Generate unique id for this zone
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id for this zone
   */
  protected static long generateZoneId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Zone.class);
  }

  /**
   * set the centroid for this zone. It is assumed the centroid is correctly configured to be compatible with this zoneImpl
   * 
   * @param centroid to set
   */
  protected void setCentroid(Centroid centroid) {
    this.centroid = centroid;
  }

  // Public

  /**
   * Constructor
   * 
   * @param tokenId contiguous id generation within this group for instances of this class
   */
  public ZoneImpl(final IdGroupingToken tokenId) {
    id = generateZoneId(tokenId);
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
   * {@inheritDoc}
   */
  @Override
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return this.xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGeometry(Polygon geometry) {
    this.geometry = geometry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Polygon getGeometry() {
    return geometry;
  }

}
