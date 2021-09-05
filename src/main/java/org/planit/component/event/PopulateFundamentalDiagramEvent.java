package org.planit.component.event;

import org.planit.component.PlanitComponentFactory;
import org.planit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;

/**
 * A Populate fundamental diagram component event is fired when PLANit requests for a registered listener to populate the newly created fundamental diagram component instance. It
 * is assumed only a single listener will populate this component and it is expected that the registration of this listener is handled by the platform rather than the user. The end
 * user will - via the listener - receive this event when implementing an input builder and registering this builder on a PLANit project for example.
 * 
 * @author markr
 *
 */
public class PopulateFundamentalDiagramEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when fundamental diagram component needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE = new PlanitComponentEventType("PLANITCOMPONENT.FUNDAMENTALDIAGRAM.POPULATE");

  /**
   * Constructor
   * 
   * @param source                of the event
   * @param fdComponentToPopulate fundamental diagram component to populate
   * @param parentNetworkLayer    to use
   */
  public PopulateFundamentalDiagramEvent(final PlanitComponentFactory<?> source, final FundamentalDiagramComponent fdComponentToPopulate,
      final MacroscopicNetworkLayer parentNetworkLayer) {
    super(EVENT_TYPE, source, fdComponentToPopulate, parentNetworkLayer);
  }

  /**
   * collect fundamental diagram component to populate
   * 
   * @return fundamental diagram component
   */
  public FundamentalDiagramComponent getFundamentalDiagramToPopulate() {
    return (FundamentalDiagramComponent) getComponentToPopulate();
  }

  /**
   * Collect PLANit network layer upon which these fundamental diagrams are to be applied
   * 
   * @return network layer
   */
  public MacroscopicNetworkLayer getParentNetworkLayer() {
    return (MacroscopicNetworkLayer) getAdditionalContent()[0];
  }

}
