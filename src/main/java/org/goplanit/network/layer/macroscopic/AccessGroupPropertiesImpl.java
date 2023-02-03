package org.goplanit.network.layer.macroscopic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;

/**
 * Group of modes with specific properties for the macroscopic perspective on the supply side, i.e. on a link segment of a particular type. While the group specifies the allowed
 * modes, it is not compulsory to define restricted maximum and or critical speeds. When absent context of the mode and links is to be used to determine the applied maximum speeds.
 * 
 * @author markr
 *
 */
public class AccessGroupPropertiesImpl implements AccessGroupProperties {

  /** Maximum speed of mode (tied to a road segment type) in km/h */
  protected Double maxSpeedKmH;

  /** Maximum speed of mode (tied to a road segment type) in km/h */
  protected Double criticalSpeedKmH;

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
    this.supportedModes = new TreeSet<>(accessModes);
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

  /**
   * Constructor
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @param accessModes supported by these properties
   */
  AccessGroupPropertiesImpl(final double maxSpeedKmH, final Collection<Mode> accessModes) {
    super();
    this.maxSpeedKmH = maxSpeedKmH;
    this.criticalSpeedKmH = null;
    this.supportedModes = new TreeSet<Mode>(accessModes);
  }

  /**
   * Constructor
   * 
   * @param maxSpeedKmH maximum speed for this mode in this context
   * @param accessModes supported by these properties
   */
  AccessGroupPropertiesImpl(final double maxSpeedKmH, final Mode... accessModes) {
    super();
    this.maxSpeedKmH = maxSpeedKmH;
    this.criticalSpeedKmH = null;
    this.supportedModes = new TreeSet<>(Arrays.asList(accessModes));
  }

  /**
   * access properties with only defining allowed modes without setting any restrictive speeds compared to the physical speed on the links it is applied on
   * 
   * @param accessModes to allow
   */
  public AccessGroupPropertiesImpl(Collection<Mode> accessModes) {
    this.criticalSpeedKmH = null;
    this.maxSpeedKmH = null;
    this.supportedModes = new TreeSet<>(accessModes);
  }

  // Getter - setters

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public AccessGroupPropertiesImpl(final AccessGroupPropertiesImpl other, boolean deepCopy /* no impact yet */) {
    super();
    this.maxSpeedKmH = other.maxSpeedKmH;
    this.criticalSpeedKmH = other.criticalSpeedKmH;
    this.supportedModes = new TreeSet<>(other.supportedModes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getMaximumSpeedKmH() {
    return maxSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getCriticalSpeedKmH() {
    return criticalSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaximumSpeedKmH(final Double maxSpeedKmH) {
    this.maxSpeedKmH = maxSpeedKmH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCriticalSpeedKmH(final Double criticalSpeed) {
    this.criticalSpeedKmH = criticalSpeed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AccessGroupProperties shallowClone() {
    return new AccessGroupPropertiesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AccessGroupProperties deepClone() {
    return new AccessGroupPropertiesImpl(this, true);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEqualExceptForModes(AccessGroupProperties accessProperties) {
    boolean maxSpeedEqual = false;
    if(getMaximumSpeedKmH() ==null || accessProperties.getMaximumSpeedKmH() == null){
      maxSpeedEqual = getMaximumSpeedKmH() ==null && accessProperties.getMaximumSpeedKmH() == null;
    }else{
      maxSpeedEqual = Precision.equal(getMaximumSpeedKmH(), accessProperties.getMaximumSpeedKmH());
    }

    boolean critSpeedEqual = false;
    if(getCriticalSpeedKmH()==null || accessProperties.getCriticalSpeedKmH()==null){
      critSpeedEqual = getCriticalSpeedKmH()==null && accessProperties.getCriticalSpeedKmH()==null;
    }else{
      critSpeedEqual = Precision.equal(getCriticalSpeedKmH(), accessProperties.getCriticalSpeedKmH());
    }

    return maxSpeedEqual && critSpeedEqual;
  }

  /**
   * Add mode to access group.
   * 
   * Use with caution if registered on a link segment type, in which case adding the mode here is not sufficient since it also requires mode based indexing on the type. Better to
   * use the link segment type methods to properly update the group access instead
   */
  @Override
  public void addAccessMode(Mode mode) {
    this.supportedModes.add(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Modes: [");
    this.supportedModes.forEach(m -> sb.append(m.toString()).append(","));
    sb.deleteCharAt(sb.length() - 1);
    sb.append("] maxSpeed (km/h): ");
    sb.append(getMaximumSpeedOrDefaultKmH(-1));
    sb.append(" critSpeed (km/h): ");
    sb.append(getCriticalSpeedOrDefaultKmH(-1));
    return sb.toString();
  }

}
