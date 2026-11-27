package org.goplanit.test.mode;

import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.ModeFactory;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;

/**
 * Modes the tests share that no predefined mode type gives
 * <p>
 * No predefined mode is an emergency mode, so tests of who pays on a toll road, or of an emergency mode itself, make a
 * custom one, an ambulance, the same way everywhere.
 * </p>
 *
 * @author markr
 */
public final class ModeTestUtils {

  /** No instances */
  private ModeTestUtils() {
  }

  /**
   * Register an ambulance: a custom motorised road vehicle whose use is emergency, driven by an operator
   *
   * @param modeFactory to register it with
   * @return the ambulance mode
   */
  public static Mode registerAmbulance(ModeFactory modeFactory) {
    return modeFactory.registerNewCustomMode("ambulance", 100, 1,
        ModeFeaturesFactory.createPhysicalFeatures(
            VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD),
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.EMERGENCY, DriverType.OPERATOR));
  }
}
