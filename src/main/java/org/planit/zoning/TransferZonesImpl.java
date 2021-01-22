package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.Zones;

/**
 * implementation of the Zones<T> interface for transfer zones
 * 
 * @author markr
 *
 */
public class TransferZonesImpl extends ZonesImpl<TransferZone> implements Zones<TransferZone> {

  /**
   * constructor
   * 
   * @param tokenId to use
   */
  public TransferZonesImpl(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * {@index}
   */
  @Override
  public TransferZone registerNew() {
    final TransferZone newZone = new TransferZoneImpl(getGroupingTokenId());
    register(newZone);
    return newZone;
  }

}
