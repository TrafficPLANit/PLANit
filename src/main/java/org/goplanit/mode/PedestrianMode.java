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
 * the predefined pedestrian mode
 * <ul>
 * <li>name: pedestrian</li>
 * <li>maxspeed (km/h): 5</li>
 * <li>pcu: 0.1</li>
 * <li>vehicular type: NO_VEHICLE</li>
 * <li>motorisation: NON_MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class PedestrianMode extends PredefinedModeImpl {

  /* default max speed value for pedestrian mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 5;

  /* default pcu value for pedestrian mode */
  public static final double DEFAULT_PCU = 0.1;

  /* default physical features of pedestrian (NO_VEHICLE, NON_MOTORISED, ROAD) */
  public static final PhysicalModeFeatures PEDESTRIAN_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.NO_VEHICLE, MotorisationModeType.NON_MOTORISED,
      TrackModeType.ROAD);

  /* default usability features of pedestrian (PRIVATE) */
  public static final UsabilityModeFeatures PEDESTRIAN_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for pedestrian mode
   * 
   * @param groupId to generate unique id
   */
  protected PedestrianMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.PEDESTRIAN, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, PEDESTRIAN_PHYSICAL_FEATURES, PEDESTRIAN_USABLITY_FEATURES);
  }

}
