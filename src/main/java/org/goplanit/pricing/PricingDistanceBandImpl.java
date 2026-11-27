package org.goplanit.pricing;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.misc.binning.Bin;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.pricing.PricingPeriod;

/**
 * A distance band of a scheme's pricing: while the distance travelled on the scheme in its period lies in its range,
 * everything the scheme charges is multiplied by its factor
 * <p>
 * For example the first 20 km of a day free, factor 0, and every km from 200 km on a quarter cheaper, factor 0.75. Its
 * range holds its start and not its end, from 0 when no start is given and without end when none is given. A band per
 * hour, or one starting at or beyond its end, is refused when made; its range is checked here so the refusal is an
 * illegal argument, as it is for every other part of a pricing.
 * </p>
 *
 * @author markr
 */
public class PricingDistanceBandImpl implements PricingDistanceBand {

  /** generated UID */
  private static final long serialVersionUID = -2783961044587261934L;

  /** the period the distance is counted over */
  private final PricingPeriod period;

  /** the range of distance travelled, in km */
  private final Bin<Double> range;

  /** the factor multiplying what the scheme charges in the range */
  private final double distanceBandFactor;

  /**
   * Constructor
   *
   * @param period the distance is counted over: use, day, week or month
   * @param fromKm start of the range, null for 0
   * @param toKm end of the range, null for no end
   * @param distanceBandFactor multiplying what the scheme charges in the range
   * @throws IllegalArgumentException when the period is an hour, or the range starts at or beyond its end
   */
  public PricingDistanceBandImpl(PricingPeriod period, Double fromKm, Double toKm, double distanceBandFactor) {
    ExceptionUtils.throwIllegalArgumentIf(period == PricingPeriod.HOUR,
        "Distance band per hour refused, its period is a use, day, week or month");
    double from = fromKm == null ? 0 : fromKm;
    double to = toKm == null ? Double.POSITIVE_INFINITY : toKm;
    ExceptionUtils.throwIllegalArgumentIf(from >= to,
        "Distance band from %s km to %s km refused, it starts before it "
        + "ends", from, to);
    this.period = period;
    this.range = Bin.of(from, to, true, false, null);
    this.distanceBandFactor = distanceBandFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingPeriod getPeriod() {
    return period;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bin<Double> getRange() {
    return range;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDistanceBandFactor() {
    return distanceBandFactor;
  }
}
