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
 * the predefined tram mode
 * <ul>
 * <li>name: tram</li>
 * <li>maxspeed (km/h): 40</li>
 * <li>pcu: 3</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class TramMode extends PredefinedModeImpl {

  /* default max speed value for tram mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 40;

  /* default pcu value for tram mode */
  public static final double DEFAULT_PCU = 3;

  /* default physical features of tram (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures TRAM_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of tram (PUBLIC) */
  public static final UsabilityModeFeatures TRAM_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for train mode
   * 
   * @param groupId to generate unique id
   */
  protected TramMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.TRAM, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, TRAM_PHYSICAL_FEATURES, TRAM_USABLITY_FEATURES);
  }

}
