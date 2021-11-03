package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZoneFactory;
import org.goplanit.utils.zoning.OdZones;

/**
 * Factory for creating od zones (on container)
 * 
 * @author markr
 */
public class OdZoneFactoryImpl extends ZoneFactoryImpl<OdZone> implements OdZoneFactory {

  /** container to use */
  protected final OdZones odZones;

  /**
   * Constructor
   * 
   * @param groupId to use
   * @param odZones to use
   */
  protected OdZoneFactoryImpl(final IdGroupingToken groupId, final OdZones odZones) {
    super(groupId);
    this.odZones = odZones;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone registerNew() {
    OdZone newOdZone = createNew();
    odZones.register(newOdZone);
    return newOdZone;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public OdZone createNew() {
    OdZoneImpl newOdZone = new OdZoneImpl(getIdGroupingToken());
    newOdZone.setCentroid(getCentroidFactory().create(newOdZone));
    return newOdZone;
  }

}
