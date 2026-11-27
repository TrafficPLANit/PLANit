package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.LocationPricingSchemeFactory;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.problem.Problems;

/**
 * Factory for location schemes: makes one on its toll roads, resolves it against the network, and has the network
 * pricing check it and add it unless a problem refuses it
 * <p>
 * The one way to add a location scheme, so its checks, its own and those across schemes, always run, in one order, and
 * the caller gets every problem found to report under its own name for the scheme.
 * </p>
 *
 * @author markr
 */
public class LocationPricingSchemeFactoryImpl extends ManagedIdEntityFactoryImpl<LocationPricingScheme>
    implements LocationPricingSchemeFactory {

  /** the network pricing the schemes belong to */
  private final NetworkPricing networkPricing;

  /** container to register on */
  private final LocationPricingSchemesImpl locationSchemes;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   * @param locationSchemes to register on
   */
  protected LocationPricingSchemeFactoryImpl(
      final IdGroupingToken groupId,
      final NetworkPricing networkPricing,
      final LocationPricingSchemesImpl locationSchemes) {
    super(groupId);
    this.networkPricing = networkPricing;
    this.locationSchemes = locationSchemes;
  }

  /**
   * The network pricing the schemes belong to
   *
   * @return the network pricing
   */
  protected NetworkPricing getNetworkPricing() {
    return networkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<LocationPricingScheme, Problems<PricingProblemKind>> registerNew(
      String name,
      PricedModeSelection modes,
      List<TollRoad> tollRoads,
      List<PricingSchemeSection> sections,
      SchemePricing<StartWithPerKmPriceRule> pricing) {
    var network = networkPricing.getParentNetwork();
    var scheme = new LocationPricingSchemeImpl(
        getIdGroupingToken(), name, modes, network.getTransportLayers(),tollRoads, sections, pricing);

    var problems = scheme.resolve(networkPricing.getSections(), networkPricing.getGates());
    var registered = networkPricing.registerUnlessRefused(scheme, problems, locationSchemes);
    return Pair.of(registered.first(), registered.second());
  }
}
