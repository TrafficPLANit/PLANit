package org.planit.network.layer.macroscopic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.AccessGroupProperties;

/**
 * Group of modes with specific properties for the macroscopic perspective on the supply side, i.e. on a link segment of a particular type
 * 
 * @author markr
 *
 */
public class AccessGroupPropertiesImpl implements AccessGroupProperties {

  /** Maximum speed of mode (tied to a road segment) in km/h */
  protected double maxSpeedKmH;

  /** Maximum speed of mode (tied to a road segment) in km/h */
  protected double criticalSpeedKmH;

  /** modes supported by this access group */
  protected final Set<Mode> supportedModes;

  // Public

  /**
   * Constructor
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @param accessModes      supported by these properties
   */
  AccessGroupPropertiesImpl(final double maxSpeedKmH, final double criticalSpeedKmH, final Collection<Mode> accessModes) {
    super();
    this.maxSpeedKmH = maxSpeedKmH;
    this.criticalSpeedKmH = criticalSpeedKmH;
    this.supportedModes = new TreeSet<Mode>(accessModes);
  }

  /**
   * Constructor
   * 
   * @param maxSpeedKmH      maximum speed for this mode in this context
   * @param criticalSpeedKmH critical speed for this mode in this context
   * @param accessModes      supported by these properties
   */
  AccessGroupPropertiesImpl(final double maxSpeedKmH, final double criticalSpeedKmH, final Mode... accessModes) {
    super();
    this.maxSpeedKmH = maxSpeedKmH;
    this.criticalSpeedKmH = criticalSpeedKmH;
    this.supportedModes = new TreeSet<Mode>(Arrays.asList(accessModes));
  }

  // Getter - setters

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public AccessGroupPropertiesImpl(final AccessGroupPropertiesImpl other) {
    this.maxSpeedKmH = other.maxSpeedKmH;
    this.criticalSpeedKmH = other.criticalSpeedKmH;
    this.supportedModes = new TreeSet<Mode>(other.supportedModes);
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
  public void setMaximumSpeedKmH(final double maxSpeedKmH) {
    this.maxSpeedKmH = maxSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCriticalSpeedKmH(final double criticalSpeed) {
    this.criticalSpeedKmH = criticalSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AccessGroupProperties clone() {
    return new AccessGroupPropertiesImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAccessModes() {
    return Collections.unmodifiableSet(this.supportedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeAccessMode(Mode toBeRemovedMode) {
    return this.supportedModes.remove(toBeRemovedMode);
  }

}
