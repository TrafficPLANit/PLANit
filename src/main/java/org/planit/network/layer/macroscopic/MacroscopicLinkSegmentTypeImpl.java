package org.planit.network.layer.macroscopic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Each macroscopic link segment is of a particular type reflecting segment specific properties. On top of the segment specific properties each segment can have user class specific
 * properties as well.
 * 
 * @author markr
 *
 */
public class MacroscopicLinkSegmentTypeImpl extends ExternalIdAbleImpl implements MacroscopicLinkSegmentType {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypeImpl.class.getCanonicalName());

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
   * Track access properties for each of the modes it supports for quick lookups
   */
  protected Map<Mode, AccessGroupProperties> modeAccessProperties;

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
    this.modeAccessProperties = new TreeMap<Mode, AccessGroupProperties>();
  }

  /**
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param accessGroupProperties mode specific access properties
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane,
      final Collection<AccessGroupProperties> accessGroupProperties) {
    this(groupId, name, capacityPerLane, maximumDensityPerLane);
    if (accessGroupProperties != null) {
      setAccessProperties(accessGroupProperties);
    }
  }

  /**
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   * @param accessGroupProperties mode specific access properties
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final double capacityPerLane, final double maximumDensityPerLane,
      final AccessGroupProperties accessGroupProperties) {
    this(groupId, name, capacityPerLane, maximumDensityPerLane);
    if (accessGroupProperties != null) {
      setAccessProperties(accessGroupProperties);
    }
  }

  /**
   * Copy constructor. Use carefully since ids are also copied causing non-unique ids. Note that the mode properties are owned by each instance so they are deep copied, everything
   * else is not
   * 
   * @param other to copy from
   */
  protected MacroscopicLinkSegmentTypeImpl(final MacroscopicLinkSegmentTypeImpl other) {
    super(other);
    setName(other.getName());
    this.capacityPerLane = other.getCapacityPerLane();
    this.maximumDensityPerLane = other.getMaximumDensityPerLane();

    this.modeAccessProperties = new TreeMap<Mode, AccessGroupProperties>();
    Set<Mode> modesDone = new TreeSet<Mode>();
    for (Mode mode : other.getAvailableModes()) {
      if (!modesDone.contains(mode)) {
        AccessGroupProperties clonedEntry = other.getAccessProperties(mode).clone();
        setAccessProperties(clonedEntry);
        modesDone.addAll(clonedEntry.getAccessModes());
      }
    }
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
  public AccessGroupProperties getAccessProperties(Mode mode) {
    if (modeAccessProperties.containsKey(mode)) {
      return modeAccessProperties.get(mode);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAvailable(Mode mode) {
    return modeAccessProperties.containsKey(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAvailableModes() {
    return modeAccessProperties.keySet();
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAccessProperties(final Collection<AccessGroupProperties> accessProperties) {
    Set<Mode> processedModes = new TreeSet<Mode>();
    for (AccessGroupProperties entry : accessProperties) {
      for (Mode mode : entry.getAccessModes()) {
        if (processedModes.contains(mode)) {
          LOGGER.warning(String.format("Multiple provided access proprties on link segment type define the same mode (%s), ignoring all but first encountered", mode.getXmlId()));
        }
        this.modeAccessProperties.put(mode, entry);
      }
      processedModes.addAll(entry.getAccessModes());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAccessProperties(final AccessGroupProperties accessProperties) {
    for (Mode mode : accessProperties.getAccessModes()) {
      this.modeAccessProperties.put(mode, accessProperties);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeModeAccess(final Mode toBeRemovedMode) {
    AccessGroupProperties accessProperties = getAccessProperties(toBeRemovedMode);
    if (accessProperties == null) {
      return false;
    }
    return accessProperties.removeAccessMode(toBeRemovedMode);
  }

}
