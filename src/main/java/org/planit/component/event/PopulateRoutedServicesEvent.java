package org.planit.component.event;

import org.planit.component.PlanitComponentFactory;
import org.planit.service.routed.RoutedServices;

/**
 * A Populate routed services event is fired when PLANit requests for a registered listener to populate the newly created routed services instance. It is assumed only a single
 * listener will populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the
 * listener - receive this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateRoutedServicesEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when network needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.ROUTEDSERVICES.POPULATE");

  /**
   * Constructor
   * 
   * @param source                   of the event
   * @param routedServicesToPopulate routed services to populate
   */
  public PopulateRoutedServicesEvent(final PlanitComponentFactory<?> source, final RoutedServices routedServicesToPopulate) {
    super(EVENT_TYPE, source, routedServicesToPopulate, null);
  }

  /**
   * collect routed services to populate
   * 
   * @return routed services
   */
  public RoutedServices getRoutedServicesToPopulate() {
    return (RoutedServices) getComponentToPopulate();
  }

}
