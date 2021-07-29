package org.planit.zoning;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.OdZoneFactory;
import org.planit.utils.zoning.OdZones;

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
   * Copy constructor
   * 
   * @param other to copy
   */
  public OdZonesImpl(OdZonesImpl other) {
    super(other);
    this.odZoneFactory = other.odZoneFactory;
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
