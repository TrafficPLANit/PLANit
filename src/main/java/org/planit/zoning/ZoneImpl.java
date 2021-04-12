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
 * Represents a demand generating zone in the network.
 * 
 * @author markr
 *
 */
public class ZoneImpl extends ExternalIdAbleImpl implements Zone {

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

}
