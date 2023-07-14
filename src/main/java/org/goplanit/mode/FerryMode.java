package org.goplanit.mode;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.*;

/**
 * The predefined ferry mode
 * <ul>
 * <li>name: ferry</li>
 * <li>maxspeed (km/h): 20</li>
 * <li>pcu: 8</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: WATER</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class FerryMode extends PredefinedModeImpl {

  /* default max speed value for light rail mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 70;

  /* default pcu value for lightrail mode */
  public static final double DEFAULT_PCU = 6;

  /* default physical features of ferry (VEHICLE, MOTORISED, WATER) */
  public static final PhysicalModeFeatures FERRY_PHYSICAL_FEATURES =
      new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.WATER);

  /* default usability features of ferry (PUBLIC) */
  public static final UsabilityModeFeatures FERRY_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for ferry mode
   *
   * @param groupId to generate unique id
   */
  protected FerryMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.FERRY, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, FERRY_PHYSICAL_FEATURES, FERRY_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected FerryMode(FerryMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FerryMode shallowClone() {
    return new FerryMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FerryMode deepClone() {
    return new FerryMode(this, true);
  }
}
