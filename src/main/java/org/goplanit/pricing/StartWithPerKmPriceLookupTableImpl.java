package org.goplanit.pricing;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.pricing.StartWithPerKmPriceLookupTable;

/**
 * The lookup table of a mode or location scheme, worked out from its pricing when made: the start per use by mode and
 * band, and the rate per km and the start per pass of each section by mode, band and section, with the section of each
 * priced link segment by link segment id
 * <p>
 * A route search asks per link segment, mode and iteration, so the table finds a link segment's section itself and
 * reads the amount from an array; outside every section the scheme's own rate applies. For example on WestConnex at
 * 08:00 a car on a link segment of the bridge section pays that section's rate, elsewhere the scheme's 0.64 per km.
 * </p>
 *
 * @author markr
 */
public class StartWithPerKmPriceLookupTableImpl extends PriceLookupTableImpl implements StartWithPerKmPriceLookupTable {

  /** generated UID */
  private static final long serialVersionUID = 4466129045718231870L;

  /** the start per use by mode id and band */
  private final double[][] startAmountsInCurrency;

  /** the rate per km by mode id, band and section index, index 0 outside every section */
  private final double[][][] perKmAmountsInCurrency;

  /** the start per pass by mode id, band and section index, index 0 outside every section */
  private final double[][][] sectionStartAmountsInCurrency;

  /** the section index of each priced link segment by link segment id, 0 outside every section */
  private final int[] sectionIndicesByLinkSegmentId;

  /**
   * The section index of a link segment
   *
   * @param linkSegment to look up
   * @return its section's position in the scheme's sections plus one, 0 when it lies in no section
   */
  private int findSectionIndex(LinkSegment linkSegment) {
    return linkSegment.getId() < sectionIndicesByLinkSegmentId.length ?
        sectionIndicesByLinkSegmentId[(int) linkSegment.getId()] : 0;
  }

  /**
   * An amount for a link segment from one of the tables by mode, time band and section, times the distance band's
   * factor
   *
   * @param amountsInCurrency the table, by mode id, time band and section index (0 outside every section)
   * @param mode using the link segment
   * @param timeBand index, see {@link #findTimeBandIndex}
   * @param distanceBand index, see {@link #findDistanceBandIndex}
   * @param linkSegment priced by the scheme
   * @return the amount, 0 for a mode the table does not hold
   */
  private double findAmountInCurrency(
      double[][][] amountsInCurrency,
      Mode mode,
      int timeBand,
      int distanceBand,
      LinkSegment linkSegment) {
    return isIndexed(mode) ?
        amountsInCurrency[(int) mode.getId()][timeBand][findSectionIndex(linkSegment)]
            * getDistanceBandFactor(distanceBand) : 0;
  }

  /**
   * Constructor, working out every amount of the scheme's pricing
   * <p>
   * For each mode the scheme charges and each band, the point price rule before any distance band, see
   * {@link SchemePricingImpl#findPointPriceRuleInTimeBand}: its start, its rate outside sections, and per section the
   * section's rate, else the scheme's, and the section's start, else none. Then the section of each priced link
   * segment, so a lookup by link segment is two array reads. A scheme with 2 bands and 3 sections holds 3 x 4 amounts
   * per mode and kind.
   * </p>
   *
   * @param scheme a mode or location scheme
   * @param modesMinimumPrices the minimum of one use by mode id, 0 when none
   * @param priceCapsPerUse the price cap per use by mode id, {@code Double.POSITIVE_INFINITY} when none
   */
  protected StartWithPerKmPriceLookupTableImpl(
      PricingSchemeImpl<StartWithPerKmPriceRule> scheme,
      double[] modesMinimumPrices,
      double[] priceCapsPerUse) {
    super(scheme.getPricing(), modesMinimumPrices, priceCapsPerUse);
    var pricing = (SchemePricingImpl<StartWithPerKmPriceRule>) scheme.getPricing();
    var timeBands = pricing.getTimeBands();
    var sections = scheme.getSectionsOrNone();
    int modeCount = modesMinimumPrices.length;
    this.startAmountsInCurrency = new double[modeCount][getTimeBandCount()];
    this.perKmAmountsInCurrency = new double[modeCount][getTimeBandCount()][sections.size() + 1];
    this.sectionStartAmountsInCurrency = new double[modeCount][getTimeBandCount()][sections.size() + 1];
    for (var mode : scheme.getChargedModes()) {
      int modeIndex = (int) mode.getId();
      for (int timeBand = 0; timeBand < getTimeBandCount(); ++timeBand) {
        var priceRule = pricing.findPointPriceRuleInTimeBand(mode,
            timeBand < timeBands.size() ? timeBands.get(timeBand) : null);
        startAmountsInCurrency[modeIndex][timeBand] = priceRule.getStartAmountInCurrency();
        perKmAmountsInCurrency[modeIndex][timeBand][0] = priceRule.getPerKmAmountInCurrency();
        for (int section = 0; section < sections.size(); ++section) {
          perKmAmountsInCurrency[modeIndex][timeBand][section
              + 1] = priceRule.findPerKmAmountInCurrency(sections.get(section));
          sectionStartAmountsInCurrency[modeIndex][timeBand][section
              + 1] = priceRule.findSectionStartAmountInCurrency(sections.get(section));
        }
      }
    }
    var sectionIndices = ListUtils.createIndexByIdentity(sections);
    int linkSegmentCount = (int) scheme.streamPricedLinkSegments().mapToLong(LinkSegment::getId).max().orElse(-1) + 1;
    this.sectionIndicesByLinkSegmentId = new int[linkSegmentCount];
    scheme.streamPricedLinkSegments().forEach(linkSegment -> {
      var section = scheme.findSection(linkSegment);
      sectionIndicesByLinkSegmentId[(int) linkSegment.getId()] = section == null ? 0 : sectionIndices.get(section) + 1;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getStartAmountInCurrency(Mode mode, int timeBand, int distanceBand) {
    return isIndexed(mode)
        ? startAmountsInCurrency[(int) mode.getId()][timeBand] * getDistanceBandFactor(distanceBand) : 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPerKmAmountInCurrency(Mode mode, int timeBand, int distanceBand, LinkSegment linkSegment) {
    return findAmountInCurrency(perKmAmountsInCurrency, mode, timeBand, distanceBand, linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getSectionStartAmountInCurrency(Mode mode, int timeBand, int distanceBand, LinkSegment linkSegment) {
    return findAmountInCurrency(sectionStartAmountsInCurrency, mode, timeBand, distanceBand, linkSegment);
  }
}
