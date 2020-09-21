package org.planit.mode;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;

/**
 * the predefined car share mode
 * <ul>
 * <li>name: car</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: RIDE_SHARE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class CarShareMode extends PredefinedModeImpl {

  /* default pcu value for car share mode */
  public static final double DEFAULT_PCU = CarMode.DEFAULT_PCU;

  /* default physical features of car share (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures CAR_SHARE_PHYSICAL_FEATURES = CarMode.CAR_PHYSICAL_FEATURES;

  /* default usability features of car share (RIDE_SHARE) */
  public static final UsabilityModeFeatures CAR_SHARE_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.RIDE_SHARE);

  /**
   * Constructor for car share mode
   * 
   * @param groupId to generate unique id
   */
  protected CarShareMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.CAR_SHARE, DEFAULT_PCU, CAR_SHARE_PHYSICAL_FEATURES, CAR_SHARE_USABLITY_FEATURES);
  }

}
