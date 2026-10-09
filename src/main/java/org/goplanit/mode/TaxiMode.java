package org.goplanit.mode;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.*;

/**
 * the predefined taxi mode
 * <ul>
 * <li>name: taxi</li>
 * <li>maxspeed (km/h): 130</li>
 * <li>pcu: 1</li>
 * <li>vehicular type: VEHICULAR</li>
 * <li>motorisation: MOTORISED</li>
 * <li>track: ROAD</li>
 * <li>use: PRIVATE</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class TaxiMode extends PredefinedModeImpl {

  /** default max speed value  */
  public static final double DEFAULT_MAX_SPEED_KMH = 130;

  /** default pcu value for taxi */
  public static final double DEFAULT_PCU = 1;

  /** default physical features of taxi  (VEHICLE, MOTORISED, ROAD) */
  public static final PhysicalModeFeatures TAXI_PHYSICAL_FEATURES =
          new PhysicalModeFeaturesImpl(VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);

  /** default usability features of taxi  (TAXI) */
  public static final UsabilityModeFeatures TAXI_USABLITY_FEATURES =
          new UsabilityModeFeaturesImpl(UseOfModeType.TAXI);

  /**
   * Constructor for taxi mode
   *
   * @param groupId to generate unique id
   */
  protected TaxiMode(IdGroupingToken groupId) {
    super(groupId, PredefinedModeType.TAXI, DEFAULT_MAX_SPEED_KMH, DEFAULT_PCU, TAXI_PHYSICAL_FEATURES,
            TAXI_USABLITY_FEATURES);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TaxiMode(TaxiMode other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TaxiMode shallowClone() {
    return new TaxiMode(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TaxiMode deepClone() {
    return new TaxiMode(this, true);
  }

}
