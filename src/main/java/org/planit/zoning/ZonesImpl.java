package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

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
   * @param groupId to use for creating ids for instances
   */
  public ZonesImpl(final IdGroupingToken groupId) {
    super(Zone::getId);
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
