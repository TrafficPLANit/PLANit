package org.planit.component.event;

import org.planit.component.PlanitComponentFactory;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.network.MacroscopicNetwork;

/**
 * A Populate initial link segment cost event is fired when PLANit requests for a registered listener to populate these initial costs which relate to a specific network. It is
 * assumed only a single handler populates this component and the registration of this handler is dealt with by the platform rather than the user. The end user will - via the
 * handler - receive this event when implementing an input builder, and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateInitialLinkSegmentCostEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when initial costs (without specific time period) needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.INITIALCOST.POPULATE");

  /**
   * Constructor
   * 
   * @param source                           of the event
   * @param initialLinkSegmentCostToPopulate cost to populate
   * @param fileName                         with the location of the costs to use for populating the memory model
   * @param network                          parent network of these costs
   */
  public PopulateInitialLinkSegmentCostEvent(final PlanitComponentFactory<?> source, final InitialLinkSegmentCost initialLinkSegmentCostToPopulate, String fileName,
      final MacroscopicNetwork network) {
    super(EVENT_TYPE, source, initialLinkSegmentCostToPopulate, new Object[] { fileName, network });
  }

  /**
   * Collect PLANit initial cost component to populate
   * 
   * @return zoning
   */
  public InitialLinkSegmentCost getInitialLinkSegmentCostToPopulate() {
    return (InitialLinkSegmentCost) getComponentToPopulate();
  }

  /**
   * Collect fileName of the file containing the actual initial costs to use to populate this instance
   * 
   * @return fileName
   */
  public String getFileName() {
    return (String) getAdditionalContent()[0];
  }

  /**
   * Collect network these costs should relate to
   * 
   * @return network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[1];
  }

}
