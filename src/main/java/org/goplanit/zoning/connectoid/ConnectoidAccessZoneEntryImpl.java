package org.goplanit.zoning.connectoid;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.utils.zoning.Zone;

import java.util.*;

/**
 * Stores access properties for each zone for a given connectoid
 *
 * @author markr
 *
 */
public class ConnectoidAccessZoneEntryImpl implements ConnectoidAccessZoneEntry {

  /**
   * the explicitly allowed modes in the order of their ids, when null all modes allowed; the modes themselves are held,
   * not their ids, so the entry still knows them after their network recreates their ids, which keeps their order
   */
  private TreeSet<Mode> explicitAllowedModes = null;

  /** access zone for these properties */
  protected Zone accessZone;

  /** length to connectoid */
  protected Optional<Double> lengthKm = DEFAULT_LENGTH_KM;

  /** type of the connectoid to zone combination */
  protected final ZoneConnectoidType type;

  /**
   * constructor
   *
   * @param accessZone to use
   * @param type to use
   */
  protected ConnectoidAccessZoneEntryImpl(Zone accessZone, ZoneConnectoidType type) {
    this.accessZone = accessZone;
    this.type = type;
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  @SuppressWarnings("unchecked")
  public ConnectoidAccessZoneEntryImpl(ConnectoidAccessZoneEntryImpl other) {
    this.accessZone = other.accessZone;
    this.lengthKm = other.lengthKm;
    this.type = other.type;

    /* shallow */
    if (other.explicitAllowedModes != null) {
      this.explicitAllowedModes = new TreeSet<>(other.explicitAllowedModes);
    }
  }

  @Override
  public ZoneConnectoidType getType() {
    return type;
  }

  @Override
  public Zone getAccessZone() {
    return accessZone;
  }

  @Override
  public void setAccessZone(Zone accessZone) {
    this.accessZone = accessZone;
  }

  @Override
  public void setLengthKm(double lengthKm) {
    this.lengthKm = Optional.of(lengthKm);
  }

  @Override
  public Optional<Double> getLengthKm() {
    return lengthKm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addExplicitAllowedMode(Mode mode) {
    if (explicitAllowedModes == null) {
      explicitAllowedModes = new TreeSet<>();
    }
    explicitAllowedModes.add(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeExplicitAllowedMode(Mode mode) {
    if (explicitAllowedModes == null || explicitAllowedModes.size() < 2 ||
        !explicitAllowedModes.contains(mode)) {
      return false;
    }
    explicitAllowedModes.remove(mode);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Mode mode) {
    if (explicitAllowedModes == null) {
      return true;
    }else{
      return explicitAllowedModes.contains(mode);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Mode> getExplicitlyAllowedModes() {
    return explicitAllowedModes==null ? Collections.emptySet() :
        Collections.unmodifiableCollection(explicitAllowedModes);
  }

  @Override
  public ConnectoidAccessZoneEntry shallowClone() {
    return new ConnectoidAccessZoneEntryImpl(this);
  }

  @Override
  public ConnectoidAccessZoneEntry deepClone() {
    return shallowClone();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()){
      return false;
    }
    ConnectoidAccessZoneEntryImpl that = (ConnectoidAccessZoneEntryImpl) o;
    return Objects.equals(
        getAccessZone(), that.getAccessZone()) &&
        Objects.equals(getLengthKm(), that.getLengthKm()) && getType() == that.getType() &&
        Objects.equals(explicitAllowedModes, that.explicitAllowedModes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAccessZone(), getLengthKm(), getType(), explicitAllowedModes);
  }
}
