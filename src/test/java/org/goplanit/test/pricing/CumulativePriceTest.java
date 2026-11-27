package org.goplanit.test.pricing;

import org.goplanit.pricing.GateFromToAmountImpl;
import org.goplanit.pricing.FromToGatesPriceRuleImpl;
import org.goplanit.pricing.FromToGatesSchemePricingImpl;
import org.goplanit.pricing.ModesMinimumPriceImpl;
import org.goplanit.pricing.PriceOverrideImpl;
import org.goplanit.pricing.PricingDistanceBandImpl;
import org.goplanit.pricing.PricingTimeBandImpl;
import org.goplanit.pricing.SectionPriceRuleImpl;
import org.goplanit.pricing.StartWithPerKmPriceRuleImpl;
import org.goplanit.pricing.StartWithPerKmSchemePricingImpl;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.LocationPricingScheme;
import org.goplanit.utils.pricing.ModesMinimumPrice;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingTimeBand;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import static org.goplanit.test.pricing.PricingTestUtils.WEEKDAYS;
import static org.goplanit.test.pricing.PricingTestUtils.at;
import static org.goplanit.test.pricing.PricingTestUtils.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the cumulative price of a use: what one use of a scheme costs in total, its start once, its km at their
 * rates and distance bands, its minimum and cap per use, and the caps over a period from a traveller's totals
 *
 * @author markr
 */
public class CumulativePriceTest extends NetworkPricingTestBase {

  /** precision of amounts compared */
  private static final double EPSILON = 1e-9;

  /** midday on a Tuesday, outside every band below */
  private static final TimeInWeekWithDayType MIDDAY = at(DayOfWeek.TUESDAY, 12);

  /**
   * A location scheme on m4, for the default modes
   *
   * @param basePriceRule of its pricing
   * @param timeBands of its pricing
   * @param distanceBands of its pricing
   * @param minimums of its pricing
   * @return the scheme
   */
  private LocationPricingScheme schemeOnM4(
      StartWithPerKmPriceRule basePriceRule,
      List<PricingTimeBand<StartWithPerKmPriceRule>> timeBands,
      List<PricingDistanceBand> distanceBands,
      List<ModesMinimumPrice> minimums) {
    return pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(), List.of(m4),
        List.of(), new StartWithPerKmSchemePricingImpl(basePriceRule, List.of(), timeBands, distanceBands, minimums))
        .first();
  }

  /**
   * A start of 0.50, 0.45 per km and a minimum of 2.00: a use of 3 km costs 2.00, one of 10 km 5.00 ("Minimum lifts a
   * short use")
   */
  @Test
  public void minimumLiftsAShortUse() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(0.50, 0.45, List.of()), List.of(), List.of(),
        List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.unspecified())));

    assertEquals(2.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 3, Map.of()), EPSILON);
    assertEquals(5.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 10, Map.of()), EPSILON);
  }

  /** 0.45 per km and a cap of 5.00 per use: a use of 20 km costs 5.00 ("Cap per use") */
  @Test
  public void capPerUse() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(null, 0.45, List.of()), List.of(), List.of(), List.of());
    pricing.getCaps().getFactory().registerNew(List.of(scheme), 5.00, null, PricingPeriod.USE,
        PricedModeSelection.unspecified());

    assertEquals(5.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 20, Map.of()), EPSILON);
  }

  /**
   * 0.10 per km and a daily distance band up to 20 km with factor 0: a use of 30 km after 15 km travelled that day
   * costs 25 x 0.10 = 2.50 ("Distance band crossed inside a use")
   */
  @Test
  public void distanceBandCrossedInsideAUse() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(null, 0.10, List.of()), List.of(),
        List.of(new PricingDistanceBandImpl(PricingPeriod.DAY, null, 20.0, 0)), List.of());

    assertEquals(2.50, pricing.computeCumulativePrice(scheme, car, MIDDAY, 15, 30, Map.of()), EPSILON);
  }

  /**
   * A start of 1.00 and 0.64 per km, and a section with a start of 0.30 and 0.40 per km: a use of 5 km, 2 of them in
   * the section, costs 1.00 + 0.30 + 2 x 0.40 + 3 x 0.64 = 4.02 ("Start once and a section's start per pass")
   */
  @Test
  public void startOnceAndASectionsStartPerPass() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(1, 2)), List.of());
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(1.00, 0.64,
            List.of(new SectionPriceRuleImpl(section, 0.30, 0.40))), List.of(), List.of(), List.of(),
                List.of())).first();

    assertEquals(4.02, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 5, Map.of(section, 2.0)), EPSILON);
  }

  /**
   * A peak band from 07:00 to 09:00 with a start of 4.41 and a base start of 2.76: a use starting at 08:55, whatever
   * time it ends, costs 4.41 ("Priced at the moment the use starts")
   */
  @Test
  public void pricedAtTheMomentTheUseStarts() {
    var peak = new PricingTimeBandImpl<StartWithPerKmPriceRule>("peak", List.of(entry(WEEKDAYS, 420, 120)),
        new StartWithPerKmPriceRuleImpl(4.41, null, List.of()), List.of());
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(2.76, null, List.of()), List.of(peak), List.of(),
        List.of());

    assertEquals(4.41, pricing.computeCumulativePrice(scheme, car, at(DayOfWeek.TUESDAY, 8, 55), 0, 12, Map.of()),
        EPSILON);
  }

  /**
   * 0.10 per km, a daily distance band up to 20 km with factor 0 and a minimum of 2.00: a use of 3 km after 0 km that
   * day costs nothing ("Free use under a minimum")
   */
  @Test
  public void freeUseUnderAMinimum() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(null, 0.10, List.of()), List.of(),
        List.of(new PricingDistanceBandImpl(PricingPeriod.DAY, null, 20.0, 0)),
        List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.unspecified())));

    assertEquals(0, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 3, Map.of()), EPSILON);
  }

  /**
   * A day cap of 10.00 and a minimum of 2.00, with 9.00 paid that day: a use that would cost 3.00 costs 1.00 ("Day cap
   * nearly reached")
   */
  @Test
  public void dayCapNearlyReached() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(3.00, null, List.of()), List.of(), List.of(),
        List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.unspecified())));
    pricing.getCaps().getFactory().registerNew(List.of(scheme), 10.00, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    var totals = pricing.getCaps().createPriceCapTotals();
    totals.addToTotal(scheme, car, 9.00, true);

    assertEquals(1.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of(), totals), EPSILON);
  }

  /**
   * A cap allowing one charged use a day: after one use that day a second costs nothing, and a use after the next day
   * starts costs its price rule again ("Once a day, second use")
   */
  @Test
  public void onceADaySecondUse() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(5.00, null, List.of()), List.of(), List.of(), List.of());
    pricing.getCaps().getFactory().registerNew(List.of(scheme), null, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    var totals = pricing.getCaps().createPriceCapTotals();

    double first = pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of(), totals);
    totals.addToTotal(scheme, car, first, true);
    assertEquals(5.00, first, EPSILON);
    assertEquals(0, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of(), totals), EPSILON);

    totals.startPeriod(PricingPeriod.DAY);
    assertEquals(5.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of(), totals), EPSILON);
  }

  /**
   * A cap allowing one charged use a day, with 5.00 added to the total without counting a use: the total holds it, the
   * use count stays 0, and the next use is still the first of the day, costing its price
   */
  @Test
  public void addedToTotalWithoutCountingAUse() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(5.00, null, List.of()), List.of(), List.of(), List.of());
    var cap = pricing.getCaps().getFactory().registerNew(List.of(scheme), null, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    var totals = pricing.getCaps().createPriceCapTotals();
    totals.addToTotal(scheme, car, 5.00, false);

    assertEquals(5.00, totals.getCurrentTotal(cap), EPSILON);
    assertEquals(0, totals.getUseCount(cap));
    assertEquals(5.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of(), totals), EPSILON);
  }

  /** A day cap of 10.00 and a use that would cost 12.00: without totals it costs 10.00, as the first use of the day */
  @Test
  public void noTotalsGiven() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(12.00, null, List.of()), List.of(), List.of(), List.of());
    pricing.getCaps().getFactory().registerNew(List.of(scheme), 10.00, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());

    assertEquals(10.00, pricing.computeCumulativePrice(scheme, car, MIDDAY, 0, 1, Map.of()), EPSILON);
  }

  /**
   * An entry-exit scheme on m8 from its south gate to its north gate at 4.90 with a factor of 3 for heavy goods: a
   * truck using it from south to north pays 14.70, and nothing for a pair no price rule lists
   */
  @Test
  public void entryExitUse() {
    var south = pricing.getGates().getFactory().registerNew(List.of(segment(2, 3)), List.of());
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var scheme = pricing.getEntryExitSchemes().getFactory().registerNew("m8", PricedModeSelection.unspecified(),
        List.of(m8), List.of(south, north), new FromToGatesSchemePricingImpl(
            new FromToGatesPriceRuleImpl(List.of(new GateFromToAmountImpl(south, north, 4.90))),
            List.of(new PriceOverrideImpl<>(PricedModeSelection.named(List.of(heavyGoods)), null, 3.0)), List.of(),
            List.of(), List.of())).first();

    assertEquals(14.70, pricing.computeCumulativePrice(scheme, heavyGoods, MIDDAY, 0, south, north), EPSILON);
    assertEquals(0, pricing.computeCumulativePrice(scheme, heavyGoods, MIDDAY, 0, north, south), EPSILON);
  }

  /** A mode the scheme does not charge pays nothing, minimum or not */
  @Test
  public void modeNotChargedPaysNothing() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(1.00, 0.50, List.of()), List.of(), List.of(),
        List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.unspecified())));

    assertEquals(0, pricing.computeCumulativePrice(scheme, bicycle, MIDDAY, 0, 10, Map.of()), EPSILON);
  }

  /**
   * A cap of 5.00 per use for cars, a day cap of 10.00 and a cap allowing one charged use a day: the first is per use
   * and applies to cars only, capping 7.00 at 5.00; with 9.00 paid that day the day cap leaves 1.00 of a 3.00 use, and
   * after one counted use the count cap leaves nothing; without totals each caps as for the first use of the day
   */
  @Test
  public void capsTellTheirModesAndCapAPrice() {
    var scheme = schemeOnM4(new StartWithPerKmPriceRuleImpl(3.00, null, List.of()), List.of(), List.of(), List.of());
    var caps = pricing.getCaps().getFactory();
    var perUse = caps.registerNew(List.of(scheme), 5.00, null, PricingPeriod.USE,
        PricedModeSelection.named(List.of(car)));
    var perDay = caps.registerNew(List.of(scheme), 10.00, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    var onceADay = caps.registerNew(List.of(scheme), null, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    assertTrue(perUse.isPerUse());
    assertFalse(perDay.isPerUse());
    assertTrue(perUse.appliesTo(car));
    assertFalse(perUse.appliesTo(heavyGoods));
    assertEquals(5.00, perUse.computeCappedPrice(7.00, null), EPSILON);

    var totals = pricing.getCaps().createPriceCapTotals();
    totals.addToTotal(scheme, car, 9.00, false);
    assertEquals(1.00, perDay.computeCappedPrice(3.00, totals), EPSILON);
    assertEquals(3.00, perDay.computeCappedPrice(3.00, null), EPSILON);
    assertEquals(3.00, onceADay.computeCappedPrice(3.00, totals), EPSILON);
    totals.addToTotal(scheme, car, 0, true);
    assertEquals(0, onceADay.computeCappedPrice(3.00, totals), EPSILON);
    assertEquals(3.00, onceADay.computeCappedPrice(3.00, null), EPSILON);
  }
}
