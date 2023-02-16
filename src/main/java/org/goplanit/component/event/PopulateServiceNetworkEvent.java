package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.zoning.Zoning;

/**
 * A Populate service network event is fired when PLANit requests for a registered listener to populate the newly created service network instance. It is assumed only a single
 * listener will populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the
 * listener - receive this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateServiceNetworkEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when network needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.SERVICENETWORK.POPULATE");

  /**
   * Constructor
   * 
   * @param source            of the event
   * @param networkToPopulate service network to populate
   * @param parentNetwork to use
   */
  public PopulateServiceNetworkEvent(final PlanitComponentFactory<?> source, final ServiceNetwork networkToPopulate, MacroscopicNetwork parentNetwork) {
    super(EVENT_TYPE, source, networkToPopulate, parentNetwork);
  }

  /**
   * collect service network to populate
   * 
   * @return network
   */
  public ServiceNetwork getServiceNetworkToPopulate() {
    return (ServiceNetwork) getComponentToPopulate();
  }

  /**
   * collect parent network to relate to
   *
   * @return network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[0];
  }

}
