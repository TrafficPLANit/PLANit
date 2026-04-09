package org.goplanit.zoning;

import org.goplanit.utils.zoning.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.ZoneConnectoidType;
import org.goplanit.utils.zoning.Zone;

import java.util.Iterator;
import java.util.Optional;

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
    protected ZoneConnectoidType type = DEFAULT_CONNECTOID_TYPE;

    /**
     * constructor
     *
     * @param accessZone to use
     */
    protected ConnectoidAccessZoneEntryImpl(Zone accessZone) {
      this.accessZone = accessZone;
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
    }

  @Override
  public void setType(ZoneConnectoidType type) {
    this.type = type;
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

  @Override
  public ConnectoidAccessZoneEntry shallowClone() {
    return new ConnectoidAccessZoneEntryImpl(this);
  }

  @Override
  public ConnectoidAccessZoneEntry deepClone() {
    return shallowClone();
  }

}
