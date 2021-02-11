package org.planit.zoning;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneType;

/**
 * A transfer zone
 * 
 * @author markr
 *
 */
public class TransferZoneImpl extends ZoneImpl implements TransferZone {

  /**
   * unique id across all transfer zones
   */
  private long transferZoneId;

  /**
   * the type of this transfer zone
   */
  private TransferZoneType type = DEFAULT_TYPE;

  /**
   * generate unique od zone id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return odZoneId
   */
  protected static long generateTransferZoneId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, OdZone.class);
  }

  /**
   * Set transfer zone Id
   * 
   * @param transferZoneId to set
   */
  protected void setTransferZoneId(long transferZoneId) {
    this.transferZoneId = transferZoneId;
  }

  /**
   * constructor
   * 
   * @param tokenId for id generation
   */
  public TransferZoneImpl(IdGroupingToken tokenId) {
    super(tokenId);
    setTransferZoneId(generateTransferZoneId(tokenId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getTransferZoneId() {
    return transferZoneId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setType(TransferZoneType type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneType getTransferZoneType() {
    return type;
  }

}
