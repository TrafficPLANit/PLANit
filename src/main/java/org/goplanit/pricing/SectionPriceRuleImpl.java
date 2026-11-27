package org.goplanit.pricing;

import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.SectionPriceRule;

/**
 * The price rule of one section of a scheme: an amount per pass of the section and a rate per km in it, either left out
 * <p>
 * For example a bridge charging 2.00 each time it is crossed on top of the scheme's rate. Its rate left out is the
 * price rule's rate, its start left out is zero.
 * </p>
 *
 * @author markr
 */
public class SectionPriceRuleImpl implements SectionPriceRule {

  /** generated UID */
  private static final long serialVersionUID = 6194057236820468321L;

  /** the section priced */
  private final PricingSchemeSection section;

  /** charged per pass, null when left out */
  private final Double startAmountInCurrency;

  /** charged per km in the section, null when left out */
  private final Double perKmAmountInCurrency;

  /**
   * Constructor
   *
   * @param section priced
   * @param startAmountInCurrency charged per pass, null when left out
   * @param perKmAmountInCurrency charged per km in the section, null when left out
   * @throws IllegalArgumentException when no section is given
   */
  public SectionPriceRuleImpl(
      PricingSchemeSection section,
      Double startAmountInCurrency,
      Double perKmAmountInCurrency) {
    ExceptionUtils.throwIllegalArgumentIf(section == null, "Section price rule refused, it prices a section");
    this.section = section;
    this.startAmountInCurrency = startAmountInCurrency;
    this.perKmAmountInCurrency = perKmAmountInCurrency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSection getSection() {
    return section;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getStartAmountInCurrency() {
    return startAmountInCurrency;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getPerKmAmountInCurrency() {
    return perKmAmountInCurrency;
  }
}
