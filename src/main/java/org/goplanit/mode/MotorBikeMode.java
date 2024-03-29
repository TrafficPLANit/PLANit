package org.goplanit.mode;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;

/**
 * the predefined motor bike mode
 * <ul>
 * <li>name: bicycle</li>
 * <li>maxspeed (km/h): 130</li>
 * <li>pcu: 0.5</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: NON_MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class MotorBikeMode extends PredefinedModeImpl {

  /* default max speed value for motor bike mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 130;

  /* default pcu value for motor bike mode */
  public static final double DEFAULT_PCU = 0.5;

  /* default physical features of motor bike (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures MOTOR_BIKE_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED,
      TrackModeType.ROAD);

  /* default usability features of motor bike (PRIVATE) */
  public static final UsabilityModeFeatures MOTOR_BIKE_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for motor bike mode
   * 
   * @param groupId to generate unique id
   */
  protected MotorBikeMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.MOTOR_BIKE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, MOTOR_BIKE_PHYSICAL_FEATURES, MOTOR_BIKE_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MotorBikeMode(MotorBikeMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MotorBikeMode shallowClone() {
    return new MotorBikeMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MotorBikeMode deepClone() {
    return new MotorBikeMode(this, true);
  }
}
