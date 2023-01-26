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
 * the predefined subway mode
 * <ul>
 * <li>name: subway</li>
 * <li>maxspeed (km/h): 60</li>
 * <li>pcu: 6</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: RAIL</li>
 * <li>use: PUBLIC</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class SubwayMode extends PredefinedModeImpl {

  /* default max speed value for subway mode */
  public static final double DEFAULT_MAX_SPEED_KMH = 60;

  /* default pcu value for subway mode */
  public static final double DEFAULT_PCU = 6;

  /* default physical features of subway (VEHICLE, MOTORISED, RAIL) */
  public static final PhysicalModeFeatures SUBWAY_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.RAIL);

  /* default usability features of subway (PUBLIC) */
  public static final UsabilityModeFeatures SUBWAY_USABLITY_FEATURES = new UsabilityModeFeaturesImpl(UseOfModeType.PUBLIC);

  /**
   * Constructor for subway mode
   * 
   * @param groupId to generate unique id
   */
  protected SubwayMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.SUBWAY, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, SUBWAY_PHYSICAL_FEATURES, SUBWAY_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected SubwayMode(SubwayMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SubwayMode clone() {
    return new SubwayMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SubwayMode deepClone() {
    return new SubwayMode(this, true);
  }
}
