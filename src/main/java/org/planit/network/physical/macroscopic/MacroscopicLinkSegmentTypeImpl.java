package org.planit.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Each macroscopic link segment is of a particular type reflecting segment specific properties. On top of the segment specific properties each segment can have user class specific
 * properties as well.
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypeImpl implements MacroscopicLinkSegmentType {

  // Protected

  /**
   * Unique segment type id
   */
  protected long id;

  /**
   * External reference number of link type
   */
  private Object externalId;

  /**
   * name of the link segment type
   */
  protected String name;

  /**
   * Maximum flow, i.e. capacity in veh/h/lane
   */
  protected final double capacityPerLane;

  /**
   * Maximum density in veh/km/lane
   */
  protected final double maximumDensityPerLane;

  /**
   * Map of mode properties for each mode for this link segment
   */
  protected Map<Mode, MacroscopicModeProperties> modeProperties;
  
  /**
   * set the id on this link segment type
   * @param id to set
   */
  protected void setId(long id) {
    this.id = id;
  }  

  /**
   * Generate next id available
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of this link segment
   */
  protected static long generateMacroscopicLinkSegmentTypeId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, MacroscopicLinkSegmentType.class);
  }

  // Public

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param externalId            external reference number of the link type
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane,
      final Object externalId) {
    setId(generateMacroscopicLinkSegmentTypeId(groupId));
    setName(name);
    this.capacityPerLane = capacityPerLane;
    this.maximumDensityPerLane = maximumDensityPerLane;
    this.externalId = externalId;
    this.modeProperties = new HashMap<Mode, MacroscopicModeProperties>();
  }

  /**
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param externalId            external reference number of the link type
   * @param modeProperties        mode properties
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane, final Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    this(groupId, name, capacityPerLane, maximumDensityPerLane, externalId);
    if (modeProperties != null) {
      setModeProperties(modeProperties);
    }
  }
  
  /**
   * Copy constructor. Use carefully since ids are also copied causing non-unique ids. Note that the mode propertoes are owned by each instance so they are deep copied, 
   * everything else is not
   * 
   * @param macroscopicLinkSegmentTypeImpl to copy from
   */
  protected MacroscopicLinkSegmentTypeImpl(final MacroscopicLinkSegmentTypeImpl other) {
    setId(other.getId());
    setName(other.getName());
    this.capacityPerLane = other.getCapacityPerLane();
    this.maximumDensityPerLane = other.getMaximumDensityPerLane();
    this.externalId = other.getExternalId();
    
    this.modeProperties = new HashMap<Mode, MacroscopicModeProperties>();
    other.modeProperties.forEach( (mode, properties) -> modeProperties.put(mode, properties.clone()));
  }  

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return id;
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
  public double getCapacityPerLane() {
    return capacityPerLane;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getMaximumDensityPerLane() {
    return maximumDensityPerLane;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setExternalId(Object externalId) {
    this.externalId = externalId;
  }  

  /**
   * Returns the mode properties for a specified mode along this link
   * 
   * @param mode the specified mode
   * @return the mode properties for this link and mode
   */
  @Override
  public MacroscopicModeProperties getModeProperties(Mode mode) {
    if (modeProperties.containsKey(mode)) {
      return modeProperties.get(mode);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setModeProperties(Map<Mode, MacroscopicModeProperties> modeProperties) {
    this.modeProperties = modeProperties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicModeProperties addModeProperties(Mode mode, MacroscopicModeProperties properties) {
    return modeProperties.put(mode, properties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAvailable(Mode mode) {
    return modeProperties.containsKey(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAvailableModes() {
    return modeProperties.keySet();
  }

  /**
   * Compare link segment types based on their id alone, which is assumed to be unique
   */
  @Override
  public int compareTo(MacroscopicLinkSegmentType other) {
    return Long.valueOf(id).compareTo(other.getId());
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType clone() {
    return new MacroscopicLinkSegmentTypeImpl(this);    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicModeProperties removeModeProperties(Mode toBeRemovedMode) {
    return modeProperties.remove(toBeRemovedMode);
  }

}
