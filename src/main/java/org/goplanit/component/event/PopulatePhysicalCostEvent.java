package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.network.MacroscopicNetwork;

/**
 * A Populate physical cost event is fired when PLANit requests for a registered listener to populate the newly created physical cost component. It is assumed only a single
 * listener will populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end user will - via the
 * listener - receive this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulatePhysicalCostEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when a physical cost component needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.COST.PHYSICAL.POPULATE");

  /**
   * Constructor
   * 
   * @param source                 of the event
   * @param physicalCostToPopulate network to populate
   */
  public PopulatePhysicalCostEvent(final PlanitComponentFactory<?> source, AbstractPhysicalCost physicalCostToPopulate, MacroscopicNetwork network) {
    super(EVENT_TYPE, source, physicalCostToPopulate, network);
  }

  /**
   * Collect physical cost to populate
   * 
   * @return physical cost component
   */
  public AbstractPhysicalCost getPhysicalCostToPopulate() {
    return (AbstractPhysicalCost) getComponentToPopulate();
  }

  /**
   * the network on which to populate the physical cost
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[0];
  }

}
