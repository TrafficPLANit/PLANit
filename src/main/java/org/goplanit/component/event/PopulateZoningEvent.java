package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * A Populate zoning event is fired when PLANit requests for a registered listener to populate the newly created zoning instance. It is assumed only a single listener will populate
 * this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the listener - receive this
 * event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateZoningEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when zoning needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.ZONING.POPULATE");

  /**
   * Constructor
   * 
   * @param source           of the event
   * @param zoningToPopulate zoning to populate
   * @param network          used by this zoning
   */
  public PopulateZoningEvent(final PlanitComponentFactory<?> source, final Zoning zoningToPopulate, final MacroscopicNetwork network) {
    super(EVENT_TYPE, source, zoningToPopulate, new Object[] { network });
  }

  /**
   * Collect PLANit zoning to populate
   * 
   * @return zoning
   */
  public Zoning getZoningToPopulate() {
    return (Zoning) getComponentToPopulate();
  }

  /**
   * Collect PLANit network upon which this zoning is to be applied
   * 
   * @return network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[0];
  }

}
