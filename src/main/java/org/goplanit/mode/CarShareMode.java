package org.goplanit.mode;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.mode.UseOfModeType;

/**
 * the predefined car share mode
 * <ul>
 * <li>name: car</li>
 * <li>maxspeed (km/h): 130</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: RIDE_SHARE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class CarShareMode extends PredefinedModeImpl {

  /* default max speed value for car share mode */
  public static final double DEFAULT_MAX_SPEED_KMH = CarMode.DEFAULT_MAX_SPEED_KMH;

  /* default pcu value for car share mode */
  public static final double DEFAULT_PCU = CarMode.DEFAULT_PCU;

  /* default physical features of car share (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures CAR_SHARE_PHYSICAL_FEATURES = CarMode.CAR_PHYSICAL_FEATURES;

  /* default usability features of car share (RIDE_SHARE) */
  public static final UsabilityModeFeatures CAR_SHARE_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.RIDE_SHARE);

  /**
   * Constructor for car share mode
   * 
   * @param groupId to generate unique id
   */
  protected CarShareMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.CAR_SHARE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, CAR_SHARE_PHYSICAL_FEATURES, CAR_SHARE_USABLITY_FEATURES);
  }

}
