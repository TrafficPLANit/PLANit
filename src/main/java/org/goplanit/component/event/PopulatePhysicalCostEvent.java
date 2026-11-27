package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.network.MacroscopicNetwork;

/**
 * A Populate physical cost event is fired when PLANit requests a registered listener to populate the newly created
 * physical cost component. Only a single listener is assumed to populate this component, and the platform rather than
 * the user is expected to register it. The end user receives this event via the listener, for example when
 * implementing an input builder and registering it on a PLANit project.
 * 
 * @author markr
 *
 */
public class PopulatePhysicalCostEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when a physical cost component needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE =
      new PlanitComponentEventType("PLANITCOMPONENT.COST.PHYSICAL.POPULATE");

  /**
   * Constructor
   * 
   * @param source                 of the event
   * @param physicalCostToPopulate network to populate
   * @param network                to apply to
   */
  public PopulatePhysicalCostEvent(
      final PlanitComponentFactory<?> source, AbstractPhysicalCost physicalCostToPopulate, MacroscopicNetwork network) {
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
