package org.goplanit.test.pricing;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.pricing.*;
import org.goplanit.pricing.GateFromToAmountImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.goplanit.test.pricing.PricingTestUtils.WEEKDAYS;
import static org.goplanit.test.pricing.PricingTestUtils.at;
import static org.goplanit.test.pricing.PricingTestUtils.entry;
import static org.goplanit.test.pricing.PricingTestUtils.distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the point price rule for a mode on a scheme at a moment and distance: the time band, the base price rule
 * filling in what the band leaves out, the mode's override, and the distance band; and the parts of a pricing refusing
 * what makes no sense on its own
 *
 * @author markr
 */
public class SchemePricingTest {

  /** precision of amounts compared */
  private static final double EPSILON = 1e-9;

  /**
   * A price rule of amounts outside sections only
   *
   * @param start the start, null when left out
   * @param perKm the rate per km, null when left out
   * @param sectionPriceRules the section price rules
   * @return the price rule
   */
  private static StartWithPerKmPriceRule priceRule(Double start, Double perKm, SectionPriceRule... sectionPriceRules) {
    return new StartWithPerKmPriceRuleImpl(start, perKm, List.of(sectionPriceRules));
  }

  /**
   * An override by a factor
   *
   * @param <P> the kind of price rule
   * @param factor multiplying the price rule
   * @param modes named
   * @return the override
   */
  private static <P extends PriceRule<P>> PriceOverride<P> factorOverride(double factor, Mode... modes) {
    return new PriceOverrideImpl<>(PricedModeSelection.named(List.of(modes)), null, factor);
  }

  private Mode car;
  private Mode heavyGoods;
  private Mode motorBike;
  private PricingSchemeSectionsImpl sections;
  private PricingSchemeGatesImpl gates;
  private MacroscopicLinkSegment linkSegment;

  /**
   * A section with no link segments, as the price rule only refers to it
   *
   * @return the section
   */
  private PricingSchemeSection section() {
    return sections.getFactory().registerNew(List.of(), List.of());
  }

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    heavyGoods = network.getModes().getFactory().registerNew(PredefinedModeType.HEAVY_GOODS_VEHICLE);
    motorBike = network.getModes().getFactory().registerNew(PredefinedModeType.MOTOR_BIKE);
    sections = new PricingSchemeSectionsImpl(IdGroupingToken.collectGlobalToken());
    gates = new PricingSchemeGatesImpl(IdGroupingToken.collectGlobalToken());
    var layer = network.getTransportLayers().getFactory().registerNew();
    var link = layer.getLinks().getFactory().registerNew(
        layer.getNodes().getFactory().registerNew(), layer.getNodes().getFactory().registerNew(), 1, true);
    linkSegment = layer.getLinkSegments().getFactory().registerNew(link, true, true);
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /**
   * A base price rule with start 2.76 and a factor of 3 for heavy goods, a peak band on Tuesday 07:00 with start 4.41
   * and a shoulder band at 10:00 with 3.30: a truck pays 13.23 in the peak and 9.90 in the shoulder, a car 4.41 in the
   * peak ("Truck in a peak band with a base factor")
   */
  @Test
  public void truckInAPeakBandWithABaseFactor() {
    var peak = new PricingTimeBandImpl<>("peak", List.of(entry(Set.of(DayOfWeek.TUESDAY), 420, 60)), priceRule(4.41,
        null),
        List.of());
    var shoulder = new PricingTimeBandImpl<>("shoulder", List.of(entry(Set.of(DayOfWeek.TUESDAY), 600, 60)),
        priceRule(3.30,
        null), List.of());
    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(2.76, null), List.of(factorOverride(3, heavyGoods)),
        List.of(peak, shoulder), List.of(), List.of());

    assertEquals(13.23, pricing.getPointPriceRule(heavyGoods, at(DayOfWeek.TUESDAY, 7), 0).getStartAmountInCurrency(),
        EPSILON);
    assertEquals(9.90, pricing.getPointPriceRule(heavyGoods, at(DayOfWeek.TUESDAY, 10), 0).getStartAmountInCurrency(),
        EPSILON);
    assertEquals(4.41, pricing.getPointPriceRule(car, at(DayOfWeek.TUESDAY, 7), 0).getStartAmountInCurrency(), EPSILON);
    assertEquals(8.28, pricing.getPointPriceRule(heavyGoods, at(DayOfWeek.TUESDAY, 12), 0).getStartAmountInCurrency(),
        EPSILON);
  }

  /**
   * A base factor of 3 for heavy goods and a night band, 21:00 to 05:00, with a factor of 1 for heavy goods and no
   * price of its own: a truck pays the car price at 23:00 ("Band override replacing a base override")
   */
  @Test
  public void bandOverrideReplacingABaseOverride() {
    var night = new PricingTimeBandImpl<StartWithPerKmPriceRule>("night", List.of(entry(Set.of(), 1260, 480)), null,
        List.of(factorOverride(1, heavyGoods)));
    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(2.76, null), List.of(factorOverride(3, heavyGoods)),
        List.of(night), List.of(), List.of());

    assertEquals(2.76, pricing.getPointPriceRule(heavyGoods, at(DayOfWeek.MONDAY, 23), 0).getStartAmountInCurrency(),
        EPSILON);
    assertEquals(2.76, pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 23), 0).getStartAmountInCurrency(), EPSILON);
  }

  /**
   * A base start of 1.00 and a section at 0.40 per km, a morning band setting that section to 0.55 per km, and a base
   * override for motorcycles setting only the start to 0.50: a motorcycle in the morning band pays a start of 0.50 and
   * 0.55 per km in that section ("Amount override inheriting the band's rate")
   */
  @Test
  public void amountOverrideInheritingTheBandsRate() {
    var tunnel = section();
    var morning = new PricingTimeBandImpl<>("morning", List.of(entry(WEEKDAYS, 420, 120)),
        priceRule(null, null, new SectionPriceRuleImpl(tunnel, null, 0.55)), List.of());
    var motorBikeStart = new PriceOverrideImpl<>(PricedModeSelection.named(List.of(motorBike)), priceRule(0.50, null),
        null);
    var pricing = new StartWithPerKmSchemePricingImpl(
        priceRule(1.00, null, new SectionPriceRuleImpl(tunnel, null, 0.40)), List.of(motorBikeStart),
        List.of(morning), List.of(), List.of());

    var priceRule = pricing.getPointPriceRule(motorBike, at(DayOfWeek.MONDAY, 8), 0);
    assertEquals(0.50, priceRule.getStartAmountInCurrency(), EPSILON);
    assertEquals(0.55, priceRule.findSectionPriceRule(tunnel).getPerKmAmountInCurrency(), EPSILON);
    assertEquals(0.40, pricing.getPointPriceRule(motorBike, at(DayOfWeek.MONDAY, 12), 0).findSectionPriceRule(tunnel)
        .getPerKmAmountInCurrency(), EPSILON);
  }

  /**
   * A band setting the rate outside sections and one section, and leaving out a second: the second keeps its base
   * section price rule during the band ("Section the band does not list")
   */
  @Test
  public void sectionTheBandDoesNotList() {
    var first = section();
    var second = section();
    var band = new PricingTimeBandImpl<>("band", List.of(entry(WEEKDAYS, 420, 120)),
        priceRule(null, 0.30, new SectionPriceRuleImpl(first, null, 0.60)), List.of());
    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(null, 0.20, new SectionPriceRuleImpl(first, null, 0.40),
        new SectionPriceRuleImpl(second, 1.00, 0.50)), List.of(), List.of(band), List.of(), List.of());

    var priceRule = pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 0);
    assertEquals(0.30, priceRule.getPerKmAmountInCurrency(), EPSILON);
    assertEquals(0.60, priceRule.findSectionPriceRule(first).getPerKmAmountInCurrency(), EPSILON);
    assertEquals(0.50, priceRule.findSectionPriceRule(second).getPerKmAmountInCurrency(), EPSILON);
    assertEquals(1.00, priceRule.findSectionPriceRule(second).getStartAmountInCurrency(), EPSILON);
  }

  /**
   * No base price rule and a weekday band from 07:00 to 09:00: nothing is charged at 12:00 on a weekday, or at 08:00 on
   * a Saturday ("No base price rule")
   */
  @Test
  public void noBasePrice() {
    var band = new PricingTimeBandImpl<>("band", List.of(entry(WEEKDAYS, 420, 120)), priceRule(2.00, 0.10), List.of());
    var pricing = new StartWithPerKmSchemePricingImpl(null, List.of(), List.of(band), List.of(), List.of());

    for (var moment : List.of(at(DayOfWeek.MONDAY, 12), at(DayOfWeek.SATURDAY, 8))) {
      var priceRule = pricing.getPointPriceRule(car, moment, 0);
      assertEquals(0, priceRule.getStartAmountInCurrency(), EPSILON);
      assertEquals(0, priceRule.getPerKmAmountInCurrency(), EPSILON);
      assertTrue(priceRule.getSectionPriceRules().isEmpty());
    }
    assertEquals(2.00, pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 0).getStartAmountInCurrency(), EPSILON);
  }

  /**
   * An entry-exit table with south to north at 4.90 and east to north at 3.00, a morning band listing only south to
   * north at 6.10, and a base factor of 3 for heavy goods: a truck pays 18.30 from south to north in the morning band,
   * and 9.00 from east to north ("Entry-exit pair in a band")
   */
  @Test
  public void entryExitPairInABand() {
    var south = gates.getFactory().registerNew(List.of(linkSegment), List.of());
    var north = gates.getFactory().registerNew(List.of(linkSegment), List.of());
    var east = gates.getFactory().registerNew(List.of(linkSegment), List.of());
    var base = new FromToGatesPriceRuleImpl(List.<GateFromToAmount>of(new GateFromToAmountImpl(south, north, 4.90),
        new GateFromToAmountImpl(east, north, 3.00)));
    var morning = new PricingTimeBandImpl<FromToGatesPriceRule>("morning", List.of(entry(WEEKDAYS, 420, 120)),
        new FromToGatesPriceRuleImpl(List.of(new GateFromToAmountImpl(south, north, 6.10))), List.of());
    var pricing = new FromToGatesSchemePricingImpl(base, List.of(factorOverride(3, heavyGoods)), List.of(morning),
        List.of(), List.of());

    var priceRule = pricing.getPointPriceRule(heavyGoods, at(DayOfWeek.MONDAY, 8), 0);
    assertEquals(18.30, priceRule.findByFromToGate(south, north).getAmountInCurrency(), EPSILON);
    assertEquals(9.00, priceRule.findByFromToGate(east, north).getAmountInCurrency(), EPSILON);
  }

  /**
   * A rate of 0.10 per km, a daily band up to 20 km with factor 0 and a daily band from 200 km with factor 0.75: a
   * car's 10th km of the day costs 0, its 100th km 0.10 and its 250th km 0.075 ("Free first kilometres and a cheaper
   * long distance")
   */
  @Test
  public void freeFirstKilometresAndACheaperLongDistance() {
    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(null, 0.10), List.of(), List.of(),
        distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km(), List.of());

    assertEquals(0, pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 9).getPerKmAmountInCurrency(), EPSILON);
    assertEquals(0.10, pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 99).getPerKmAmountInCurrency(), EPSILON);
    assertEquals(0.075, pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 249).getPerKmAmountInCurrency(),
        EPSILON);
  }

  /**
   * A start of 1.00 and 0.30 per km with a bridge section starting at 2.00: a 10 km use crossing the bridge costs
   * 1.00 + 2.00 + 10 x 0.30 = 6.00, the bridge's rate left out being the scheme's ("Trip charge plus a bridge"); with
   * no start and a start per gantry section, nothing is charged per use ("Charge per gantry only")
   */
  @Test
  public void amountsOfATripChargePlusABridgeAndOfAChargePerGantry() {
    var bridge = section();
    var pricing = new StartWithPerKmSchemePricingImpl(
        priceRule(1.00, 0.30, new SectionPriceRuleImpl(bridge, 2.00, null)), List.of(), List.of(), List.of(),
        List.of());
    var priceRule = pricing.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 0);
    var bridgePriceRule = priceRule.findSectionPriceRule(bridge);
    assertEquals(0.30, bridgePriceRule.getPerKmAmountInCurrency(), EPSILON);
    assertEquals(6.00,
        priceRule.getStartAmountInCurrency() + bridgePriceRule.getStartAmountInCurrency()
            + 10 * priceRule.getPerKmAmountInCurrency(), EPSILON);

    var firstGantry = section();
    var secondGantry = section();
    var perGantry = new StartWithPerKmSchemePricingImpl(priceRule(null, null,
        new SectionPriceRuleImpl(firstGantry, 1.20, null), new SectionPriceRuleImpl(secondGantry, 0.90, null)),
        List.of(), List.of(), List.of(), List.of());
    var gantryPriceRule = perGantry.getPointPriceRule(car, at(DayOfWeek.MONDAY, 8), 0);
    assertEquals(0, gantryPriceRule.getStartAmountInCurrency(), EPSILON);
    assertEquals(1.20, gantryPriceRule.findSectionPriceRule(firstGantry).getStartAmountInCurrency(), EPSILON);
    assertSame(secondGantry, gantryPriceRule.getSectionPriceRules().get(1).getSection());
  }

  /**
   * Parts that make no sense on their own are refused when made: an override with both amounts and a factor, or
   * neither, a price rule giving no amount counting as none, or with modes not named; a band without entries, or with
   * neither a price rule nor overrides; a distance band per hour, or starting beyond its end
   */
  @Test
  public void partsThatMakeNoSenseAreRefused() {
    var named = PricedModeSelection.named(List.of(heavyGoods));
    assertThrows(IllegalArgumentException.class,
        () -> new PriceOverrideImpl<>(named, priceRule(1.00, null), 3.0));
    assertThrows(IllegalArgumentException.class, () -> new PriceOverrideImpl<StartWithPerKmPriceRule>(named, null,
        null));
    assertThrows(IllegalArgumentException.class, () -> new PriceOverrideImpl<>(named, priceRule(null, null), null));
    assertThrows(IllegalArgumentException.class, () -> new PriceOverrideImpl<StartWithPerKmPriceRule>(
        PricedModeSelection.exempting(List.of(car)), null, 3.0));
    assertThrows(IllegalArgumentException.class, () -> new PricingTimeBandImpl<>("band", List.of(), priceRule(1.0,
        null),
        List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> new PricingTimeBandImpl<>("band", List.of(entry(WEEKDAYS, 420, 60)), null, List.of()));
    assertThrows(IllegalArgumentException.class, () -> new PricingDistanceBandImpl(PricingPeriod.HOUR, null, 20.0, 0));
    assertThrows(IllegalArgumentException.class, () -> new PricingDistanceBandImpl(PricingPeriod.DAY, 30.0, 20.0, 0));
  }

  /**
   * The first 20 km of a day free and a quarter less from 200 km: the free band holds 10 km and gives factor 0, 1 at
   * 25 km; a use of 30 km after 15 km has 5 km inside it; the pricing gives 0 at 10 km, 1 at 100 km and 0.75 at 250
   * km, and that use's 30 km count as 25
   */
  @Test
  public void distanceBandsGiveTheirFactorAndTheKmInside() {
    var distanceBands = distanceBandsFreeFirst20KmAndAQuarterLessFrom200Km();
    var free = distanceBands.get(0);
    assertTrue(free.contains(10));
    assertFalse(free.contains(25));
    assertEquals(0, free.computeDistanceBandFactor(10), EPSILON);
    assertEquals(1, free.computeDistanceBandFactor(25), EPSILON);
    assertEquals(5, free.findKmInside(15, 30), EPSILON);
    assertEquals(0, free.findKmInside(25, 30), EPSILON);

    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(null, 0.10), List.of(), List.of(), distanceBands,
        List.of());
    assertEquals(0, pricing.computeDistanceBandFactor(10), EPSILON);
    assertEquals(1, pricing.computeDistanceBandFactor(100), EPSILON);
    assertEquals(0.75, pricing.computeDistanceBandFactor(250), EPSILON);
    assertEquals(25, pricing.computeKmTimesDistanceBandFactors(15, 30), EPSILON);
  }

  /**
   * A weekday peak from 07:00 to 09:00 with a factor of 3 for heavy goods, and a base override for motorbikes: the band
   * covers Tuesday 08:00 but not Saturday 08:00 or Tuesday 12:00, and finds the heavy goods override only; the pricing
   * finds the band at Tuesday 08:00, none at noon, and the motorbike override of its base
   */
  @Test
  public void timeBandCoversAMomentAndFindsItsOverride() {
    PriceOverride<StartWithPerKmPriceRule> heavyGoodsOverride = factorOverride(3, heavyGoods);
    var peak = new PricingTimeBandImpl<>("peak", List.of(entry(WEEKDAYS, 420, 120)), priceRule(4.41, null),
        List.of(heavyGoodsOverride));
    assertTrue(peak.covers(at(DayOfWeek.TUESDAY, 8)));
    assertFalse(peak.covers(at(DayOfWeek.SATURDAY, 8)));
    assertFalse(peak.covers(at(DayOfWeek.TUESDAY, 12)));
    assertSame(heavyGoodsOverride, peak.findPriceOverride(heavyGoods));
    assertNull(peak.findPriceOverride(car));

    PriceOverride<StartWithPerKmPriceRule> motorBikeOverride = factorOverride(1, motorBike);
    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(2.76, null), List.of(motorBikeOverride),
        List.of(peak), List.of(), List.of());
    assertSame(peak, pricing.findTimeBand(at(DayOfWeek.TUESDAY, 8)));
    assertNull(pricing.findTimeBand(at(DayOfWeek.TUESDAY, 12)));
    assertSame(motorBikeOverride, pricing.findPriceOverride(motorBike));
    assertNull(pricing.findPriceOverride(heavyGoods));
  }

  /**
   * A rate of 0.64 per km, a tunnel at 0.40 per km with a start of 1.50, and a bridge giving a start of 2.00 only: the
   * tunnel is charged 0.40 per km, the bridge and a section the rule does not list 0.64; the tunnel's start per pass is
   * 1.50, the other section's 0; a pair of gates gives 4.90 one way and nothing the other
   */
  @Test
  public void priceRuleFindsRatesStartsAndGateAmounts() {
    var tunnel = section();
    var bridge = section();
    var other = section();
    var rule = priceRule(null, 0.64, new SectionPriceRuleImpl(tunnel, 1.50, 0.40),
        new SectionPriceRuleImpl(bridge, 2.00, null));
    assertEquals(0.40, rule.findPerKmAmountInCurrency(tunnel), EPSILON);
    assertEquals(0.64, rule.findPerKmAmountInCurrency(bridge), EPSILON);
    assertEquals(0.64, rule.findPerKmAmountInCurrency(other), EPSILON);
    assertEquals(1.50, rule.findSectionStartAmountInCurrency(tunnel), EPSILON);
    assertEquals(2.00, rule.findSectionStartAmountInCurrency(bridge), EPSILON);
    assertEquals(0, rule.findSectionStartAmountInCurrency(other), EPSILON);

    var south = gates.getFactory().registerNew(List.of(linkSegment), List.of());
    var north = gates.getFactory().registerNew(List.of(), List.of(linkSegment));
    var pairs = new FromToGatesPriceRuleImpl(List.of(new GateFromToAmountImpl(south, north, 4.90)));
    assertEquals(4.90, pairs.findAmountInCurrency(south, north), EPSILON);
    assertEquals(0, pairs.findAmountInCurrency(north, south), EPSILON);
  }

  /**
   * Minimums of 2.00 for every mode and 3.00 for heavy goods, and an override for heavy goods: each applies to the
   * modes it covers, and the pricing finds 3.00 for heavy goods, 2.00 for a car
   */
  @Test
  public void minimumsAndOverridesApplyToTheirModes() {
    var everyMode = new ModesMinimumPriceImpl(2.00, PricedModeSelection.unspecified());
    var trucks = new ModesMinimumPriceImpl(3.00, PricedModeSelection.named(List.of(heavyGoods)));
    assertTrue(everyMode.appliesTo(car));
    assertTrue(trucks.appliesTo(heavyGoods));
    assertFalse(trucks.appliesTo(car));
    PriceOverride<StartWithPerKmPriceRule> heavyGoodsOverride = factorOverride(3, heavyGoods);
    assertTrue(heavyGoodsOverride.appliesTo(heavyGoods));
    assertFalse(heavyGoodsOverride.appliesTo(car));

    var pricing = new StartWithPerKmSchemePricingImpl(priceRule(1.00, null), List.of(heavyGoodsOverride), List.of(),
        List.of(), List.of(everyMode, trucks));
    assertEquals(3.00, pricing.findModesMinimumPrice(heavyGoods), EPSILON);
    assertEquals(2.00, pricing.findModesMinimumPrice(car), EPSILON);
  }
}
