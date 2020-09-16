package org.planit.mode;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;

/**
 * factory clss to instantiate different (pre-specified) mode types. You can of course create your own modes. However, using the pre-specified modes makes it easier to interpret
 * and exhcnage projects/applications using these modes.
 * 
 * @author markr
 *
 */
public class ModeFactory {

  /**
   * create a car mode. A car has:
   * <ul>
   * <li>name: car</li>
   * <li>pcu: 1</li>
   * <li>vehicular type: VEHICULAR</li>
   * <li>motorisation: MOTORISED</li>
   * <li>track: DOUBLE_TRACK</li>
   * <li>use: PRIVATE</li>
   * </ul>
   * 
   * @return
   */
  public static Mode createPredefinedCarMode(IdGroupingToken groupId) {
    PhysicalModeFeatures physicalFeatures = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);
    UsabilityModeFeatures usedToFeatures = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

    ModeImpl theMode = new ModeImpl(groupId, CAR, 1 /* pcu */, physicalFeatures, usedToFeatures);
    return theMode;
  }

}
