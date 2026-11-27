package org.goplanit.pricing;

import java.util.Map;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.FromToGatesPriceLookupTable;
import org.goplanit.utils.pricing.PricingSchemeGate;

/**
 * The lookup table of an entry-exit scheme, worked out from its pricing when made: the amount by mode, band and pair
 * of gates
 * <p>
 * A route search reaching an exit asks for the amount of the use from the gate it entered at, per mode and iteration,
 * so the table reads it from an array by the position of each gate in the scheme. For example a truck from the north
 * interchange to the south one of a closed motorway in the morning band pays three times the band's 6.10. A pair no
 * price rule lists costs nothing.
 * </p>
 *
 * @author markr
 */
public class FromToGatesPriceLookupTableImpl extends PriceLookupTableImpl
    implements FromToGatesPriceLookupTable {

  /** generated UID */
  private static final long serialVersionUID = -3093855287410127564L;

  /** the position of each of the scheme's gates, found by identity */
  private final Map<PricingSchemeGate, Integer> gateIndices;

  /** the amount by mode id, time band, from gate and to gate */
  private final double[][][][] amountsInCurrency;

  /**
   * Constructor, working out every amount of the scheme's pricing
   * <p>
   * For each mode the scheme charges and each band, the point price rule before any distance band, see
   * {@link SchemePricingImpl#findPointPriceRuleInTimeBand}, and of it the amount of each pair of the scheme's gates, 0
   * when it lists none. A scheme with 50 gates and 2 bands holds 3 x 2,500 amounts per mode.
   * </p>
   *
   * @param scheme an entry-exit scheme
   * @param modesMinimumPrices the minimum of one use by mode id, 0 when none
   * @param priceCapsPerUse the price cap per use by mode id, {@code Double.POSITIVE_INFINITY} when none
   */
  protected FromToGatesPriceLookupTableImpl(
      PricingSchemeImpl<FromToGatesPriceRule> scheme,
      double[] modesMinimumPrices,
      double[] priceCapsPerUse) {
    super(scheme.getPricing(), modesMinimumPrices, priceCapsPerUse);
    var pricing = (SchemePricingImpl<FromToGatesPriceRule>) scheme.getPricing();
    var timeBands = pricing.getTimeBands();
    var gates = scheme.getGatesOrNone();
    this.gateIndices = ListUtils.createIndexByIdentity(gates);
    this.amountsInCurrency = new double[modesMinimumPrices.length][getTimeBandCount()][gates.size()][gates.size()];
    for (var mode : scheme.getChargedModes()) {
      for (int timeBand = 0; timeBand < getTimeBandCount(); ++timeBand) {
        var priceRule = pricing.findPointPriceRuleInTimeBand(mode,
            timeBand < timeBands.size() ? timeBands.get(timeBand) : null);
        for (var gatePair : priceRule.getPricedFromToGates()) {
          var fromIndex = gateIndices.get(gatePair.getFromGate());
          var toIndex = gateIndices.get(gatePair.getToGate());
          if (fromIndex != null && toIndex != null) {
            amountsInCurrency[(int) mode.getId()][timeBand][fromIndex][toIndex] = gatePair.getAmountInCurrency();
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getAmountInCurrency(
      Mode mode,
      int timeBand,
      int distanceBand,
      PricingSchemeGate fromGate,
      PricingSchemeGate toGate) {
    var fromIndex = gateIndices.get(fromGate);
    var toIndex = gateIndices.get(toGate);
    return isIndexed(mode) && fromIndex != null && toIndex != null ?
        amountsInCurrency[(int) mode.getId()][timeBand][fromIndex][toIndex] * getDistanceBandFactor(distanceBand) : 0;
  }
}
