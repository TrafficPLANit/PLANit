package org.goplanit.test.pricing;

import org.goplanit.pricing.GateFromToAmountImpl;
import org.goplanit.pricing.FromToGatesPriceRuleImpl;
import org.goplanit.pricing.FromToGatesSchemePricingImpl;
import org.goplanit.pricing.ModesMinimumPriceImpl;
import org.goplanit.pricing.PriceOverrideImpl;
import org.goplanit.pricing.PricingTimeBandImpl;
import org.goplanit.pricing.SectionPriceRuleImpl;
import org.goplanit.pricing.StartWithPerKmPriceRuleImpl;
import org.goplanit.pricing.StartWithPerKmSchemePricingImpl;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import org.goplanit.utils.time.TimeInWeekWithDayType;

import static org.goplanit.test.pricing.PricingTestUtils.WEEKDAYS;
import static org.goplanit.test.pricing.PricingTestUtils.at;
import static org.goplanit.test.pricing.PricingTestUtils.entry;
import static org.goplanit.test.pricing.PricingTestUtils.distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests of a scheme's price rule lookup table: every amount it gives matches the point price rule for that mode, time
 * band and distance band, a link segment's rate is that of its section, and it holds each mode's minimum and cap per
 * use
 *
 * @author markr
 */
public class PriceLookupTableTest extends NetworkPricingTestBase {

  /** precision of amounts compared */
  private static final double EPSILON = 1e-9;

  /** a moment in each band of the pricings below, and one outside them: peak, night, midday */
  private static final List<TimeInWeekWithDayType> MOMENTS =
      List.of(at(DayOfWeek.TUESDAY, 8), at(DayOfWeek.TUESDAY, 23), at(DayOfWeek.TUESDAY, 12));

  /** a distance in each distance band of the pricings below, and one outside them */
  private static final List<Double> DISTANCES = List.of(10.0, 250.0, 100.0);

  /**
   * A location scheme on m4 with a base price rule, a section with price rules of its own, a factor override for heavy
   * goods, a peak band and a night band with an override, and two distance bands: for each charged mode, band and
   * distance band the table gives the start, rate and section start of the point price rule ("Table matches the point
   * price rule")
   */
  @Test
  public void tableMatchesThePointPriceOfAStartWithPerKmScheme() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(1, 2)), List.of());
    var peak = new PricingTimeBandImpl<StartWithPerKmPriceRule>("peak", List.of(entry(WEEKDAYS, 420, 120)),
        new StartWithPerKmPriceRuleImpl(4.41, null, List.of(new SectionPriceRuleImpl(section, null, 0.95))), List.of());
    var night = new PricingTimeBandImpl<StartWithPerKmPriceRule>("night", List.of(entry(Set.of(), 1260, 480)), null,
        List.of(new PriceOverrideImpl<>(PricedModeSelection.named(List.of(heavyGoods)), null, 1.0)));
    var schemePricing = new StartWithPerKmSchemePricingImpl(
        new StartWithPerKmPriceRuleImpl(2.76, 0.5, List.of(new SectionPriceRuleImpl(section, 1.0, 0.8))),
        List.of(new PriceOverrideImpl<>(PricedModeSelection.named(List.of(heavyGoods)), null, 3.0)),
        List.of(peak, night), distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km(), List.of());
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), schemePricing).first();

    var table = (StartWithPerKmPriceLookupTable) pricing.createPriceLookupTable(scheme);

    long timeBandCount = MOMENTS.stream()
        .map(table::findTimeBandIndex)
        .distinct()
        .count();
    assertEquals(3, timeBandCount);
    assertEquals(3, DISTANCES.stream()
        .map(table::findDistanceBandIndex)
        .distinct()
        .count());
    for (var mode : scheme.getChargedModes()) {
      for (var moment : MOMENTS) {
        for (var distance : DISTANCES) {
          var priceRule = scheme.getPricing().getPointPriceRule(mode, moment, distance);
          int timeBand = table.findTimeBandIndex(moment);
          int distanceBand = table.findDistanceBandIndex(distance);
          assertEquals(priceRule.getStartAmountInCurrency(), table.getStartAmountInCurrency(mode, timeBand,
              distanceBand),
              EPSILON);
          assertEquals(priceRule.getPerKmAmountInCurrency(), table.getPerKmAmountInCurrency(mode, timeBand,
              distanceBand,
              segment(0, 1)), EPSILON);
          assertEquals(0, table.getSectionStartAmountInCurrency(mode, timeBand, distanceBand, segment(0, 1)), EPSILON);
          var sectionPriceRule = priceRule.findSectionPriceRule(section);
          assertEquals(sectionPriceRule.getPerKmAmountInCurrency(), table.getPerKmAmountInCurrency(mode, timeBand,
              distanceBand, segment(1, 2)), EPSILON);
          assertEquals(sectionPriceRule.getStartAmountInCurrency(), table.getSectionStartAmountInCurrency(mode,
              timeBand,
              distanceBand, segment(1, 2)), EPSILON);
        }
      }
    }
  }

  /**
   * An entry-exit scheme on m8 from its south gate to its north gate at 4.90, 6.10 in the peak band, with a factor of
   * 3 for heavy goods and two distance bands: for each charged mode, band and distance band the table gives the amount
   * of the point price rule ("Table matches the point price rule")
   */
  @Test
  public void tableMatchesThePointPriceOfAFromToGatesScheme() {
    var south = pricing.getGates().getFactory().registerNew(List.of(segment(2, 3)), List.of());
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var peak = new PricingTimeBandImpl<FromToGatesPriceRule>("peak", List.of(entry(WEEKDAYS, 420, 120)),
        new FromToGatesPriceRuleImpl(List.of(new GateFromToAmountImpl(south, north, 6.10))), List.of());
    var schemePricing = new FromToGatesSchemePricingImpl(
        new FromToGatesPriceRuleImpl(List.of(new GateFromToAmountImpl(south, north, 4.90))),
        List.of(new PriceOverrideImpl<>(PricedModeSelection.named(List.of(heavyGoods)), null, 3.0)), List.of(peak),
        distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km(), List.of());
    var scheme = pricing.getEntryExitSchemes().getFactory().registerNew("m8", PricedModeSelection.unspecified(),
        List.of(m8), List.of(south, north), schemePricing).first();

    var table = (FromToGatesPriceLookupTable) pricing.createPriceLookupTable(scheme);

    for (var mode : scheme.getChargedModes()) {
      for (var moment : MOMENTS) {
        for (var distance : DISTANCES) {
          var priceRule = scheme.getPricing().getPointPriceRule(mode, moment, distance);
          assertEquals(priceRule.findByFromToGate(south, north).getAmountInCurrency(), table.getAmountInCurrency(mode,
              table.findTimeBandIndex(moment), table.findDistanceBandIndex(distance), south, north), EPSILON);
          assertEquals(0, table.getAmountInCurrency(mode, table.findTimeBandIndex(moment),
              table.findDistanceBandIndex(distance), north, south), EPSILON);
        }
      }
    }
  }

  /**
   * A location scheme with 0.64 per km and a section on 1-2 with 0.40 per km: the table gives 0.40 on 1-2 and 0.64 on
   * 0-1 ("Rate by link segment")
   */
  @Test
  public void rateByLinkSegment() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(1, 2)), List.of());
    var schemePricing = new StartWithPerKmSchemePricingImpl(
        new StartWithPerKmPriceRuleImpl(null, 0.64, List.of(new SectionPriceRuleImpl(section, null, 0.40))), List.of(),
        List.of(), List.of(), List.of());
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), schemePricing).first();

    var table = (StartWithPerKmPriceLookupTable) pricing.createPriceLookupTable(scheme);
    int timeBand = table.findTimeBandIndex(at(DayOfWeek.TUESDAY, 12));
    int distanceBand = table.findDistanceBandIndex(0);

    assertEquals(0.40, table.getPerKmAmountInCurrency(car, timeBand, distanceBand, segment(1, 2)), EPSILON);
    assertEquals(0.64, table.getPerKmAmountInCurrency(car, timeBand, distanceBand, segment(0, 1)), EPSILON);
  }

  /**
   * A scheme with a minimum of 2.00 for cars and a cap of 12.25 per use for cars: the table gives cars a minimum of
   * 2.00 and a cap per use of 12.25, and heavy goods neither; a mode the scheme does not charge pays nothing
   * ("Minimum and cap per use")
   */
  @Test
  public void minimumAndCapPerUse() {
    var schemePricing = new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(1.0, 0.5, List.of()),
        List.of(), List.of(), List.of(),
        List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.named(List.of(car)))));
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), schemePricing).first();
    pricing.getCaps().getFactory().registerNew(List.of(scheme), 12.25, null, PricingPeriod.USE,
        PricedModeSelection.named(List.of(car)));

    var table = (StartWithPerKmPriceLookupTable) pricing.createPriceLookupTable(scheme);

    assertEquals(2.00, table.getModesMinimumPrice(car), EPSILON);
    assertEquals(12.25, table.getPriceCapPerUse(car), EPSILON);
    assertEquals(0, table.getModesMinimumPrice(heavyGoods), EPSILON);
    assertEquals(Double.POSITIVE_INFINITY, table.getPriceCapPerUse(heavyGoods));
    assertFalse(scheme.getChargedModes().contains(bicycle));
    int timeBand = table.findTimeBandIndex(at(DayOfWeek.TUESDAY, 12));
    assertEquals(0, table.getStartAmountInCurrency(bicycle, timeBand, table.findDistanceBandIndex(0)), EPSILON);
    assertEquals(0, table.getModesMinimumPrice(bicycle), EPSILON);
    assertEquals(Double.POSITIVE_INFINITY, table.getPriceCapPerUse(bicycle));
  }
}
