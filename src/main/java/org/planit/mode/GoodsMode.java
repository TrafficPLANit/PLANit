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
 * the predefined goods vehicle mode, i.e., truck below 3.5 tonnes
 * <ul>
 * <li>name: gv</li>
 * <li>pcu: 1.8</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: GOODS</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class GoodsMode extends PredefinedModeImpl {

  /* default pcu value for hgv mode */
  public static final double DEFAULT_PCU = 1.8;

  /* default physical features of gv (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures GV_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of gv (GOODS) */
  public static final UsabilityModeFeatures GV_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.GOODS);

  /**
   * Constructor for heavy goods vehicle mode
   * 
   * @param groupId to generate unique id
   */
  protected GoodsMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.GOODS_VEHICLE, DEFAULT_PCU, GV_PHYSICAL_FEATURES, GV_USABLITY_FEATURES);
  }

}
