package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.SectionPriceRule;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.pricing.PricingSchemeGate;

/**
 * The price rule of a mode or location scheme: a start per use, a rate per km outside sections, and the price rules of
 * sections, each of which may be left out
 * <p>
 * A time band's price rule leaves out what it does not change, and gets it from the base price rule; an amount override
 * replaces only the amounts it gives. So working out the point price rule for a mode at a moment combines price rules
 * of this kind, which these methods do, each giving a new price rule: this price rule filled in from another,
 * multiplied by a factor, and with what it left out made explicit, an amount as zero and a section's rate as the price
 * rule's own.
 * </p>
 *
 * @author markr
 */
public class StartWithPerKmPriceRuleImpl implements StartWithPerKmPriceRule {

  /** generated UID */
  private static final long serialVersionUID = -6839014325670195421L;

  /**
   * An amount multiplied by a factor, left out when it is left out
   *
   * @param value to multiply, may be null
   * @param factor to multiply by
   * @return the result, null when the amount is null
   */
  private static Double multiplyWith(Double value, double factor) {
    return value == null ? null : value * factor;
  }

  /**
   * One amount, or the other when it is left out
   *
   * @param value preferred, may be null
   * @param fallback used when it is null, may be null
   * @return the amount
   */
  private static Double orElse(Double value, Double fallback) {
    return value != null ? value : fallback;
  }

  /** charged once per use, null when left out */
  private final Double startAmountInCurrency;

  /** charged per km outside every section, null when left out */
  private final Double perKmAmountInCurrency;

  /** the price rules of sections */
  private final List<SectionPriceRule> sectionPriceRules;

  /**
   * Create this price rule with its amounts outside sections, and its section price rules each changed
   *
   * @param newStartAmountInCurrency the new start
   * @param newPerKmAmountInCurrency the new rate per km
   * @param sectionPriceRuleChange applied to each section price rule, null to drop it
   * @return the new price rule
   */
  private StartWithPerKmPriceRuleImpl createChanged(
      Double newStartAmountInCurrency,
      Double newPerKmAmountInCurrency,
      Function<SectionPriceRule, SectionPriceRule> sectionPriceRuleChange) {
    return new StartWithPerKmPriceRuleImpl(newStartAmountInCurrency, newPerKmAmountInCurrency,
        sectionPriceRules.stream()
            .map(sectionPriceRuleChange)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Each amount and section price rule this one leaves out is taken from the other, and a section price rule both give
   * is filled in amount by amount.
   * </p>
   * <p>
   * A time band's price rule, and an override's amounts, say only what they change. For example a morning band setting
   * one section's rate keeps the base price rule's start, rate and other sections, and a motorcycle override setting
   * only the start keeps the band's rates. Section price rules keep the other's order, followed by those only this one
   * gives.
   * </p>
   *
   */
  @Override
  public StartWithPerKmPriceRuleImpl createFilledInFrom(StartWithPerKmPriceRule fallback) {
    var filledIn = new ArrayList<SectionPriceRule>(fallback.getSectionPriceRules().size());
    for (var fallbackSectionPriceRule : fallback.getSectionPriceRules()) {
      var given = findSectionPriceRule(fallbackSectionPriceRule.getSection());
      filledIn.add(given == null ? fallbackSectionPriceRule : new SectionPriceRuleImpl(given.getSection(),
          orElse(given.getStartAmountInCurrency(), fallbackSectionPriceRule.getStartAmountInCurrency()),
          orElse(given.getPerKmAmountInCurrency(), fallbackSectionPriceRule.getPerKmAmountInCurrency())));
    }
    sectionPriceRules.stream()
        .filter(sectionPriceRule -> fallback.findSectionPriceRule(sectionPriceRule.getSection()) == null)
        .forEach(filledIn::add);
    return new StartWithPerKmPriceRuleImpl(orElse(startAmountInCurrency, fallback.getStartAmountInCurrency()),
        orElse(perKmAmountInCurrency, fallback.getPerKmAmountInCurrency()), filledIn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StartWithPerKmPriceRuleImpl createMultipliedWith(double factor) {
    return createChanged(multiplyWith(startAmountInCurrency, factor), multiplyWith(perKmAmountInCurrency, factor),
        sectionPriceRule -> new SectionPriceRuleImpl(sectionPriceRule.getSection(),
            multiplyWith(sectionPriceRule.getStartAmountInCurrency(), factor),
            multiplyWith(sectionPriceRule.getPerKmAmountInCurrency(), factor)));
  }

  /**
   * {@inheritDoc}
   * <p>
   * A start or rate left out is given as zero, a section's start left out as zero and its rate left out as the price
   * rule's rate, never the price rule's start.
   * </p>
   */
  @Override
  public StartWithPerKmPriceRuleImpl createWithLeftOutAmountsGiven() {
    double givenPerKmAmountInCurrency = orElse(perKmAmountInCurrency, 0.0);
    return createChanged(orElse(startAmountInCurrency, 0.0), givenPerKmAmountInCurrency,
        sectionPriceRule -> new SectionPriceRuleImpl(sectionPriceRule.getSection(),
            orElse(sectionPriceRule.getStartAmountInCurrency(), 0.0),
            orElse(sectionPriceRule.getPerKmAmountInCurrency(), givenPerKmAmountInCurrency)));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Its section price rules refer to the mapped sections, a section mapped to null dropping its section price rule; it
   * refers to no gate.
   * </p>
   */
  @Override
  public StartWithPerKmPriceRuleImpl createWithSectionsAndGatesMapped(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    return createChanged(startAmountInCurrency, perKmAmountInCurrency, sectionPriceRule -> {
      var section = sectionMapping.apply(sectionPriceRule.getSection());
      return section == null ? null : new SectionPriceRuleImpl(section, sectionPriceRule.getStartAmountInCurrency(),
          sectionPriceRule.getPerKmAmountInCurrency());
    });
  }

  /**
   * Constructor
   *
   * @param startAmountInCurrency charged once per use, null when left out
   * @param perKmAmountInCurrency charged per km outside every section, null when left out
   * @param sectionPriceRules the price rules of sections, may be empty
   */
  public StartWithPerKmPriceRuleImpl(
      Double startAmountInCurrency,
      Double perKmAmountInCurrency,
      List<? extends SectionPriceRule> sectionPriceRules) {
    this.startAmountInCurrency = startAmountInCurrency;
    this.perKmAmountInCurrency = perKmAmountInCurrency;
    this.sectionPriceRules = List.copyOf(sectionPriceRules);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SectionPriceRule> getSectionPriceRules() {
    return sectionPriceRules;
  }
}
