package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.Zones;

/**
 * implementation of the Zones &lt;T&gt; interface for Od zones
 * 
 * @author markr
 *
 */
public class OdZonesImpl extends ZonesImpl<OdZone> implements Zones<OdZone> {

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  public OdZonesImpl(IdGroupingToken tokenId) {
    super(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone registerNew() {
    final OdZone newZone = new OdZoneImpl(getGroupingTokenId());
    register(newZone);
    return newZone;
  }

}
