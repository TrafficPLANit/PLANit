package org.planit.network.layer.macroscopic;

import java.util.Collection;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;

/**
 * Create Mode specific properties for the macroscopic perspective on the supply side, i.e. on a link segment of a particular type
 * 
 * @author markr
 *
 */
public class MacroscopicModePropertiesFactory {

  /**
   * factory method
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @return created properties
   */
  public static MacroscopicModeProperties create(final double maxSpeedKmH, final double criticalSpeedKmH) {
    return new MacroscopicModePropertiesImpl(maxSpeedKmH, criticalSpeedKmH);
  }

  /**
   * factory method adopting default value for critical speed
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @return created properties
   */
  public static MacroscopicModeProperties create(final double maxSpeedKmH) {
    return create(maxSpeedKmH, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH);
  }

  /**
   * factory method adopting default values
   * 
   * @return created properties
   */
  public static MacroscopicModeProperties create() {
    return create(Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH);
  }

  /**
   * add mode properties for the passed in modes to the passed in link segment type where we cap the max and critical speed based on the minimum of the mode's maximum speed and the
   * osmway type's maximum speed
   * 
   * @param linkSegmentType to populate for
   * @param modesToAdd      to add
   * @param maxSpeedKmH     maxSpeed to set
   */
  public static void createOnLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType, final Collection<Mode> modesToAdd, final double maxSpeedKmH) {
    /* apply the way type's maximum speed to all modes, but for clarity already cap it to the mode's max speed if needed */
    for (Mode planitMode : modesToAdd) {
      createOnLinkSegmentType(linkSegmentType, planitMode, maxSpeedKmH);
    }
  }

  /**
   * add mode properties for the passed in modes to the passed in link segment type where we cap the max and critical speed based on the minimum of the mode's maximum speed and the
   * osmway type's maximum speed
   * 
   * @param linkSegmentType to populate for
   * @param modeToAdd       to add
   * @param maxSpeedKmH     maxSpeed to set
   */
  public static void createOnLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType, final Mode modeToAdd, final double maxSpeedKmH) {
    /* apply the way type's maximum speed to all modes, but for clarity already cap it to the mode's max speed if needed */
    double cappedMaxSpeed = Math.min(maxSpeedKmH, modeToAdd.getMaximumSpeedKmH());
    linkSegmentType.addModeProperties(modeToAdd, create(cappedMaxSpeed, cappedMaxSpeed));
  }

}
