package org.planit.network.macroscopic.physical;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

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
   */
  public static MacroscopicModeProperties create(final double maxSpeedKmH, final double criticalSpeedKmH) {
    return new MacroscopicModePropertiesImpl(maxSpeedKmH, criticalSpeedKmH);
  }

  /**
   * factory method adopting default value for critical speed
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   */
  public static MacroscopicModeProperties create(final double maxSpeedKmH) {
    return create(maxSpeedKmH, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH);
  }

  /**
   * factory method adopting default values
   */
  public static MacroscopicModeProperties create() {
    return create(Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH);
  }

}
