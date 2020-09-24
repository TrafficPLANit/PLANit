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

}
