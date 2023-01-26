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
 * the predefined car mode
 * <ul>
 * <li>name: car</li>
 * <li>maxspeed (km/h): 130</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class CarMode extends PredefinedModeImpl {

  /* default max speed value for car mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 130;

  /* default pcu value for car mode */
  public static final double DEFAULT_PCU = 1;

  /* default physical features of car (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures CAR_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

  /* default usability features of car (PRIVATE) */
  public static final UsabilityModeFeatures CAR_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for car mode
   * 
   * @param groupId to generate unique id
   */
  protected CarMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.CAR, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, CAR_PHYSICAL_FEATURES, CAR_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected CarMode(CarMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CarMode clone() {
    return new CarMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CarMode deepClone() {
    return new CarMode(this, true);
  }

}
