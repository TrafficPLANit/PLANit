package org.goplanit.zoning;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZoneFactory;
import org.goplanit.utils.zoning.OdZones;

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
   */
  public OdZonesImpl(OdZonesImpl other) {
    super(other);
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
  public OdZonesImpl clone() {
    return new OdZonesImpl(this);
  }

}
