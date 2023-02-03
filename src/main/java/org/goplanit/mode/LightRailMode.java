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
 * the predefined lightrail mode
 * <ul>
 * <li>name: lightrail</li>
 * <li>maxspeed (km/h): 70</li>
 * <li>pcu: 6</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: RAIL</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class LightRailMode extends PredefinedModeImpl {

  /* default max speed value for light rail mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 70;

  /* default pcu value for lightrail mode */
  public static final double DEFAULT_PCU = 6;

  /* default physical features of lightrail (VEHICLE, MOTORISED, RAIL) */
  public static final PhysicalModeFeatures LIGHTRAIL_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED,
      TrackModeType.RAIL);

  /* default usability features of lightrail (PUBLIC) */
  public static final UsabilityModeFeatures LIGHTRAIL_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for lightrail mode
   * 
   * @param groupId to generate unique id
   */
  protected LightRailMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.LIGHTRAIL, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, LIGHTRAIL_PHYSICAL_FEATURES, LIGHTRAIL_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected LightRailMode(LightRailMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LightRailMode shallowClone() {
    return new LightRailMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LightRailMode deepClone() {
    return new LightRailMode(this, true);
  }
}
