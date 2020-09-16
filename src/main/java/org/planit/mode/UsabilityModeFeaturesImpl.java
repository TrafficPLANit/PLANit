package org.planit.mode;

import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;

/**
 * Class to highlight the use of a particular mode, i.e., is it a public or private mode, ride-share, etc. Inspired by the categorisation as offered in open street maps as per
 * https://wiki.openstreetmap.org/wiki/Key:access#Transport_mode_restrictions
 * 
 * @author markr
 *
 */
public class UsabilityModeFeaturesImpl implements UsabilityModeFeatures {

  /** the use of the type */
  protected UseOfModeType useOfType;

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
   * Constructor
   * 
   * @param useOfType to use
   */
  protected UsabilityModeFeaturesImpl(UseOfModeType useOfType) {
    this.useOfType = useOfType;
  }

  /* getters - setters */

  /**
   * {@inheritDoc}
   */
  @Override
  public UseOfModeType getUseOfType() {
    return useOfType;
  }

}
