package org.planit.mode;

import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;

/**
 * Factory class to create physical and usability features for custom modes
 * 
 * @author markr
 *
 */
public class ModeFeaturesFactory {

  /**
   * create physical mode features
   * 
   * @param vehicleType      to use
   * @param motorisationType to use
   * @param trackType        to use
   * @returnphysical mode features that are created
   */
  public static PhysicalModeFeatures createPhysicalFeatures(VehicularModeType vehicleType, MotorisationModeType motorisationType, TrackModeType trackType) {
    return new PhysicalModeFeaturesImpl(vehicleType, motorisationType, trackType);
  }

  /**
   * create usability mode features
   * 
   * @param useOfModeType to use
   * @return usability mode features that are created
   */
  public static UsabilityModeFeatures createUsabilityFeatures(UseOfModeType useOfModeType) {
    return new UsabilityModeFeaturesImpl(useOfModeType);
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
   * create default physical mode features (VEHICLE, MOTORISED, DOUBLE (track))
   * 
   * @return usability mode features that are created
   */
  public static PhysicalModeFeatures createDefaultPhysicalFeatures() {
    return new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);
  }
}
