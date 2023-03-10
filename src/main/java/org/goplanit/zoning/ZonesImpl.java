package org.goplanit.zoning;

import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.Zones;

import java.util.function.BiConsumer;

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
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public ZonesImpl(ZonesImpl<Z> zonesImpl, boolean deepCopy, BiConsumer<Z, Z> mapper) {
    super(zonesImpl, deepCopy, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ZonesImpl<Z> deepCloneWithMapping(BiConsumer<Z, Z> mapper);

}
