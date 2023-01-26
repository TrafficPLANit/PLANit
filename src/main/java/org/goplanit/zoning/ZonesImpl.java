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
   */
  public ZonesImpl() {
    super(Zone::getId, Zone.ZONE_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param zonesImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ZonesImpl(ZonesImpl<Z> zonesImpl, boolean deepCopy) {
    super(zonesImpl, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> deepClone();

}
