package org.goplanit.pricing;

import java.util.List;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.pricing.PriceRule;
import org.goplanit.utils.pricing.PriceOverride;
import org.goplanit.utils.pricing.PricingTimeBand;
import org.goplanit.utils.pricing.PricingTimeBandEntry;

/**
 * A time band of a scheme's pricing: when it applies, and its price rule, its overrides by mode, or both
 * <p>
 * For example a morning peak on weekdays from 06:30 to 09:30 and an evening peak from 16:00 to 19:00, priced alike.
 * What its price rule leaves out comes from the base price rule. A band without entries, or with neither a price rule
 * nor overrides, is refused when made, as it would never apply or never change anything. Its price rule and overrides
 * are of its scheme's kind of price rule.
 * </p>
 *
 * @param <P> the kind of price rule
 * @author markr
 */
public class PricingTimeBandImpl<P extends PriceRule<P>> implements PricingTimeBand<P> {

  /** generated UID */
  private static final long serialVersionUID = 7810243059436785261L;

  /** the id of the band in the file */
  private final String xmlId;

  /** when the band applies */
  private final List<PricingTimeBandEntry> entries;

  /** the band's price rule, null when it gives none */
  private final P priceRule;

  /** the band's overrides of its price rule */
  private final List<PriceOverride<P>> priceOverrides;

  /**
   * Constructor
   *
   * @param xmlId of the band in the file, unique within its scheme
   * @param entries when the band applies, at least one
   * @param priceRule of the band, null when it gives none
   * @param priceOverrides of the band, may be empty
   * @throws IllegalArgumentException when there are no entries, or neither a price rule nor overrides
   */
  public PricingTimeBandImpl(
      String xmlId,
      List<? extends PricingTimeBandEntry> entries,
      P priceRule,
      List<? extends PriceOverride<P>> priceOverrides) {

    ExceptionUtils.throwIllegalArgumentIf(entries.isEmpty(), "Time band %s refused, it has no entries", xmlId);
    ExceptionUtils.throwIllegalArgumentIf(priceRule == null && priceOverrides.isEmpty(),
        "Time band %s refused, it gives a price rule, overrides or both", xmlId);

    this.xmlId = xmlId;
    this.entries = List.copyOf(entries);
    this.priceRule = priceRule;
    this.priceOverrides = List.copyOf(priceOverrides);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingTimeBandEntry> getEntries() {
    return entries;
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
  public List<PriceOverride<P>> getPriceOverrides() {
    return priceOverrides;
  }
}
