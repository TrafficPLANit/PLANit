package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZoneFactory;
import org.goplanit.utils.zoning.OdZones;

import java.util.function.BiConsumer;

/**
 * implementation of the Zones &lt;T&gt; interface for Od zones
 * 
 * @author markr
 *
 */
public class OdZonesImpl extends ZonesImpl<OdZone> implements OdZones {

  /** factory to use */
  private final OdZoneFactory odZoneFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public OdZonesImpl(final IdGroupingToken groupId) {
    super();
    this.odZoneFactory = new OdZoneFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param odZoneFactory the factory to use
   */
  public OdZonesImpl(OdZoneFactory odZoneFactory) {
    super();
    this.odZoneFactory = odZoneFactory;
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public OdZonesImpl(OdZonesImpl other, boolean deepCopy, BiConsumer<OdZone, OdZone> mapper) {
    super(other, deepCopy, mapper);
    this.odZoneFactory = new OdZoneFactoryImpl(other.odZoneFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZoneFactory getFactory() {
    return odZoneFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional od zone id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), OdZone.OD_ZONE_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZonesImpl shallowClone() {
    return new OdZonesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZonesImpl deepClone() {
    return new OdZonesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZonesImpl deepCloneWithMapping(BiConsumer<OdZone, OdZone> mapper) {
    return new OdZonesImpl(this, true, mapper);
  }

}
