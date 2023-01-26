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
 * the predefined bicycle mode
 * <ul>
 * <li>name: bicycle</li>
 * <li>maxspeed (km/h): 15</li>
 * <li>pcu: 0.2</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: NON_MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class BicycleMode extends PredefinedModeImpl {

  /* default max speed value for bicycle mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 15;

  /* default pcu value for bicycle mode */
  public static final double DEFAULT_PCU = 0.2;

  /* default physical features of bicycle (VEHICLE, NON_MOTORISED, ROAD) */
  public static final PhysicalModeFeatures BICYCLE_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.NON_MOTORISED,
      TrackModeType.ROAD);

  /* default usability features of bicycle (PRIVATE) */
  public static final UsabilityModeFeatures BICYCLE_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for bicycle mode
   * 
   * @param groupId to generate unique id
   */
  protected BicycleMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.BICYCLE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, BICYCLE_PHYSICAL_FEATURES, BICYCLE_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected BicycleMode(BicycleMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BicycleMode clone() {
    return new BicycleMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BicycleMode deepClone() {
    return new BicycleMode(this, true);
  }
}
