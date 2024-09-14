package org.goplanit.network.layer.macroscopic;

import java.util.Arrays;
import java.util.Collection;

import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Create Mode specific access properties to be used on macroscopic link segment types
 * 
 * @author markr
 *
 */
public class AccessGroupPropertiesFactory {

  /**
   * Factory method
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @param accessModes      these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(
      final double maxSpeedKmH, final double criticalSpeedKmH, final Mode... accessModes) {
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
  public static AccessGroupProperties create(
      final double maxSpeedKmH, final double criticalSpeedKmH, final Collection<Mode> accessModes) {
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
    return new AccessGroupPropertiesImpl(maxSpeedKmH, accessModes);
  }

  /**
   * Factory method adopting saem value for maximum and critical speed
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final double maxSpeedKmH, final Collection<Mode> accessModes) {
    return new AccessGroupPropertiesImpl(maxSpeedKmH, accessModes);
  }

  /**
   * Factory method allowing access for given modes without any further specification on their speeds
   * 
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final Mode... accessModes) {
    return create(Arrays.asList(accessModes));
  }

  /**
   * Factory method allowing access for given modes without any further specification on their speeds
   * 
   * @param accessModes these properties relate to
   * @return created properties
   */
  public static AccessGroupProperties create(final Collection<Mode> accessModes) {
    return new AccessGroupPropertiesImpl(accessModes);
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
    linkSegmentType.setAccessGroupProperties(create(maxSpeedKmH, modesToAdd));
  }

  /**
   * Add access properties for the passed in modes to the passed in link segment type where we cap the max and critical speed based on the minimum of the mode's maximum speed and
   * the maximum speed
   * 
   * @param linkSegmentType to populate for
   * @param modeToAdd       to add
   * @param maxSpeedKmH     maxSpeed to set, if exceeding mode maximum speed, only access for the mode is registered as the provided speed is not a restriction compared to the
   *                        physical restriction
   */
  public static void createOnLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType, final Mode modeToAdd, final double maxSpeedKmH) {
    if (Precision.greater(maxSpeedKmH, modeToAdd.getMaximumSpeedKmH())) {
      create(modeToAdd); //todo: this does not seem to do anything...
    } else {
      linkSegmentType.setAccessGroupProperties(create(maxSpeedKmH, modeToAdd));
    }
  }

}
