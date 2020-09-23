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
 * the predefined lightrail mode
 * <ul>
 * <li>name: lightrail</li>
 * <li>maxspeed (km/h): 70</li>
 * <li>pcu: 6</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
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

  /* default physical features of lightrail (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures LIGHTRAIL_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED,
      TrackModeType.DOUBLE);

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

}
