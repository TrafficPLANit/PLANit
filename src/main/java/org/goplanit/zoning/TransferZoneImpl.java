package org.goplanit.zoning;

import java.util.*;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.TransferZoneType;

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

  /** List of human-readable platform names relevant for this transfer zone */
  List<String> platformNames;

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
    this.platformNames = null;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TransferZoneImpl(TransferZoneImpl other) {
    super(other);
    this.transferZoneId = other.transferZoneId;
    this.type = other.type;
    if (other.hasTransferZoneGroup()) {
      this.transferZoneGroups = new HashSet<TransferZoneGroup>(other.getTransferZoneGroups());
    }
    if(other.hasPlatformNames()){
      this.platformNames = new ArrayList<>(other.getTransferZonePlatformNames());
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
  public List<String> getTransferZonePlatformNames() {
    return this.platformNames;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addTransferZonePlatformName(String platformName) {
    if(this.platformNames == null){
      this.platformNames = new ArrayList<>(1);
    }else if(this.platformNames.contains(platformName)){
      return false;
    }

    this.platformNames.add(platformName);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeTransferZonePlatformName(String platformName) {
    return this.platformNames.remove(platformName);
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
