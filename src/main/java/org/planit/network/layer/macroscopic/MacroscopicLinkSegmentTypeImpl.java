package org.planit.network.layer.macroscopic;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;

/**
 * Each macroscopic link segment is of a particular type reflecting segment specific properties. On top of the segment specific properties each segment can have user class specific
 * properties as well.
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypeImpl extends ExternalIdAbleImpl implements MacroscopicLinkSegmentType {

  // Protected

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
   * 
   * @param id to set
   */
  protected void setId(long id) {
    super.setId(id);
  }

  /**
   * Generate an id based on token
   * 
   * @param idGroupingToken to use
   * @return created id
   */
  protected static long generateId(IdGroupingToken idGroupingToken) {
    return IdGenerator.generateId(idGroupingToken, MACROSCOPIC_LINK_SEGMENT_TYPE_ID_CLASS);
  }

  // Public

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane) {
    super(generateId(groupId));
    setName(name);
    this.capacityPerLane = capacityPerLane;
    this.maximumDensityPerLane = maximumDensityPerLane;
    this.modeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
  }

  /**
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param modeProperties        mode properties
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    this(groupId, name, capacityPerLane, maximumDensityPerLane);
    if (modeProperties != null) {
      setModeProperties(modeProperties);
    }
  }

  /**
   * Copy constructor. Use carefully since ids are also copied causing non-unique ids. Note that the mode propertoes are owned by each instance so they are deep copied, everything
   * else is not
   * 
   * @param other to copy from
   */
  protected MacroscopicLinkSegmentTypeImpl(final MacroscopicLinkSegmentTypeImpl other) {
    super(other);
    setName(other.getName());
    this.capacityPerLane = other.getCapacityPerLane();
    this.maximumDensityPerLane = other.getMaximumDensityPerLane();

    this.modeProperties = new TreeMap<Mode, MacroscopicModeProperties>();
    other.modeProperties.forEach((mode, properties) -> modeProperties.put(mode, properties.clone()));
  }

  // Getters - Setters

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
   * {@inheritDoc}
   */
  @Override
  public MacroscopicModeProperties removeModeProperties(Mode toBeRemovedMode) {
    return modeProperties.remove(toBeRemovedMode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentTypeImpl clone() {
    return new MacroscopicLinkSegmentTypeImpl(this);
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

}
