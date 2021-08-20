package org.planit.network.layer.macroscopic;

import java.util.Collection;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Create Mode specific access properties to be used on macroscopic link segment types
 * 
 * @author markr
 *
 */
public class LinkSegmentTypeAccessPropertiesFactory {

  /**
   * Factory method
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @param accessModes      these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final double maxSpeedKmH, final double criticalSpeedKmH, final Mode... accessModes) {
    return new AccessGroupPropertiesImpl(maxSpeedKmH, criticalSpeedKmH, accessModes);
  }

  /**
   * Factory method
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @param accessModes      these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final double maxSpeedKmH, final double criticalSpeedKmH, final Collection<Mode> accessModes) {
    return new AccessGroupPropertiesImpl(maxSpeedKmH, criticalSpeedKmH, accessModes);
  }

  /**
   * Factory method adopting default value for critical speed
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final double maxSpeedKmH, final Mode... accessModes) {
    return create(maxSpeedKmH, AccessGroupProperties.DEFAULT_CRITICAL_SPEED_KMH, accessModes);
  }

  /**
   * Factory method adopting default value for critical speed
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final double maxSpeedKmH, final Collection<Mode> accessModes) {
    return create(maxSpeedKmH, AccessGroupProperties.DEFAULT_CRITICAL_SPEED_KMH, accessModes);
  }

  /**
   * Factory method adopting default values
   * 
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final Mode... accessModes) {
    return create(Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH, AccessGroupProperties.DEFAULT_CRITICAL_SPEED_KMH, accessModes);
  }

  /**
   * Factory method adopting default values
   * 
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final Collection<Mode> accessModes) {
    return create(Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH, AccessGroupProperties.DEFAULT_CRITICAL_SPEED_KMH, accessModes);
  }

  /**
   * Add mode properties for the passed in modes to the passed in link segment type where we cap the max and critical speed based on the minimum of the mode's maximum speed and the
   * maximum speed
   * 
   * @param linkSegmentType to populate for
   * @param modesToAdd      to add
   * @param maxSpeedKmH     maxSpeed to set
   */
  public static void createOnLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType, final double maxSpeedKmH, final Collection<Mode> modesToAdd) {
    linkSegmentType.setAccessProperties(create(maxSpeedKmH, modesToAdd));
  }

  /**
   * Add access properties for the passed in modes to the passed in link segment type where we cap the max and critical speed based on the minimum of the mode's maximum speed and
   * the maximum speed
   * 
   * @param linkSegmentType to populate for
   * @param modeToAdd       to add
   * @param maxSpeedKmH     maxSpeed to set
   */
  public static void createOnLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType, final Mode modeToAdd, final double maxSpeedKmH) {
    /* apply the way type's maximum speed to all modes, but for clarity already cap it to the mode's max speed if needed */
    double cappedMaxSpeed = Math.min(maxSpeedKmH, modeToAdd.getMaximumSpeedKmH());
    linkSegmentType.setAccessProperties(create(cappedMaxSpeed, cappedMaxSpeed, modeToAdd));
  }

}
