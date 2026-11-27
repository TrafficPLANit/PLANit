package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;

/**
 * The pricing of an entry-exit scheme, whose price rules are an amount per pair of entry and exit gate
 * <p>
 * The steps of the point price rule are its base class's; this class gives them the combining of its kind of price
 * rule, which {@link FromToGatesPriceRuleImpl} does. For example a closed motorway: 4.90 from north to south, 6.10 in
 * the morning band, three times that for trucks.
 * </p>
 *
 * @author markr
 */
public class FromToGatesSchemePricingImpl extends SchemePricingImpl<FromToGatesPriceRule> {

  /** generated UID */
  private static final long serialVersionUID = -5571204493860251377L;

  /**
   * {@inheritDoc}
   */
  @Override
  protected FromToGatesPriceRule createPriceRuleChargingNothing() {
    return new FromToGatesPriceRuleImpl(List.of());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected FromToGatesSchemePricingImpl createSchemePricing(
      FromToGatesPriceRule newBasePriceRule,
      List<PriceOverride<FromToGatesPriceRule>> newPriceOverrides,
      List<PricingTimeBand<FromToGatesPriceRule>> newTimeBands,
      List<PricingDistanceBand> newDistanceBands,
      List<ModesMinimumPrice> newMinimums) {
    return new FromToGatesSchemePricingImpl(newBasePriceRule, newPriceOverrides, newTimeBands, newDistanceBands,
        newMinimums);
  }

  /**
   * Constructor
   *
   * @param basePriceRule for every mode the scheme charges outside its time bands, null when there is none
   * @param priceOverrides of the base price rule, may be empty
   * @param timeBands may be empty
   * @param distanceBands may be empty
   * @param minimums of one use, may be empty
   */
  public FromToGatesSchemePricingImpl(
      FromToGatesPriceRule basePriceRule,
      List<? extends PriceOverride<FromToGatesPriceRule>> priceOverrides,
      List<? extends PricingTimeBand<FromToGatesPriceRule>> timeBands,
      List<? extends PricingDistanceBand> distanceBands,
      List<? extends ModesMinimumPrice> minimums) {
    super(basePriceRule, priceOverrides, timeBands, distanceBands, minimums);
  }
}
