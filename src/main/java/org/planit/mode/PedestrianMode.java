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
 * the predefined pedestrian mode
 * <ul>
 * <li>name: pedestrian</li>
 * <li>pcu: 0.1</li>
 * <li>vehicular type: NO_VEHICLE</li>
 * <li>motorisation: NON_MOTORISED</li>
 * <li>track: SINGLE_TRACK</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class PedestrianMode extends PredefinedModeImpl {

  /* default pcu value for pedestrian mode */
  public static final double DEFAULT_PCU = 0.1;

  /* default physical features of pedestrian (NO_VEHICLE, NON_MOTORISED, SINGLE_TRACK) */
  public static final PhysicalModeFeatures PEDESTRIAN_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.NO_VEHICLE, MotorisationModeType.NON_MOTORISED,
      TrackModeType.SINGLE);

  /* default usability features of pedestrian (PRIVATE) */
  public static final UsabilityModeFeatures PEDESTRIAN_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PRIVATE);

  /**
   * Constructor for pedestrian mode
   * 
   * @param groupId to generate unique id
   */
  protected PedestrianMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.PEDESTRIAN, DEFAULT_PCU, PEDESTRIAN_PHYSICAL_FEATURES, PEDESTRIAN_USABLITY_FEATURES);
  }

}
