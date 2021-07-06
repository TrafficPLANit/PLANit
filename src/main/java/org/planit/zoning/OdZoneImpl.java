package org.planit.zoning;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZone;

/**
 * An Od zone
 * 
 * @author markr
 *
 */
public class OdZoneImpl extends ZoneImpl implements OdZone {

  /**
   * unique id across all OdZones
   */
  private long odZoneId;

  /**
   * generate unique od zone id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return odZoneId
   */
  protected static long generateOdZoneId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, OdZone.OD_ZONE_ID_CLASS);
  }

  /**
   * Set OD Zone Id
   * 
   * @param odZoneId to set
   */
  protected void setOdZoneId(long odZoneId) {
    this.odZoneId = odZoneId;
  }

  /**
   * constructor
   * 
   * @param tokenId for id generation
   */
  public OdZoneImpl(IdGroupingToken tokenId) {
    super(tokenId);
    setOdZoneId(generateOdZoneId(tokenId));
  }

  /**
   * Copy constructor
   * 
   * @param odZoneImpl to copy
   */
  public OdZoneImpl(OdZoneImpl odZoneImpl) {
    super(odZoneImpl);
    this.odZoneId = odZoneImpl.odZoneId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOdZoneId() {
    return odZoneId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setOdZoneId(generateOdZoneId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZoneImpl clone() {
    return new OdZoneImpl(this);
  }

}
