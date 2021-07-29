package org.planit.zoning;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneFactory;
import org.planit.utils.zoning.TransferZones;

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
   * @param groupId             to use for creating ids for instances
   * @param transferZoneFactory the factory to use
   */
  public TransferZonesImpl(TransferZoneFactory transferZoneFactory) {
    super();
    this.transferZoneFactory = transferZoneFactory;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public TransferZonesImpl(TransferZonesImpl other) {
    super(other);
    this.transferZoneFactory = other.transferZoneFactory;
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
    return new TransferZonesImpl(this);
  }

}
