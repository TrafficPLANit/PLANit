package org.planit.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;
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
  protected final long id;

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
   * Generate next id available
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id of this link segment
   */
  protected static int generateMacroscopicLinkSegmentTypeId(final IdGroupingToken groupId) {
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
  public MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane,
      final Object externalId) {
    this.id = generateMacroscopicLinkSegmentTypeId(groupId);
    this.name = name;
    this.capacityPerLane = capacityPerLane;
    this.maximumDensityPerLane = maximumDensityPerLane;
    this.externalId = externalId;
    modeProperties = new HashMap<Mode, MacroscopicModeProperties>();
  }

  /**
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param externalId            external reference number of the link type
   * @param modeProperties        mode properties
   */
  public MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane, final Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    this(groupId, name, capacityPerLane, maximumDensityPerLane, externalId);
    setModeProperties(modeProperties);
  }

  // Getters - Setters

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public double getCapacityPerLane() {
    return capacityPerLane;
  }

  @Override
  public double getMaximumDensityPerLane() {
    return maximumDensityPerLane;
  }

  @Override
  public Object getExternalId() {
    return externalId;
  }

  @Override
  public boolean hasExternalId() {
    return (externalId != null);
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
   * Set the map of mode properties for this link
   * 
   * @param modeProperties map of mode properties for this link
   */
  @Override
  public void setModeProperties(Map<Mode, MacroscopicModeProperties> modeProperties) {
    this.modeProperties = modeProperties;
  }

}
