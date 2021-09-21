package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneFactory;
import org.planit.utils.zoning.TransferZones;

/**
 * Factory for creating od zones (on container)
 * 
 * @author markr
 */
public class TransferZoneFactoryImpl extends ZoneFactoryImpl<TransferZone> implements TransferZoneFactory {

  /** container to use */
  protected final TransferZones transferZones;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected TransferZoneFactoryImpl(final IdGroupingToken groupId, final TransferZones transferZones) {
    super(groupId);
    this.transferZones = transferZones;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone registerNew() {
    TransferZone newOdZone = createNew();
    transferZones.register(newOdZone);
    return newOdZone;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public TransferZone createNew() {
    TransferZoneImpl transferZone = new TransferZoneImpl(getIdGroupingToken());
    transferZone.setCentroid(getCentroidFactory().create(transferZone));
    return transferZone;
  }

}
