package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.problem.Problems;

/**
 * Factory for entry-exit schemes: makes one on its toll roads with its gates, resolves it against the network, and has
 * the network pricing check it and add it unless a problem refuses it
 * <p>
 * The one way to add an entry-exit scheme, so its checks, its own and those across schemes, always run, in one order,
 * and the caller gets every problem found to report under its own name for the scheme.
 * </p>
 *
 * @author markr
 */
public class EntryExitPricingSchemeFactoryImpl extends ManagedIdEntityFactoryImpl<EntryExitPricingScheme>
    implements EntryExitPricingSchemeFactory {

  /** the network pricing the schemes belong to */
  private final NetworkPricing networkPricing;

  /** container to register on */
  private final EntryExitPricingSchemesImpl entryExitSchemes;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param networkPricing the schemes belong to
   * @param entryExitSchemes to register on
   */
  protected EntryExitPricingSchemeFactoryImpl(
      final IdGroupingToken groupId,
      final NetworkPricing networkPricing,
      final EntryExitPricingSchemesImpl entryExitSchemes) {
    super(groupId);
    this.networkPricing = networkPricing;
    this.entryExitSchemes = entryExitSchemes;
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
  public Pair<EntryExitPricingScheme, Problems<PricingProblemKind>> registerNew(
      String name,
      PricedModeSelection modes,
      List<TollRoad> tollRoads,
      List<PricingSchemeGate> gates,
      SchemePricing<FromToGatesPriceRule> pricing) {
    var network = networkPricing.getParentNetwork();
    var scheme = new EntryExitPricingSchemeImpl(
        getIdGroupingToken(), name, modes, network.getTransportLayers(), tollRoads, gates, pricing);

    var problems = scheme.resolve(networkPricing.getSections(), networkPricing.getGates());
    var registered = networkPricing.registerUnlessRefused(scheme, problems, entryExitSchemes);
    return Pair.of(registered.first(), registered.second());
  }
}
