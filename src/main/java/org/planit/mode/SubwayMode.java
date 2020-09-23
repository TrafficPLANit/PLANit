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
 * the predefined subway mode
 * <ul>
 * <li>name: subway</li>
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
public class SubwayMode extends PredefinedModeImpl {

  /* default pcu value for subway mode */
  public static final double DEFAULT_PCU = 6;

  /* default physical features of subway (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures SUBWAY_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of subway (PUBLIC) */
  public static final UsabilityModeFeatures SUBWAY_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for subway mode
   * 
   * @param groupId to generate unique id
   */
  protected SubwayMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.SUBWAY, DEFAULT_PCU, SUBWAY_PHYSICAL_FEATURES, SUBWAY_USABLITY_FEATURES);
  }

}
