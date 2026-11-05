package org.goplanit.mode;

import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;

/**
 * Factory class to create physical and usability features for custom modes
 * 
 * @author markr
 *
 */
public class ModeFeaturesFactory {

  /** Constructor to disallow instance creation */
  private ModeFeaturesFactory(){}

  /**
   * create physical mode features
   * 
   * @param vehicleType      to use
   * @param motorisationType to use
   * @param trackType        to use
   * @return physical mode features that are created
   */
  public static PhysicalModeFeatures createPhysicalFeatures(
      VehicularModeType vehicleType, MotorisationModeType motorisationType, TrackModeType trackType) {
    return new PhysicalModeFeaturesImpl(vehicleType, motorisationType, trackType);
  }

  /**
   * create usability mode features, who drives the mode not known
   *
   * @param useOfModeType to use
   * @return usability mode features that are created
   */
  public static UsabilityModeFeatures createUsabilityFeatures(UseOfModeType useOfModeType) {
    return createUsabilityFeatures(useOfModeType, null);
  }

  /**
   * create usability mode features
   *
   * @param useOfModeType to use
   * @param driverType who drives the mode, null when not known
   * @return usability mode features that are created
   */
  public static UsabilityModeFeatures createUsabilityFeatures(UseOfModeType useOfModeType, DriverType driverType) {
    return new UsabilityModeFeaturesImpl(useOfModeType, driverType);
  }

  /**
   * create default usability mode features (PRIVATE)
   * 
   * @return usability mode features that are created
   */
  public static UsabilityModeFeatures createDefaultUsabilityFeatures() {
    return new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);
  }

  /**
   * create default physical mode features (VEHICLE, MOTORISED, ROAD)
   * 
   * @return usability mode features that are created
   */
  public static PhysicalModeFeatures createDefaultPhysicalFeatures() {
    return new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);
  }
}
