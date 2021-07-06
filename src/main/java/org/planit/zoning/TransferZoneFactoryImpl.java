package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneFactory;
import org.planit.utils.zoning.Zones;

/**
 * Factory for creating od zones (on container)
 * 
 * @author markr
 */
public class TransferZoneFactoryImpl extends ManagedIdEntityFactoryImpl<TransferZone> implements TransferZoneFactory {

  /** container to use */
  protected final Zones<TransferZone> transferZones;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected TransferZoneFactoryImpl(final IdGroupingToken groupId, final Zones<TransferZone> transferZones) {
    super(groupId);
    this.transferZones = transferZones;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone registerUniqueCopyOf(ManagedId transferZone) {
    TransferZone copy = createUniqueCopyOf(transferZone);
    transferZones.register(copy);
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone registerNew(final Centroid centroid) {
    TransferZone newOdZone = createNew(centroid);
    transferZones.register(newOdZone);
    return newOdZone;
  }

  /**
   * {@inheritDoc}
   * 
   * @param centroid to use
   * @return created zone
   */
  @Override
  public TransferZone createNew(final Centroid centroid) {
    TransferZoneImpl transferZone = new TransferZoneImpl(getIdGroupingToken());
    transferZone.setCentroid(centroid);
    return transferZone;
  }

}
