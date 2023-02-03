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
 * the predefined train mode
 * <ul>
 * <li>name: train</li>
 * <li>maxspeed (km/h): 140</li>
 * <li>pcu: 10</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: RAIL</li>
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

  /* default physical features of train (VEHICLE, MOTORISED, RAIL) */
  public static final PhysicalModeFeatures TRAIN_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.RAIL);

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

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TrainMode(TrainMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TrainMode shallowClone() {
    return new TrainMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TrainMode deepClone() {
    return new TrainMode(this, true);
  }
}
