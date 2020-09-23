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
 * the predefined train mode
 * <ul>
 * <li>name: train</li>
 * <li>maxspeed (km/h): 140</li>
 * <li>pcu: 10</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class TrainMode extends PredefinedModeImpl {

  /* default max speed value for train mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 140;

  /* default pcu value for train mode */
  public static final double DEFAULT_PCU = 10;

  /* default physical features of train (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures TRAIN_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of train (PUBLIC) */
  public static final UsabilityModeFeatures TRAIN_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for train mode
   * 
   * @param groupId to generate unique id
   */
  protected TrainMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.TRAIN, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, TRAIN_PHYSICAL_FEATURES, TRAIN_USABLITY_FEATURES);
  }

}
