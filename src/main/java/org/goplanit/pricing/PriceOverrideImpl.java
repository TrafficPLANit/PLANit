package org.goplanit.pricing;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.PriceRule;
import org.goplanit.utils.pricing.PriceOverride;

/**
 * An override of a scheme's price rule for the modes it names: either a price rule of the scheme's kind, its amounts
 * replacing those of the price rule at that moment, or a factor multiplying the whole price rule
 * <p>
 * For example a truck paying three times the car price, a factor of 3, or a motorcycle paying a start of 0.50 and the
 * car's rate per km, a price rule giving the start only. One class serves both kinds of price rule, as an override
 * holds its price rule rather than being one. An override giving both a price rule and a factor, or neither, or not
 * naming its modes, is refused when made, as it says nothing that can be charged; a price rule giving no amount counts
 * as none.
 * </p>
 *
 * @param <P> the kind of price rule
 * @author markr
 */
public class PriceOverrideImpl<P extends PriceRule<P>> implements PriceOverride<P> {

  /** generated UID */
  private static final long serialVersionUID = 2079138548817126435L;

  /** the modes the override is for, given by name */
  private final PricedModeSelection pricedModeOverrides;

  /** the price rule given, null when a factor is given */
  private final P priceRule;

  /** the factor multiplying the whole price rule, null when a price rule is given */
  private final Double priceOverrideFactor;

  /**
   * Constructor
   *
   * @param pricedModeOverrides the modes the override is for, given by name
   * @param priceRule given, null when a factor is given
   * @param priceOverrideFactor multiplying the whole price rule, null when a price rule is given
   * @throws IllegalArgumentException when the modes are not named, or both a price rule and a factor are given, or
   *     neither
   */
  public PriceOverrideImpl(PricedModeSelection pricedModeOverrides, P priceRule, Double priceOverrideFactor) {
    ExceptionUtils.throwIllegalArgumentIf(pricedModeOverrides.getPricedModes().isEmpty(),
        "Price override refused, it names its modes");
    boolean givesPriceRule = priceRule != null && !priceRule.givesNoAmount();
    ExceptionUtils.throwIllegalArgumentIf(givesPriceRule == (priceOverrideFactor != null),
        "Price override refused, it gives either amounts or a factor, not both or neither");
    this.pricedModeOverrides = pricedModeOverrides;
    this.priceRule = givesPriceRule ? priceRule : null;
    this.priceOverrideFactor = priceOverrideFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricedModeSelection getPricedModeOverrides() {
    return pricedModeOverrides;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public P getPriceRule() {
    return priceRule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getPriceOverrideFactor() {
    return priceOverrideFactor;
  }
}
