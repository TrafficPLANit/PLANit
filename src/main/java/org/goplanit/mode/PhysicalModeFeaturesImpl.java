package org.goplanit.mode;

import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.VehicularModeType;

/**
 * the physical features of a mode are listed by this class. Inspired by the categorisation as offered in open street maps as per
 * https://wiki.openstreetmap.org/wiki/Key:access#Transport_mode_restrictions
 * 
 * @author markr
 *
 */
public class PhysicalModeFeaturesImpl implements PhysicalModeFeatures {

  /** the vehicular type */
  private VehicularModeType vehicularType;

  /** the motorisation type */
  private MotorisationModeType motorisationType;

  /** the track type */
  private TrackModeType trackType;

  /**
   * set the vehicular type
   * 
   * @param vehicularType to use
   */
  protected void setVehicularType(VehicularModeType vehicularType) {
    this.vehicularType = vehicularType;
  }

  /**
   * set the motorisation type
   * 
   * @param motorisationType to use
   */
  protected void setMotorisationType(MotorisationModeType motorisationType) {
    this.motorisationType = motorisationType;
  }

  /**
   * set the track type to use
   * 
   * @param trackType to use
   */
  protected void setTrackType(TrackModeType trackType) {
    this.trackType = trackType;
  }

  /**
   * Default constructor
   */
  protected PhysicalModeFeaturesImpl() {
    this.vehicularType = DEFAULT_VEHICULAR_TYPE;
    this.motorisationType = DEFAULT_MOTORISATION_TYPE;
    this.trackType = DEFAULT_TRACK_TYPE;
  }

  /**
   * @param vehicularType    to use
   * @param motorisationType to use
   * @param trackType        to use
   */
  protected PhysicalModeFeaturesImpl(VehicularModeType vehicularType, MotorisationModeType motorisationType, TrackModeType trackType) {
    this.vehicularType = vehicularType;
    this.motorisationType = motorisationType;
    this.trackType = trackType;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  protected PhysicalModeFeaturesImpl(PhysicalModeFeaturesImpl other) {
    this.vehicularType = other.vehicularType;
    this.motorisationType = other.motorisationType;
    this.trackType = other.trackType;
  }

  /* getters - setters */

  @Override
  public VehicularModeType getVehicularType() {
    return vehicularType;
  }

  @Override
  public MotorisationModeType getMotorisationType() {
    return motorisationType;
  }

  @Override
  public TrackModeType getTrackType() {
    return trackType;
  }

}
