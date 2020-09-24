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
 * the predefined bus mode
 * <ul>
 * <li>name: bus</li>
 * <li>maxspeed (km/h): 100</li>
 * <li>pcu: 2</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class BusMode extends PredefinedModeImpl {

  /* default max speed value for bus mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 100;

  /* default pcu value for bus mode */
  public static final double DEFAULT_PCU = 2;

  /* default physical features of bus (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures BUS_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

  /* default usability features of bus (PUBLIC) */
  public static final UsabilityModeFeatures BUS_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for bus mode
   * 
   * @param groupId to generate unique id
   */
  protected BusMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.BUS, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, BUS_PHYSICAL_FEATURES, BUS_USABLITY_FEATURES);
  }

}
