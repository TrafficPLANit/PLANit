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
 * the predefined goods vehicle mode, i.e., truck below 3.5 tonnes
 * <ul>
 * <li>name: gv</li>
 * <li>maxspeed (km/h): 100</li>
 * <li>pcu: 1.8</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: GOODS</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class GoodsMode extends PredefinedModeImpl {

  /* default max speed value for goods mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 100;

  /* default pcu value for hgv mode */
  public static final double DEFAULT_PCU = 1.8;

  /* default physical features of gv (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures GV_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

  /* default usability features of gv (GOODS) */
  public static final UsabilityModeFeatures GV_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.GOODS);

  /**
   * Constructor for heavy goods vehicle mode
   * 
   * @param groupId to generate unique id
   */
  protected GoodsMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.GOODS_VEHICLE, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, GV_PHYSICAL_FEATURES, GV_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected GoodsMode(GoodsMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GoodsMode clone() {
    return new GoodsMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GoodsMode deepClone() {
    return new GoodsMode(this, true);
  }
}
