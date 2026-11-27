package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.ModePricingSchemeFactory;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.problem.Problems;

/**
 * Factory for mode schemes: makes one, resolves it against the network, and has the network pricing check it and add
 * it unless a problem refuses it
 * <p>
 * The one way to add a mode scheme, so its checks, its own and those across schemes, always run, in one order, and the
 * caller gets every problem found to report under its own name for the scheme.
 * </p>
 *
 * @author markr
 */
public class ModePricingSchemeFactoryImpl extends ManagedIdEntityFactoryImpl<ModePricingScheme>
    implements ModePricingSchemeFactory {

  /** the network pricing the schemes belong to */
  private final NetworkPricing networkPricing;

  /** container to register on */
  private final ModePricingSchemesImpl modeSchemes;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   * @param modeSchemes to register on
   */
  protected ModePricingSchemeFactoryImpl(
      final IdGroupingToken groupId,
      final NetworkPricing networkPricing,
      final ModePricingSchemesImpl modeSchemes) {
    super(groupId);
    this.networkPricing = networkPricing;
    this.modeSchemes = modeSchemes;
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
  public Pair<ModePricingScheme, Problems<PricingProblemKind>> registerNew(
      String name,
      PricedModeSelection modes,
      ModePricingSchemeLinkSegmentCoverage pricedLinkSegments,
      List<PricingSchemeSection> sections,
      SchemePricing<StartWithPerKmPriceRule> pricing) {
    var network = networkPricing.getParentNetwork();
    var scheme = new ModePricingSchemeImpl(
        getIdGroupingToken(), name, modes, network.getTransportLayers(), pricedLinkSegments, sections, pricing);

    var problems = scheme.resolve(networkPricing.getSections(),networkPricing.getGates());
    var registered = networkPricing.registerUnlessRefused(scheme, problems, modeSchemes);
    return Pair.of(registered.first(), registered.second());
  }
}
