package org.planit.component.event;

import org.planit.component.PlanitComponentFactory;
import org.planit.network.MacroscopicNetwork;

/**
 * A Populate component event is fired when PLANit requests for a registered listener to populate the provided PLANit component instance. It is assumed only a single listener will
 * populate this component and it is expected that the regisration of this listener is handled by the platform rather than the user. The end user will receive this event when
 * implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateNetworkEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when edge has been broken */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.NETWORK.POPULATE");

  /**
   * Constructor
   * 
   * @param source            of the event
   * @param networkToPopulate network to populate
   */
  public PopulateNetworkEvent(final PlanitComponentFactory<?> source, MacroscopicNetwork networkToPopulate) {
    super(EVENT_TYPE, source, networkToPopulate, null);
  }

  /**
   * collect PLANit component to populate
   * 
   * @return component to break
   */
  public MacroscopicNetwork getNetworkToPopulate() {
    return (MacroscopicNetwork) getComponentToPopulate();
  }

}
