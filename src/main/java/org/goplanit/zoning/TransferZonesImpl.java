package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.*;

import java.util.function.BiConsumer;

/**
 * implementation of the Zones &lt;T&gt; interface for transfer zones
 * 
 * @author markr
 *
 */
public class TransferZonesImpl extends ZonesImpl<TransferZone> implements TransferZones {

  /** factory to use */
  private final TransferZoneFactory transferZoneFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public TransferZonesImpl(final IdGroupingToken groupId) {
    super();
    this.transferZoneFactory = new TransferZoneFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param transferZoneFactory the factory to use
   */
  public TransferZonesImpl(TransferZoneFactory transferZoneFactory) {
    super();
    this.transferZoneFactory = transferZoneFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public TransferZonesImpl(TransferZonesImpl other, boolean deepCopy, BiConsumer<TransferZone, TransferZone> mapper) {
    super(other, deepCopy, mapper);
    this.transferZoneFactory =
            new TransferZoneFactoryImpl(other.transferZoneFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneFactory getFactory() {
    return transferZoneFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional transfer zone id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), TransferZone.TRANSFER_ZONE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZonesImpl shallowClone() {
    return new TransferZonesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZonesImpl deepClone() {
    return new TransferZonesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZonesImpl deepCloneWithMapping(BiConsumer<TransferZone, TransferZone> mapper) {
    return new TransferZonesImpl(this, true, mapper);
  }

}
