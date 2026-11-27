package org.goplanit.pricing;

import java.util.IdentityHashMap;
import java.util.Map;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PriceCapTotals;
import org.goplanit.utils.pricing.PriceCaps;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingScheme;

/**
 * One traveller's totals per cap over a period, made by the network pricing, whose caps it finds by scheme
 * <p>
 * Kept per cap, as each counts over its own period and schemes: a use of WestConnex adds to its own day cap and to a
 * week cap it shares with the Harbour Bridge. A cap over a use needs no totals, as one use is all it counts. Caps are
 * held by identity, as their ids may be recreated.
 * </p>
 *
 * @author markr
 */
public class PriceCapTotalsImpl implements PriceCapTotals {

  /** generated UID */
  private static final long serialVersionUID = 6017449378813346251L;

  /** the caps of the network pricing, found by scheme */
  private final PriceCaps caps;

  /** the total paid in each cap's current period */
  private final Map<PriceCap, Double> currentTotals = new IdentityHashMap<>();

  /** the use count of each cap's current period */
  private final Map<PriceCap, Integer> useCounts = new IdentityHashMap<>();

  /**
   * Constructor, with nothing paid and no use made yet
   *
   * @param caps of the network pricing
   */
  protected PriceCapTotalsImpl(PriceCaps caps) {
    this.caps = caps;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addToTotal(PricingScheme scheme, Mode mode, double cumulativePrice, boolean incrementUseCount) {
    for (var cap : caps.getByScheme(scheme)) {
      if (!cap.isPerUse() && cap.appliesTo(mode)) {
        currentTotals.merge(cap, cumulativePrice, Double::sum);
        if (incrementUseCount) {
          useCounts.merge(cap, 1, Integer::sum);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startPeriod(PricingPeriod period) {
    var started = period.findPeriodsEqualOrSmaller();
    currentTotals.keySet().removeIf(cap -> started.contains(cap.getPeriod()));
    useCounts.keySet().removeIf(cap -> started.contains(cap.getPeriod()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCurrentTotal(PriceCap cap) {
    return currentTotals.getOrDefault(cap, 0.0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getUseCount(PriceCap cap) {
    return useCounts.getOrDefault(cap, 0);
  }
}
