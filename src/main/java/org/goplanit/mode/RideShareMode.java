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
public class RideShareMode extends PredefinedModeImpl {

  /** default max speed value for ride share mode */
  public static final double DEFAULT_MAX_SPEED_KMH = CarMode.DEFAULT_MAX_SPEED_KMH;

  /** default pcu value for ride share mode */
  public static final double DEFAULT_PCU = CarMode.DEFAULT_PCU;

  /** default physical features of ride share (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures RIDE_SHARE_PHYSICAL_FEATURES = CarMode.CAR_PHYSICAL_FEATURES;

  /** default usability features of ride-share (RIDE_SHARE) different to taxi as it may be shared payment with others*/
  public static final UsabilityModeFeatures RIDE_SHARE_USABLITY_FEATURES =
          new UsabilityModeFeaturesImpl(UseOfModeType.RIDE_SHARE);

  /**
   * Constructor for ride-share mode
   *
   * @param groupId to generate unique id
   */
  protected RideShareMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.RIDE_SHARE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU,
        RIDE_SHARE_PHYSICAL_FEATURES, RIDE_SHARE_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected RideShareMode(RideShareMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RideShareMode shallowClone() {
    return new RideShareMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RideShareMode deepClone() {
    return new RideShareMode(this, true);
  }
}
