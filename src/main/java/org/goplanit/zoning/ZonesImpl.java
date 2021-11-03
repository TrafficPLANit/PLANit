package org.goplanit.zoning;

import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.Zones;

/**
 * Partial implementation of the Zones &lt;T&gt; interface
 * 
 * @author markr
 *
 * @param <Z> zone type
 */
public abstract class ZonesImpl<Z extends Zone> extends ManagedIdEntitiesImpl<Z> implements Zones<Z> {

  /**
   * Constructor
   * 
   * @param tokenId to use for creating ids for instances
   */
  public ZonesImpl() {
    super(Zone::getId, Zone.ZONE_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param zonesImpl to copy
   */
  public ZonesImpl(ZonesImpl<Z> zonesImpl) {
    super(zonesImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> clone();

}
