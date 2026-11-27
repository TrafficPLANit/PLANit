package org.goplanit.pricing;

import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.ModesMinimumPrice;

/**
 * The least one use of a scheme costs, for some of the modes it charges or all of them
 * <p>
 * For example a toll road whose short trips still cost 1.00. What a use costs is added up by whoever prices the trip,
 * so the minimum is held here and applied there.
 * </p>
 *
 * @author markr
 */
public class ModesMinimumPriceImpl implements ModesMinimumPrice {

  /** generated UID */
  private static final long serialVersionUID = 5513069847028143265L;

  /** the least one use costs */
  private final double amountInCurrency;

  /** the modes it is for */
  private final PricedModeSelection pricedModeSelection;

  /**
   * Constructor
   *
   * @param amountInCurrency the least one use costs
   * @param pricedModeSelection the modes it is for, unspecified for every mode the scheme charges
   */
  public ModesMinimumPriceImpl(double amountInCurrency, PricedModeSelection pricedModeSelection) {
    this.amountInCurrency = amountInCurrency;
    this.pricedModeSelection = pricedModeSelection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getAmountInCurrency() {
    return amountInCurrency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricedModeSelection getPricedModeSelection() {
    return pricedModeSelection;
  }
}
