package org.goplanit.zoning;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.ZoneConnectoidType;
import org.goplanit.utils.zoning.Zone;

import java.util.*;

/**
 * Stores access properties for each zone for a given connectoid
 *
 * @author markr
 *
 */
public class ConnectoidAccessZoneEntryImpl implements ConnectoidAccessZoneEntry {

  /** access zone for these properties */
  protected Zone accessZone;

  /** length to connectoid */
  protected Optional<Double> lengthKm = DEFAULT_LENGTH_KM;

  /** type of the connectoid to zone combination */
  protected final ZoneConnectoidType type;

  /** the explicitly allowed modes, when null all modes allowed */
  private TreeMap<Long, Mode> explicitAllowedModes = null;

  /**
   * constructor
   *
   * @param accessZone to use
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
      this.explicitAllowedModes = new TreeMap<>(other.explicitAllowedModes);
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
      explicitAllowedModes = new TreeMap<>();
    }
    explicitAllowedModes.put(mode.getId(), mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Mode mode) {
    if (explicitAllowedModes == null) {
      return true;
    }else{
      return explicitAllowedModes.containsKey(mode.getId());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Mode> getExplicitlyAllowedModes() {
    return explicitAllowedModes==null ? Collections.emptySet() :
        Collections.unmodifiableCollection(explicitAllowedModes.values());
  }

  @Override
  public ConnectoidAccessZoneEntry shallowClone() {
    return new ConnectoidAccessZoneEntryImpl(this);
  }

  @Override
  public ConnectoidAccessZoneEntry deepClone() {
    return shallowClone();
  }

}
