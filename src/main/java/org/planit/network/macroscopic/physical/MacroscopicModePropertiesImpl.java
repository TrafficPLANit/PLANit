package org.planit.network.macroscopic.physical;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Mode specific properties for the macroscopic perspective on the supply side, i.e. on a link segment of a particular type
 * 
 * @author markr
 *
 */
public class MacroscopicModePropertiesImpl implements MacroscopicModeProperties {

  /**
   * Maximum speed of mode (tied to a road segment) in km/h
   */
  protected final double maxSpeedKmH;

  /**
   * Maximum speed of mode (tied to a road segment) in km/h
   */
  protected final double criticalSpeedKmH;

  // Public

  /**
   * Constructor
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   */
  MacroscopicModePropertiesImpl(final double maxSpeedKmH, final double criticalSpeedKmH) {
    super();
    this.maxSpeedKmH = maxSpeedKmH;
    this.criticalSpeedKmH = criticalSpeedKmH;
  }

  /**
   * Constructor adopting default values
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   */
  MacroscopicModePropertiesImpl(final double maxSpeedKmH) {
    this(maxSpeedKmH, DEFAULT_CRITICAL_SPEED_KMH);
  }

  /**
   * Constructor adopting default values
   */
  MacroscopicModePropertiesImpl() {
    this(Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH, DEFAULT_CRITICAL_SPEED_KMH);
  }

  // Getter - setters

  /**
   * Copy constructor 
   * 
   * @param other to copy
   */
  public MacroscopicModePropertiesImpl(final MacroscopicModePropertiesImpl other) {
    this.maxSpeedKmH = other.maxSpeedKmH;
    this.criticalSpeedKmH = other.criticalSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getMaximumSpeedKmH() {
    return maxSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCriticalSpeedKmH() {
    return criticalSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double setMaximumSpeedKmH(final double maxSpeed) {
    return this.maxSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double setCriticalSpeedKmH(final double criticalSpeed) {
    return this.criticalSpeedKmH;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override  
  public MacroscopicModeProperties clone() {
    return new MacroscopicModePropertiesImpl(this);    
  }

}
