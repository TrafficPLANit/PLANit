package org.planit.zoning;

import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.Zones;

/**
 * implementation of the Zones &lt;T&gt; interface for transfer zones
 * 
 * @author markr
 *
 */
public class TransferZonesImpl extends ZonesImpl<TransferZone> implements Zones<TransferZone> {

  /** the zoning builder to use */
  protected final ZoningBuilder zoningBuilder;
  
  /**
   * Constructor
   * 
   * @param zoningBuilder to use
   */
  public TransferZonesImpl(ZoningBuilder zoningBuilder) {
    super();
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone createNew() {
    return zoningBuilder.createTransferZone();
  }

}
