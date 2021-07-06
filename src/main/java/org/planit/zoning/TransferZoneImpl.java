package org.planit.zoning;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
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

  /** the transfer zone groups this transfer zone is part of */
  Set<TransferZoneGroup> transferZoneGroups = null;

  /**
   * generate unique od zone id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return odZoneId
   */
  protected static long generateTransferZoneId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, TransferZone.TRANSFER_ZONE_ID_CLASS);
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
   * Copy constructor
   * 
   * @param transferZoneImpl to copy
   */
  public TransferZoneImpl(TransferZoneImpl transferZoneImpl) {
    super(transferZoneImpl);
    this.transferZoneId = transferZoneImpl.transferZoneId;
    this.type = transferZoneImpl.type;
    if (transferZoneImpl.hasTransferZoneGroup()) {
      this.transferZoneGroups = new HashSet<TransferZoneGroup>(transferZoneImpl.getTransferZoneGroups());
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasTransferZoneGroup() {
    return this.transferZoneGroups != null && !this.transferZoneGroups.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isInTransferZoneGroup(TransferZoneGroup transferZoneGroup) {
    if (transferZoneGroup == null || !hasTransferZoneGroup()) {
      return false;
    }

    return this.transferZoneGroups.contains(transferZoneGroup);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addToTransferZoneGroup(TransferZoneGroup transferZoneGroup) {
    if (transferZoneGroup == null) {
      LOGGER.warning(String.format("transfer zone group null, unable to register on transfer zone %s", getXmlId()));
      return;
    }

    if (!hasTransferZoneGroup()) {
      this.transferZoneGroups = new HashSet<TransferZoneGroup>();
    }

    /* register */
    this.transferZoneGroups.add(transferZoneGroup);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeFromTransferZoneGroup(TransferZoneGroup transferZoneGroup) {
    boolean success = false;
    if (isInTransferZoneGroup(transferZoneGroup)) {
      success = this.transferZoneGroups.remove(transferZoneGroup);
      if (transferZoneGroups.isEmpty()) {
        transferZoneGroups = null;
      }

      if (transferZoneGroup.hasTransferZone(this)) {
        transferZoneGroup.removeTransferZone(this);
      }
    }
    return success;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeFromAllTransferZoneGroups() {
    if (hasTransferZoneGroup()) {
      Iterator<TransferZoneGroup> iterator = transferZoneGroups.iterator();
      while (iterator.hasNext()) {
        TransferZoneGroup group = iterator.next();
        iterator.remove();
        group.removeTransferZone(this);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Set<TransferZoneGroup> getTransferZoneGroups() {
    if (hasTransferZoneGroup()) {
      return Collections.unmodifiableSet(transferZoneGroups);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setTransferZoneId(generateTransferZoneId(tokenId));
    return super.recreateManagedIds(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneImpl clone() {
    return new TransferZoneImpl(this);
  }

}
