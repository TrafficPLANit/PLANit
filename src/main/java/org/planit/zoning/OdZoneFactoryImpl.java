package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.OdZoneFactory;
import org.planit.utils.zoning.Zones;

/**
 * Factory for creating od zones (on container)
 * 
 * @author markr
 */
public class OdZoneFactoryImpl extends ManagedIdEntityFactoryImpl<OdZone> implements OdZoneFactory {

  /** container to use */
  protected final Zones<OdZone> odZones;

  /**
   * Constructor
   * 
   * @param groupId             to use
   * @param directedConnectoids to use
   */
  protected OdZoneFactoryImpl(final IdGroupingToken groupId, final Zones<OdZone> odZones) {
    super(groupId);
    this.odZones = odZones;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone registerUniqueCopyOf(ManagedId odZone) {
    OdZone copy = createUniqueCopyOf(odZone);
    odZones.register(copy);
    return copy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone registerNew(final Centroid centroid) {
    OdZone newOdZone = createNew(centroid);
    odZones.register(newOdZone);
    return newOdZone;
  }

  /**
   * {@inheritDoc}
   * 
   * @param centroid to use
   * @return created zone
   */
  @Override
  public OdZone createNew(final Centroid centroid) {
    OdZoneImpl OdZone = new OdZoneImpl(getIdGroupingToken());
    OdZone.setCentroid(centroid);
    return OdZone;
  }

}
