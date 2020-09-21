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
 * the predefined heavy goods vehicle mode, i.e., small truck up to 3.5 tonnes
 * <ul>
 * <li>name: hgv</li>
 * <li>pcu: 2</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: DOUBLE_TRACK</li>
 * <li>use: GOODS</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class HeavyGoodsMode extends PredefinedModeImpl {

  /* default pcu value for hgv mode */
  public static final double DEFAULT_PCU = 2;

  /* default physical features of hgv (VEHICLE, MOTORISED, DOUBLE_TRACK) */
  public static final PhysicalModeFeatures HGV_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.DOUBLE);

  /* default usability features of hgv (GOODS) */
  public static final UsabilityModeFeatures HGV_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.GOODS);

  /**
   * Constructor for heavy goods vehicle mode
   * 
   * @param groupId to generate unique id
   */
  protected HeavyGoodsMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.HEAVY_GOODS_VEHICLE, DEFAULT_PCU, HGV_PHYSICAL_FEATURES, HGV_USABLITY_FEATURES);
  }

}
