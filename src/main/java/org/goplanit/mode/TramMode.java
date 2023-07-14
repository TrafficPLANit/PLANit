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
 * the predefined tram mode
 * <ul>
 * <li>name: tram</li>
 * <li>maxspeed (km/h): 40</li>
 * <li>pcu: 3</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: RAIL</li>
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

  /* default physical features of tram (VEHICLE, MOTORISED, RAIL) */
  public static final PhysicalModeFeatures TRAM_PHYSICAL_FEATURES = new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.RAIL);

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

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TramMode(TramMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TramMode shallowClone() {
    return new TramMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TramMode deepClone() {
    return new TramMode(this, true);
  }
}
