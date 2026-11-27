package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.ExternalIdAbleUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.ModesMinimumPrice;
import org.goplanit.utils.pricing.PriceRule;
import org.goplanit.utils.pricing.PriceOverride;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.PricingTimeBand;
import org.goplanit.utils.problem.Problems;

/**
 * The price rules of a scheme, of one kind: a base price rule, overrides by mode, time bands, distance bands and
 * minimums, any of which may be absent
 * <p>
 * A scheme's price rules change with the time of day, the mode and the distance travelled, while most of a band or
 * override is the same as the base. So each part says only what it changes, and the point price rule for a mode at a
 * moment is worked out when asked, by the steps of {@link #getPointPriceRule}. The steps are the same for both kinds of
 * price rule, while how two price rules combine is each kind's own: each kind has its pricing,
 * {@link StartWithPerKmSchemePricingImpl} and {@link FromToGatesSchemePricingImpl}, giving the steps its kind's
 * combining. The parts cannot be changed once made; the checks across them, such as bands overlapping or a mode in two
 * overrides of one band, are its scheme's, when the scheme is registered.
 * </p>
 *
 * @param <P> the kind of price rule
 * @author markr
 */
public abstract class SchemePricingImpl<P extends PriceRule<P>> implements SchemePricing<P> {

  /** generated UID */
  private static final long serialVersionUID = -1384426970153879215L;

  /**
   * The overrides limited to modes charged, see {@link #limitToModes(Object, PricedModeSelection, Collection,
   * Function, Problems, String, String)}
   *
   * @param <P> the kind of price rule
   * @param overrides to limit
   * @param chargedModes the modes the scheme charges
   * @param problems to record each mode or override dropped in
   * @param subject where the overrides sit, such as a band's id, empty for the base
   * @return the overrides kept
   */
  private static <P extends PriceRule<P>> List<PriceOverride<P>> limitToModes(
      List<PriceOverride<P>> overrides,
      Collection<Mode> chargedModes,
      Problems<PricingProblemKind> problems,
      String subject) {

    return overrides.stream()
        .map(priceOverride -> limitToModes(priceOverride, priceOverride.getPricedModeOverrides(), chargedModes,
            modes -> (PriceOverride<P>) new PriceOverrideImpl<>(modes, priceOverride.getPriceRule(),
                priceOverride.getPriceOverrideFactor()),
            problems, subject, "override"))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /** the base price rule, null when there is none */
  private final P basePriceRule;

  /** the overrides of the base price rule */
  private final List<PriceOverride<P>> priceOverrides;

  /** the time bands */
  private final List<PricingTimeBand<P>> timeBands;

  /** the distance bands */
  private final List<PricingDistanceBand> distanceBands;

  /** the minimums of one use */
  private final List<ModesMinimumPrice> modesMinimumPrices;

  /**
   * Create an override referring to other sections and gates, see
   * {@link #createWithSectionsAndGatesMapped(UnaryOperator, UnaryOperator)}: one giving a factor stays as it is, one
   * whose price rule is left giving no amount is dropped
   *
   * @param priceOverride to map
   * @param sectionMapping from each section to the one to refer to, null to drop it
   * @param gateMapping from each gate to the one to refer to, null to drop it
   * @return the new override, null when it is dropped
   */
  private PriceOverride<P> createWithSectionsAndGatesMapped(
      PriceOverride<P> priceOverride,
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    if (priceOverride.getPriceRule() == null) {
      return priceOverride;
    }
    var mappedPriceRule = priceOverride.getPriceRule().createWithSectionsAndGatesMapped(sectionMapping, gateMapping);
    return mappedPriceRule.givesNoAmount() ? null :
        new PriceOverrideImpl<>(priceOverride.getPricedModeOverrides(), mappedPriceRule, null);
  }

  /**
   * Create one price rule filled in from another, either of which may be absent
   *
   * @param priceRule to fill in, may be null
   * @param fallback to fill in from, may be null
   * @return the price rule filled in, the one given when the other is null, null when both are null
   */
  private P createFilledInWhereGiven(P priceRule, P fallback) {
    if (priceRule == null || fallback == null) {
      return priceRule == null ? fallback : priceRule;
    }
    return priceRule.createFilledInFrom(fallback);
  }

  /**
   * Create a price rule referring to other sections and gates, which may be absent
   *
   * @param priceRule to map, may be null
   * @param sectionMapping from each section to the one to refer to, null to drop it
   * @param gateMapping from each gate to the one to refer to, null to drop it
   * @return the new price rule, null when the price rule is null
   */
  private P createWithSectionsAndGatesMapped(
      P priceRule,
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    return priceRule == null ? null : priceRule.createWithSectionsAndGatesMapped(sectionMapping, gateMapping);
  }

  /**
   * A part of a scheme naming modes, such as an override, a minimum or a cap, limited to the modes the scheme charges:
   * it loses the modes it names that are not charged, each recorded as a problem, and is dropped when none is left.
   * A part naming no modes covers whatever the scheme charges and is kept as it is
   *
   * @param <T> type of the part
   * @param part to limit
   * @param modes the part names
   * @param chargedModes the modes the scheme charges
   * @param withModes makes the part again with fewer modes named
   * @param problems to record each mode or part dropped in
   * @param subject where the part sits, such as a band's id, empty when nowhere in particular
   * @param partName what the part is, such as "override", for the problems
   * @return the part limited, the part itself when nothing was dropped, null when it is dropped
   */
  protected static <T> T limitToModes(
      T part,
      PricedModeSelection modes,
      Collection<Mode> chargedModes,
      Function<PricedModeSelection, T> withModes,
      Problems<PricingProblemKind> problems,
      String subject,
      String partName) {

    var named = new ArrayList<>(modes.getPricedModes());
    var dropped = ContainerUtils.removeMatching(named, mode -> !ListUtils.containsByIdentity(chargedModes, mode));
    if (dropped.isEmpty()) {
      return part;
    }
    problems.add(PricingProblemKind.MODE_NOT_CHARGED_BY_SCHEME, "", subject,
        partName + " for " + ExternalIdAbleUtils.toIdsAsString(dropped));
    problems.addIf(
        named.isEmpty(), PricingProblemKind.LEFT_WITHOUT_MODES, "", subject, partName + " dropped");
    return named.isEmpty() ? null : withModes.apply(PricedModeSelection.named(named));
  }

  /**
   * A price rule of this kind charging nothing, for a mode neither a band nor the base prices
   *
   * @return the price rule
   */
  protected abstract P createPriceRuleChargingNothing();

  /**
   * A pricing of this kind from its parts, for a pricing limited or mapped
   *
   * @param newBasePriceRule for every mode the scheme charges outside its time bands, null when there is none
   * @param newPriceOverrides of the base price rule, may be empty
   * @param newTimeBands may be empty
   * @param newDistanceBands may be empty
   * @param newMinimums of one use, may be empty
   * @return the pricing
   */
  protected abstract SchemePricingImpl<P> createSchemePricing(
      P newBasePriceRule,
      List<PriceOverride<P>> newPriceOverrides,
      List<PricingTimeBand<P>> newTimeBands,
      List<PricingDistanceBand> newDistanceBands,
      List<ModesMinimumPrice> newMinimums);

  /**
   * Create this pricing limited to the modes its scheme charges: overrides and minimums naming a mode it does not
   * charge lose it, one left without modes is dropped, and a time band left with neither a price rule nor overrides
   * goes with it
   * <p>
   * A scheme on a truck-only toll road that names car and trucks charges trucks only, so an override for cars says
   * nothing. Each mode and part dropped is recorded as a problem of the scheme.
   * </p>
   *
   * @param chargedModes the modes the scheme charges
   * @param problems to record each mode or part dropped in
   * @return the pricing limited, this one when nothing was dropped
   */
  protected SchemePricingImpl<P> createLimitedToModes(
      Collection<Mode> chargedModes,
      Problems<PricingProblemKind> problems) {
    int problemsBefore = problems.size();
    var keptOverrides = limitToModes(priceOverrides, chargedModes, problems, "");
    var keptBands = new ArrayList<PricingTimeBand<P>>(timeBands.size());
    for (var band : timeBands) {
      var bandOverrides = limitToModes(band.getPriceOverrides(), chargedModes, problems, band.getXmlId());
      if (band.getPriceRule() != null || !bandOverrides.isEmpty()) {
        keptBands.add(bandOverrides.equals(band.getPriceOverrides()) ? band :
            new PricingTimeBandImpl<>(band.getXmlId(), band.getEntries(), band.getPriceRule(), bandOverrides));
      }
    }
    var keptMinimums = modesMinimumPrices.stream()
        .map(modesMinimumPrice -> limitToModes(modesMinimumPrice, modesMinimumPrice.getPricedModeSelection(),
            chargedModes,
            modes -> (ModesMinimumPrice) new ModesMinimumPriceImpl(modesMinimumPrice.getAmountInCurrency(), modes),
            problems, "", "minimum"))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return problems.size() == problemsBefore ? this :
        createSchemePricing(basePriceRule, keptOverrides, keptBands, distanceBands, keptMinimums);
  }

  /**
   * Create this pricing referring to other sections and gates, such as their copies in a copy of the network pricing; a
   * section or gate mapped to null drops the section price rules and gate pairs referring to it, an override left
   * giving no amount, and a time band left with neither a price rule nor overrides
   *
   * @param sectionMapping from each section to the one to refer to, null to drop it
   * @param gateMapping from each gate to the one to refer to, null to drop it
   * @return the new pricing
   */
  protected SchemePricingImpl<P> createWithSectionsAndGatesMapped(
      UnaryOperator<PricingSchemeSection> sectionMapping,
      UnaryOperator<PricingSchemeGate> gateMapping) {
    Function<List<PriceOverride<P>>, List<PriceOverride<P>>> mapOverrides = overrides -> overrides.stream()
        .map(priceOverride -> createWithSectionsAndGatesMapped(priceOverride, sectionMapping, gateMapping))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    var mappedBands = new ArrayList<PricingTimeBand<P>>(timeBands.size());
    for (var band : timeBands) {
      var bandPriceRule = createWithSectionsAndGatesMapped(band.getPriceRule(), sectionMapping, gateMapping);
      var bandOverrides = mapOverrides.apply(band.getPriceOverrides());
      if (bandPriceRule != null || !bandOverrides.isEmpty()) {
        mappedBands.add(new PricingTimeBandImpl<>(band.getXmlId(), band.getEntries(), bandPriceRule, bandOverrides));
      }
    }
    return createSchemePricing(createWithSectionsAndGatesMapped(basePriceRule, sectionMapping, gateMapping),
        mapOverrides.apply(priceOverrides), mappedBands, distanceBands, modesMinimumPrices);
  }

  /**
   * The point price rule for a mode in a time band, or outside every band, before any distance band's factor: the
   * band's price rule filled in from the base price rule, then the mode's override of the band, else its override of
   * the base, its price rule filling in or its factor multiplying; every amount given
   * <p>
   * The steps of {@link #getPointPriceRule} that do not depend on the distance, so a lookup table can work them out
   * once per mode and band and multiply in a distance band's factor when asked.
   * </p>
   *
   * @param mode priced
   * @param timeBand the band, null for outside every band
   * @return the price rule, charging nothing for a mode neither the band nor the base prices
   */
  protected P findPointPriceRuleInTimeBand(Mode mode, PricingTimeBand<P> timeBand) {
    var priceRule = timeBand == null ? basePriceRule : createFilledInWhereGiven(timeBand.getPriceRule(), basePriceRule);
    var priceOverride = timeBand == null ? null : timeBand.findPriceOverride(mode);
    if (priceOverride == null) {
      priceOverride = findPriceOverride(mode);
    }
    if (priceOverride != null && priceOverride.getPriceRule() != null) {
      priceRule = createFilledInWhereGiven(priceOverride.getPriceRule(), priceRule);
    } else if (priceOverride != null && priceRule != null) {
      priceRule = priceRule.createMultipliedWith(priceOverride.getPriceOverrideFactor());
    }
    return priceRule == null ? createPriceRuleChargingNothing() : priceRule.createWithLeftOutAmountsGiven();
  }

  /**
   * Constructor
   *
   * @param basePriceRule for every mode the scheme charges outside its time bands, null when there is none
   * @param priceOverrides of the base price rule, may be empty
   * @param timeBands may be empty
   * @param distanceBands may be empty
   * @param minimums of one use, may be empty
   */
  protected SchemePricingImpl(
      P basePriceRule,
      List<? extends PriceOverride<P>> priceOverrides,
      List<? extends PricingTimeBand<P>> timeBands,
      List<? extends PricingDistanceBand> distanceBands,
      List<? extends ModesMinimumPrice> minimums) {
    this.basePriceRule = basePriceRule;
    this.priceOverrides = List.copyOf(priceOverrides);
    this.timeBands = List.copyOf(timeBands);
    this.distanceBands = List.copyOf(distanceBands);
    this.modesMinimumPrices = List.copyOf(minimums);
  }

  /**
   * {@inheritDoc}
   * <p>
   * An override's price rule for the mode stands on its own where neither the band nor the base gives a price rule,
   * such as a band pricing trucks only; a factor then has nothing to multiply, and the mode pays nothing.
   * </p>
   */
  @Override
  public P getPointPriceRule(Mode mode, TimeInWeekWithDayType moment, double distanceTravelledForBinSelectionKm) {
    var priceRule = findPointPriceRuleInTimeBand(mode, findTimeBand(moment));
    double factor = computeDistanceBandFactor(distanceTravelledForBinSelectionKm);
    return factor == 1 ? priceRule : priceRule.createMultipliedWith(factor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public P getBasePriceRule() {
    return basePriceRule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PriceOverride<P>> getPriceOverrides() {
    return priceOverrides;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingTimeBand<P>> getTimeBands() {
    return timeBands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingDistanceBand> getDistanceBands() {
    return distanceBands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ModesMinimumPrice> getModesMinimumPrices() {
    return modesMinimumPrices;
  }
}
