package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneFactory;
import org.goplanit.utils.zoning.TransferZones;

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
   */
  public TransferZonesImpl(TransferZonesImpl other, boolean deepCopy) {
    super(other, deepCopy);
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
  public TransferZonesImpl clone() {
    return new TransferZonesImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZonesImpl deepClone() {
    return new TransferZonesImpl(this, true);
  }

}
