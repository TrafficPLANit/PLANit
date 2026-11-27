package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.PriceLookupTable;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.PricingTimeBand;
import org.goplanit.utils.containers.ListUtils;

/**
 * What a lookup table of either kind of price rule holds: the scheme's time bands and distance bands, to find a
 * moment's band and a distance's distance band, and each mode's minimum and cap per use
 * <p>
 * Arrays are by mode id, as ids do not change while a table is used, and a mode not charged pays nothing: no minimum
 * and a cap per use of {@code Double.POSITIVE_INFINITY}. A distance band's factor is multiplied in when an amount is
 * asked for, so the arrays of amounts of each kind have no dimension for distance bands. For example a scheme with a
 * morning and an evening band has three band indices, the last for outside every band.
 * </p>
 *
 * @author markr
 */
public abstract class PriceLookupTableImpl implements PriceLookupTable {

  /** generated UID */
  private static final long serialVersionUID = -8215730946613480257L;

  /** the time bands, in the pricing's order */
  private final List<? extends PricingTimeBand<?>> timeBands;

  /** the distance bands, in the pricing's order */
  private final List<PricingDistanceBand> distanceBands;

  /** the factor of each distance band, then 1 for outside every distance band */
  private final double[] distanceBandFactors;

  /** the minimum of one use by mode id, 0 when none */
  private final double[] modesMinimumPrices;

  /** the price cap per use by mode id, {@code Double.POSITIVE_INFINITY} when none */
  private final double[] priceCapsPerUse;

  /**
   * The number of band indices: one per time band and one for outside every band
   *
   * @return the number
   */
  protected int getTimeBandCount() {
    return timeBands.size() + 1;
  }

  /**
   * The factor of a distance band index
   *
   * @param distanceBand index, see {@link #findDistanceBandIndex}
   * @return the factor, 1 for outside every distance band
   */
  protected double getDistanceBandFactor(int distanceBand) {
    return distanceBandFactors[distanceBand];
  }

  /**
   * Whether the table has arrays for a mode, so a mode added to the network after the table was built pays nothing
   *
   * @param mode to check
   * @return true when its id is within the arrays
   */
  protected boolean isIndexed(Mode mode) {
    return mode.getId() < modesMinimumPrices.length;
  }

  /**
   * Constructor
   *
   * @param pricing of the scheme, giving its time bands and distance bands
   * @param modesMinimumPrices the minimum of one use by mode id, 0 when none
   * @param priceCapsPerUse the price cap per use by mode id, {@code Double.POSITIVE_INFINITY} when none
   */
  protected PriceLookupTableImpl(SchemePricing<?> pricing, double[] modesMinimumPrices, double[] priceCapsPerUse) {
    this.timeBands = List.copyOf(pricing.getTimeBands());
    this.distanceBands = List.copyOf(pricing.getDistanceBands());
    this.distanceBandFactors = new double[distanceBands.size() + 1];
    for (int index = 0; index < distanceBands.size(); ++index) {
      distanceBandFactors[index] = distanceBands.get(index).getDistanceBandFactor();
    }
    distanceBandFactors[distanceBands.size()] = 1;
    this.modesMinimumPrices = modesMinimumPrices.clone();
    this.priceCapsPerUse = priceCapsPerUse.clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int findTimeBandIndex(TimeInWeekWithDayType moment) {
    int index = ListUtils.findIndexOfFirstMatching(timeBands, timeBand -> timeBand.covers(moment));
    return index < 0 ? timeBands.size() : index;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int findDistanceBandIndex(double distanceTravelledForBinSelectionKm) {
    int index = ListUtils.findIndexOfFirstMatching(distanceBands,
        distanceBand -> distanceBand.contains(distanceTravelledForBinSelectionKm));
    return index < 0 ? distanceBands.size() : index;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getModesMinimumPrice(Mode mode) {
    return isIndexed(mode) ? modesMinimumPrices[(int) mode.getId()] : 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPriceCapPerUse(Mode mode) {
    return isIndexed(mode) ? priceCapsPerUse[(int) mode.getId()] : Double.POSITIVE_INFINITY;
  }
}
