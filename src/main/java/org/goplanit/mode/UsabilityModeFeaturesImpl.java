package org.goplanit.mode;

import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.mode.UseOfModeType;

/**
 * Class to highlight the use of a particular mode, i.e., is it a public or private mode, ride-share, etc.
 * Inspired by the categorisation as offered in open street maps as per
 * https://wiki.openstreetmap.org/wiki/Key:access#Transport_mode_restrictions
 * 
 * @author markr
 *
 */
public class UsabilityModeFeaturesImpl implements UsabilityModeFeatures {

  /** the use of the type */
  protected UseOfModeType useOfType;

  /** who drives the mode, null when not known */
  protected DriverType driverType;

  /**
   * set the use of type
   * 
   * @param useOfType to use
   */
  protected void setUseOfType(UseOfModeType useOfType) {
    this.useOfType = useOfType;
  }

  /**
   * Default constructor
   */
  protected UsabilityModeFeaturesImpl() {
    this(DEFAULT_USEOF_TYPE);
  }

  /**
   * Constructor, who drives the mode not known
   *
   * @param useOfType to use
   */
  protected UsabilityModeFeaturesImpl(UseOfModeType useOfType) {
    this(useOfType, null);
  }

  /**
   * Constructor
   *
   * @param useOfType to use
   * @param driverType who drives the mode, null when not known
   */
  protected UsabilityModeFeaturesImpl(UseOfModeType useOfType, DriverType driverType) {
    this.useOfType = useOfType;
    this.driverType = driverType;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  protected UsabilityModeFeaturesImpl(UsabilityModeFeaturesImpl other) {
    this(other.useOfType, other.driverType);
  }

  /* getters - setters */

  /**
   * {@inheritDoc}
   */
  @Override
  public UseOfModeType getUseOfType() {
    return useOfType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DriverType getDriverType() {
    return driverType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UsabilityModeFeatures shallowClone() {
    return new UsabilityModeFeaturesImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UsabilityModeFeatures deepClone() {
    return shallowClone(); // nothing to deep copy yet
  }

}
