package org.goplanit.component.event;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.pricing.NetworkPricing;

/**
 * Event fired when PLANit asks a registered listener to populate a newly created network pricing
 * <p>
 * As for a zoning, the listener is usually an input builder registered on a project, which reads the pricing from its
 * input onto the network it refers to.
 * </p>
 *
 * @author markr
 */
public class PopulateNetworkPricingEvent extends PopulateUntypedComponentEvent {

  /** event type fired off when a network pricing needs to be populated */
  public static final PlanitComponentEventType EVENT_TYPE =
      new PlanitComponentEventType("PLANITCOMPONENT.NETWORKPRICING.POPULATE");

  /**
   * Constructor
   *
   * @param source of the event
   * @param networkPricingToPopulate network pricing to populate
   * @param network the pricing refers to
   */
  public PopulateNetworkPricingEvent(
      final PlanitComponentFactory<?> source,
      final NetworkPricing networkPricingToPopulate,
      final MacroscopicNetwork network) {
    super(EVENT_TYPE, source, networkPricingToPopulate, new Object[] { network });
  }

  /**
   * The network pricing to populate
   *
   * @return network pricing
   */
  public NetworkPricing getNetworkPricingToPopulate() {
    return (NetworkPricing) getComponentToPopulate();
  }

  /**
   * The network the pricing refers to
   *
   * @return network
   */
  public MacroscopicNetwork getParentNetwork() {
    return (MacroscopicNetwork) getAdditionalContent()[0];
  }
}
