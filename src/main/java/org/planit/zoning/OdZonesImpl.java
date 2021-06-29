package org.planit.zoning;

import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.Zones;

/**
 * implementation of the Zones &lt;T&gt; interface for Od zones
 * 
 * @author markr
 *
 */
public class OdZonesImpl extends ZonesImpl<OdZone> implements Zones<OdZone> {

  /** the zoning builder to use */
  protected final ZoningBuilder zoningBuilder;

  /**
   * Constructor
   * 
   * @param zoningBuilder to use
   */
  public OdZonesImpl(ZoningBuilder zoningBuilder) {
    super();
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone createNew() {
    return zoningBuilder.createOdZone();
  }

}
