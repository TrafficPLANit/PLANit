package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.pricing.ModesMinimumPrice;
import org.goplanit.utils.pricing.PriceOverride;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.pricing.PricingTimeBand;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;

/**
 * The pricing of a mode or location scheme, whose price rules are a start per use and a rate per km, with price rules
 * per section
 * <p>
 * The steps of the point price rule are its base class's; this class gives them the combining of its kind of price
 * rule, which {@link StartWithPerKmPriceRuleImpl} does. For example WestConnex: a start of 1.73 and 0.64 per km, three
 * times that for trucks, and 0.80 per km in the morning band.
 * </p>
 *
 * @author markr
 */
public class StartWithPerKmSchemePricingImpl extends SchemePricingImpl<StartWithPerKmPriceRule> {

  /** generated UID */
  private static final long serialVersionUID = 3398142771254960215L;

  /**
   * {@inheritDoc}
   */
  @Override
  protected StartWithPerKmPriceRule createPriceRuleChargingNothing() {
    return new StartWithPerKmPriceRuleImpl(0.0, 0.0, List.of());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StartWithPerKmSchemePricingImpl createSchemePricing(
      StartWithPerKmPriceRule newBasePriceRule,
      List<PriceOverride<StartWithPerKmPriceRule>> newPriceOverrides,
      List<PricingTimeBand<StartWithPerKmPriceRule>> newTimeBands,
      List<PricingDistanceBand> newDistanceBands,
      List<ModesMinimumPrice> newMinimums) {
    return new StartWithPerKmSchemePricingImpl(newBasePriceRule, newPriceOverrides, newTimeBands, newDistanceBands,
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
  public StartWithPerKmSchemePricingImpl(
      StartWithPerKmPriceRule basePriceRule,
      List<? extends PriceOverride<StartWithPerKmPriceRule>> priceOverrides,
      List<? extends PricingTimeBand<StartWithPerKmPriceRule>> timeBands,
      List<? extends PricingDistanceBand> distanceBands,
      List<? extends ModesMinimumPrice> minimums) {
    super(basePriceRule, priceOverrides, timeBands, distanceBands, minimums);
  }
}
