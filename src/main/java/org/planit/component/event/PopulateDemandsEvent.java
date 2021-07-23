package org.planit.component.event;

import org.planit.component.PlanitComponentFactory;
import org.planit.demands.Demands;
import org.planit.network.MacroscopicNetwork;
import org.planit.zoning.Zoning;

/**
 * A Populate demands event is fired when PLANit requests for a registered listener to populate the newly created demands instance. It is assumed only a single listener will
 * populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the listener - receive
 * this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateDemandsEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when demands need to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.DEMANDS.POPULATE");

  /**
   * Constructor
   * 
   * @param source            of the event
   * @param demandsToPopulate demands to populate
   * @param parentZoning      upon which the demands are to be build
   * @param parentNetwork     upon which the demands are to be build
   */
  public PopulateDemandsEvent(final PlanitComponentFactory<?> source, Demands demandsToPopulate, Zoning parentZoning, MacroscopicNetwork parentNetwork) {
    super(EVENT_TYPE, source, demandsToPopulate, new Object[] { parentZoning, parentNetwork });
  }

  /**
   * Collect PLANit demands to populate
   * 
   * @return demands
   */
  public Demands getDemandsToPopulate() {
    return (Demands) getComponentToPopulate();
  }

  /**
   * Collect PLANit zoning upon which this demands is to be applied
   * 
   * @return zoning
   */
  public Zoning getParentZoning() {
    return (Zoning) getAdditionalContent()[0];
  }

  /**
   * Collect PLANit network upon which this demands is to be applied
   * 
   * @return network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[1];
  }

}
