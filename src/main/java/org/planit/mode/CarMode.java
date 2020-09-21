package org.planit.mode;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;

/**
 * the predefined car mode
 * <ul>
 * <li>name: car</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class CarMode extends PredefinedModeImpl {

  /* default pcu value for car mode */
  public static final double DEFAULT_PCU = 1;

  /* default physical features of car (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures CAR_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of car (PRIVATE) */
  public static final UsabilityModeFeatures CAR_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for car mode
   * 
   * @param groupId to generate unique id
   */
  protected CarMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.CAR, DEFAULT_PCU, CAR_PHYSICAL_FEATURES, CAR_USABLITY_FEATURES);
  }

}
