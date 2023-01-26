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
 * the predefined large heavy goods vehicle mode, i.e., large articulated truck, larger than heavy goods
 * <ul>
 * <li>name: lhgv</li>
 * <li>maxspeed (km/h): 90</li>
 * <li>pcu: 3</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: GOODS</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class LargeHeavyGoodsMode extends PredefinedModeImpl {

  /* default max speed value for lhgv mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 90;

  /* default pcu value for lhgv mode */
  public static final double DEFAULT_PCU = 3;

  /* default physical features of lhgv (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures LHGV_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

  /* default usability features of lhgv (GOODS) */
  public static final UsabilityModeFeatures LHGV_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.GOODS);

  /**
   * Constructor for large heavy goods vehicle mode
   * 
   * @param groupId to generate unique id
   */
  protected LargeHeavyGoodsMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.LARGE_HEAVY_GOODS_VEHICLE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, LHGV_PHYSICAL_FEATURES, LHGV_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected LargeHeavyGoodsMode(LargeHeavyGoodsMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LargeHeavyGoodsMode clone() {
    return new LargeHeavyGoodsMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LargeHeavyGoodsMode deepClone() {
    return new LargeHeavyGoodsMode(this, true);
  }
}
