package org.goplanit.network.layer.macroscopic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

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
   * Maximum flow, i.e. capacity in pcu/h/lane
   */
  protected final Double capacityPerLanePcuHourLane;

  /**
   * Maximum density in pcu/km/lane
   */
  protected final Double maximumDensityPerLanePcuKmLane;

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
   * Constructor with no value for capacity and max density, so when collected they will provide the default instead
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param name    name of this link segment type
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name) {
    this(groupId, name, null, null);
  }

  /**
   * Constructor with no value for capacity and max density, so when collected they will provide the default instead
   * 
   * @param groupId         contiguous id generation within this group for instances of this class
   * @param name            name of this link segment type
   * @param capacityPerLane capacity per lane of this link segment type
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final Double capacityPerLane) {
    this(groupId, name, capacityPerLane, null);
  }

  /**
   * Constructor
   * 
   * @param groupId               contiguous id generation within this group for instances of this class
   * @param name                  name of this link segment type
   * @param capacityPerLane       capacity per lane of this link segment type
   * @param maximumDensityPerLane maximum density per lane of this link segment type
   */
  protected MacroscopicLinkSegmentTypeImpl(final IdGroupingToken groupId, final String name, final Double capacityPerLane, final Double maximumDensityPerLane) {
    super(generateId(groupId));
    setName(name);
    this.capacityPerLanePcuHourLane = capacityPerLane;
    this.maximumDensityPerLanePcuKmLane = maximumDensityPerLane;
    this.modeAccessProperties = new TreeMap<>();
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
    this.capacityPerLanePcuHourLane = other.getExplicitCapacityPerLane();
    this.maximumDensityPerLanePcuKmLane = other.getExplicitMaximumDensityPerLane();

    this.modeAccessProperties = new TreeMap<>();
    Set<Mode> modesDone = new TreeSet<>();
    for (Mode mode : other.getAllowedModes()) {
      if (!modesDone.contains(mode)) {
        AccessGroupProperties clonedEntry = other.getAccessProperties(mode).clone();
        setAccessGroupProperties(clonedEntry);
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
  public Double getExplicitCapacityPerLane() {
    return this.capacityPerLanePcuHourLane;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getExplicitMaximumDensityPerLane() {
    return this.maximumDensityPerLanePcuKmLane;
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
  public boolean isModeAllowed(Mode mode) {
    return modeAccessProperties.containsKey(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAllowedModes() {
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
  public void setAccessGroupProperties(final Collection<AccessGroupProperties> accessProperties) {
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
  public void setAccessGroupProperties(final AccessGroupProperties accessProperties) {
    for (Mode mode : accessProperties.getAccessModes()) {
      this.modeAccessProperties.put(mode, accessProperties);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addAccessGroupProperties(AccessGroupProperties accessProperties) {
    if (findEqualAccessPropertiesForAnyMode(accessProperties) != null) {
      LOGGER.warning(String.format("IGNORE: Unable to register new access properties on link segment type %s, identical group already exist", getXmlId()));
    }
    setAccessGroupProperties(accessProperties);
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
    boolean success = accessProperties.removeAccessMode(toBeRemovedMode);
    this.modeAccessProperties.remove(toBeRemovedMode);
    if(!accessProperties.hasAccessModes() && !hasAllowedModes()){
      LOGGER.warning(String.format("Link segment type (%s) has no more supported modes, consider removing", this.getXmlId()));
    }
    return success;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AccessGroupProperties findEqualAccessPropertiesForAnyMode(AccessGroupProperties accessProperties) {
    Set<Mode> processedModes = new HashSet<Mode>();
    for (AccessGroupProperties properties : this.modeAccessProperties.values()) {
      if (processedModes.contains(properties.getAccessModes().iterator().next())) {
        continue;
      }
      /* check if equal except for modes */
      if (properties.isEqualExceptForModes(accessProperties)) {
        return properties;
      }
      processedModes.addAll(properties.getAccessModes());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerModeOnAccessGroup(Mode accessMode, AccessGroupProperties accessGroupProperties) {
    if(findEqualAccessPropertiesForAnyMode(accessGroupProperties) == null){
      LOGGER.warning(String.format("IGNORE: Unable to register new access mode on provided access group because access group does not exist on this link segment type (%s)", getXmlId()));
      return;
    }
    if(modeAccessProperties.containsKey(accessMode)){
      LOGGER.warning(String.format("IGNORE: Unable to register new access mode on provided access group because mode is already registered on an access group for this link segment type (%s)", getXmlId()));
      return;
    }
    this.modeAccessProperties.put(accessMode, accessGroupProperties);
    accessGroupProperties.addAccessMode(accessMode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("[%d,%s,%s]", getId(), getXmlId(), getName());
  }

}
