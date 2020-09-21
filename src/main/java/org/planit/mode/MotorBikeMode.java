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
 * the predefined motor bike mode
 * <ul>
 * <li>name: bicycle</li>
 * <li>pcu: 0.5</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: NON_MOTORISED</li>
 * <li>track: SINGLE_TRACK</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class MotorBikeMode extends PredefinedModeImpl {

  /* default pcu value for motor bike mode */
  public static final double DEFAULT_PCU = 0.5;

  /* default physical features of motor bike (VEHICLE, MOTORISED, SINGLE_TRACK) */
  public static final PhysicalModeFeatures MOTOR_BIKE_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED,
      TrackModeType.SINGLE);

  /* default usability features of motor bike (PRIVATE) */
  public static final UsabilityModeFeatures MOTOR_BIKE_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for motor bike mode
   * 
   * @param groupId to generate unique id
   */
  protected MotorBikeMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.MOTOR_BIKE, DEFAULT_PCU, MOTOR_BIKE_PHYSICAL_FEATURES, MOTOR_BIKE_USABLITY_FEATURES);
  }

}
