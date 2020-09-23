package org.planit.mode;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;

/**
 * the predefined car hov mode
 * <ul>
 * <li>name: car_hov</li>
 * <li>maxspeed (km/h): 130</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: HIGH_OCCUPANCY</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class CarHighOccupancyMode extends PredefinedModeImpl {

  /* default max speed value for car hov mode */
  public static final double DEFAULT_MAX_SPEED_KMH = CarMode.DEFAULT_MAX_SPEED_KMH;

  /* default pcu value for car hov mode */
  public static final double DEFAULT_PCU = CarMode.DEFAULT_PCU;

  /* default physical features of car hov (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures CAR_HOV_PHYSICAL_FEATURES = CarMode.CAR_PHYSICAL_FEATURES;

  /* default usability features of car hov (RIDE_SHARE) */
  public static final UsabilityModeFeatures CAR_HOV_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.HIGH_OCCUPANCY);

  /**
   * Constructor for car hov mode
   * 
   * @param groupId to generate unique id
   */
  protected CarHighOccupancyMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.CAR_HIGH_OCCUPANCY, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, CAR_HOV_PHYSICAL_FEATURES, CAR_HOV_USABLITY_FEATURES);
  }

}
