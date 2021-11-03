package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.network.MacroscopicNetwork;

/**
 * A Populate network event is fired when PLANit requests for a registered listener to populate the newly created network instance. It is assumed only a single listener will
 * populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the listener - receive
 * this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateNetworkEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when network needs to be populated */
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
   * collect network to populate
   * 
   * @return network
   */
  public MacroscopicNetwork getNetworkToPopulate() {
    return (MacroscopicNetwork) getComponentToPopulate();
  }

}
