package org.goplanit.test.pricing;

import org.goplanit.pricing.PricingDistanceBandImpl;
import org.goplanit.pricing.GateFromToAmountImpl;
import org.goplanit.pricing.ModesMinimumPriceImpl;
import org.goplanit.pricing.StartWithPerKmPriceRuleImpl;
import org.goplanit.pricing.PriceOverrideImpl;
import org.goplanit.pricing.PricingSchemeGateImpl;
import org.goplanit.pricing.StartWithPerKmSchemePricingImpl;
import org.goplanit.pricing.SectionPriceRuleImpl;
import org.goplanit.pricing.PricingTimeBandImpl;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.goplanit.pricing.PriceCapUtils;

import static org.goplanit.test.pricing.PricingTestUtils.WEEKDAYS;
import static org.goplanit.test.pricing.PricingTestUtils.bandsOf;
import static org.goplanit.test.pricing.PricingTestUtils.entry;
import static org.goplanit.test.pricing.PricingTestUtils.baseOf;
import static org.goplanit.test.pricing.PricingTestUtils.gatePairsOf;
import static org.goplanit.test.pricing.PricingTestUtils.startOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of a scheme's own rules, applied when it is registered: its priced link segments, the modes it charges, its
 * sections and gates, and its pricing's overrides and bands. Problems are asked by kind, never by their text
 *
 * @author markr
 */
public class SchemeTest extends NetworkPricingTestBase {

  /**
   * Register an entry-exit scheme on both toll roads, for the default modes
   *
   * @param gates of the scheme
   * @param schemePricing of the scheme
   * @return the scheme, null when refused, and the problems found
   */
  private Pair<EntryExitPricingScheme, Problems<PricingProblemKind>> entryExitScheme(
      List<PricingSchemeGate> gates,
      SchemePricing<FromToGatesPriceRule> schemePricing) {
    return pricing.getEntryExitSchemes().getFactory().registerNew("motorway", PricedModeSelection.unspecified(),
        List.of(m4, m8), gates, schemePricing);
  }

  /**
   * A location scheme on m4 with two sections on its link segments charges on exactly that road's link segments, and
   * its section price rules refer to its two sections ("Scheme on a toll road")
   */
  @Test
  public void schemeOnATollRoad() {
    var first = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1)), List.of());
    var second = pricing.getSections().getFactory().registerNew(List.of(segment(1, 2)), List.of());
    var schemePricing = baseOf(new StartWithPerKmPriceRuleImpl(1.0, null, List.of(new SectionPriceRuleImpl(first, 0.5,
        null),
        new SectionPriceRuleImpl(second, 0.7, null))));

    var registered = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(first, second), schemePricing);

    var scheme = registered.first();
    assertNotNull(scheme);
    assertTrue(registered.second().isEmpty());
    assertEquals(Set.of(segment(0, 1), segment(1, 2)), scheme.streamPricedLinkSegments().collect(Collectors.toSet()));
    assertFalse(scheme.isPricedLinkSegment(segment(2, 3)));
    assertSame(second, scheme.findSection(segment(1, 2)));
    assertSame(first, scheme.getPricing().getBasePriceRule().getSectionPriceRules().get(0).getSection());
    assertSame(scheme, pricing.getLocationSchemes().get(scheme.getId()));
  }

  /**
   * A location scheme on m4 and m8, where m4 leads straight onto m8, charges on the link segments of both, and the link
   * segments where m4 leads onto m8 are neither where the scheme is left nor where it is entered ("Roads priced as
   * one")
   */
  @Test
  public void roadsPricedAsOne() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4 and m8", PricedModeSelection.unspecified(),
        List.of(m4, m8), List.of(), startOf(1.0)).first();

    assertEquals(4, scheme.streamPricedLinkSegments().count());
    var entered = scheme.findPricedLinkSegmentsEnteringFromOutside();
    var left = scheme.findPricedLinkSegmentsLeavingToOutside();
    assertFalse(left.contains(segment(1, 2)));
    assertFalse(entered.contains(segment(2, 3)));
    assertEquals(Set.of(segment(0, 1), segment(1, 2)), Set.copyOf(entered));
    assertEquals(Set.of(segment(0, 1), segment(3, 4)), Set.copyOf(left));
  }

  /**
   * A location scheme on a toll road priced for the default modes and one for heavy goods only is refused ("Toll roads
   * pricing different modes")
   */
  @Test
  public void tollRoadsPricingDifferentModes() {
    var registered = pricing.getLocationSchemes().getFactory().registerNew("mixed", PricedModeSelection.unspecified(),
        List.of(m8, truckOnlyTollRoad()), List.of(), startOf(1.0));
    assertNull(registered.first());
    assertTrue(registered.second().has(PricingProblemKind.TOLL_ROADS_PRICE_DIFFERENT_MODES));
    assertTrue(pricing.getLocationSchemes().isEmpty());
  }

  /**
   * A section of a scheme on m4 given 2-3, which is not on m4, loses that link segment, reported, and the scheme is
   * added ("Section off the priced link segments")
   */
  @Test
  public void sectionOffThePricedLinkSegments() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1), segment(2, 3)), List.of());
    var registered = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), startOf(1.0));

    assertNotNull(registered.first());
    assertTrue(registered.second().has(PricingProblemKind.SECTION_LINK_SEGMENT_NOT_PRICED));
    assertEquals(List.of(segment(0, 1)), section.getLinkSegments());
    assertTrue(pricing.getSections().getByLinkSegment(segment(2, 3)).isEmpty());
  }

  /**
   * Two sections of a scheme sharing a link segment refuse it, and the pricing keeps neither section, as each belongs
   * to its scheme
   */
  @Test
  public void sectionsSharingALinkSegmentAreRefused() {
    var first = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1)), List.of());
    var second = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1), segment(1, 2)), List.of());
    var registered = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(first, second), startOf(1.0));
    assertNull(registered.first());
    assertTrue(registered.second().has(PricingProblemKind.SECTIONS_SHARE_LINK_SEGMENT));
    assertTrue(pricing.getSections().isEmpty());
  }

  /**
   * A gate given 0-1 and 1-2 as entries, each entered from outside the motorway, and 0-1 as exit, left by the off-ramp,
   * holds all three ("Interchange with several ramps")
   */
  @Test
  public void interchangeWithSeveralRamps() {
    var interchange = pricing.getGates().getFactory().registerNew(List.of(segment(0, 1), segment(1, 2)),
        List.of(segment(0, 1)));
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var registered = entryExitScheme(List.of(interchange, north),
        gatePairsOf(new GateFromToAmountImpl(interchange, north, 3.0)));

    assertNotNull(registered.first());
    assertFalse(registered.second().has(PricingProblemKind.GATE_NOT_AT_EDGE));
    assertEquals(List.of(segment(0, 1), segment(1, 2)), interchange.getEntryLinkSegments());
    assertEquals(List.of(segment(0, 1)), interchange.getExitLinkSegments());
  }

  /**
   * A gate given 2-3 as an entry, reached only from the motorway itself, loses it, reported ("Entry in the middle of
   * the road"); a scheme without gates is refused
   */
  @Test
  public void entryInTheMiddleOfTheRoad() {
    var middle = pricing.getGates().getFactory().registerNew(List.of(segment(0, 1), segment(2, 3)), List.of());
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var registered = entryExitScheme(List.of(middle, north), gatePairsOf(new GateFromToAmountImpl(middle, north,
        3.0)));

    assertTrue(registered.second().has(PricingProblemKind.GATE_NOT_AT_EDGE));
    assertEquals(List.of(segment(0, 1)), middle.getEntryLinkSegments());

    var withoutGates = entryExitScheme(List.of(), gatePairsOf());
    assertNull(withoutGates.first());
    assertTrue(withoutGates.second().has(PricingProblemKind.NO_GATES));
  }

  /**
   * An entry and another gate's exit connected along the motorway that no price rule lists are reported once, and not
   * once listed
   */
  @Test
  public void pairWithoutPriceIsReported() {
    var south = pricing.getGates().getFactory().registerNew(List.of(segment(0, 1)), List.of());
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var unlisted = entryExitScheme(List.of(south, north), gatePairsOf());
    assertTrue(unlisted.second().has(PricingProblemKind.PAIR_WITHOUT_PRICE));
    assertEquals(1, unlisted.second().stream().filter(problem ->
        problem.getKind() == PricingProblemKind.PAIR_WITHOUT_PRICE).count());
  }

  /**
   * A mode scheme including nothing covers every link segment of the network, less those it excludes ("Mode scheme over
   * the whole network")
   */
  @Test
  public void modeSchemeOverTheWholeNetwork() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.excludeLinkSegments(List.of(segment(9, 0)));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("trucks", PricedModeSelection.named(
        List.of(heavyGoods)), coverage, List.of(), startOf(1.0)).first();

    assertEquals(layer.getLinkSegments().size() - 1, scheme.streamPricedLinkSegments().count());
    assertFalse(scheme.isPricedLinkSegment(segment(9, 0)));
    assertTrue(scheme.isPricedLinkSegment(segment(1, 6)));
  }

  /**
   * A mode scheme on a network of two layers including one layer and no types or link segments covers every link
   * segment of that layer and none of the other ("Whole layer")
   */
  @Test
  public void wholeLayer() {
    var other = network.getTransportLayers().getFactory().registerNew();
    var link = other.getLinks().getFactory().registerNew(
        other.getNodes().getFactory().registerNew(), other.getNodes().getFactory().registerNew(), 1, true);
    var otherSegment = other.getLinkSegments().getFactory().registerNew(link, true, true);
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.considerLayers(List.of(other));

    var scheme = pricing.getModeSchemes().getFactory().registerNew("trucks", PricedModeSelection.named(
        List.of(heavyGoods)), coverage, List.of(), startOf(1.0)).first();

    assertEquals(List.of(otherSegment), scheme.streamPricedLinkSegments().collect(Collectors.toList()));
    assertFalse(scheme.isPricedLinkSegment(segment(0, 1)));
  }

  /**
   * A mode scheme including the motorway, trunk and primary types with a section given by the primary type finds that
   * section for every primary link segment, and none for a motorway link segment ("Section given by type")
   */
  @Test
  public void sectionGivenByType() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegmentTypes(List.of(motorway, trunk, primary));
    var section = pricing.getSections().getFactory().registerNew(List.of(), List.of(primary));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("trucks", PricedModeSelection.named(
        List.of(heavyGoods)), coverage, List.of(section), startOf(1.0)).first();

    assertSame(section, scheme.findSection(segment(5, 1)));
    assertSame(section, scheme.findSection(segment(1, 6)));
    assertNull(scheme.findSection(segment(0, 1)));
  }

  /**
   * A location scheme on the m4, whose link segments are motorway, with a section given by the motorway type finds
   * that section for each link segment of the toll road, and none off the scheme ("Section given by type on a toll
   * road")
   */
  @Test
  public void sectionGivenByTypeOnATollRoad() {
    var section = pricing.getSections().getFactory().registerNew(List.of(), List.of(motorway));
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), startOf(1.0)).first();

    assertSame(section, scheme.findSection(segment(0, 1)));
    assertSame(section, scheme.findSection(segment(1, 2)));
    assertNull(scheme.findSection(segment(2, 3)));
  }

  /**
   * A location scheme naming car and heavy goods on a toll road only heavy goods pay on charges heavy goods only, and
   * dropping car is reported; an exempt mode the road does not price is reported too ("Car named on a truck-only toll
   * road")
   */
  @Test
  public void carNamedOnATruckOnlyTollRoad() {
    var truckOnly = truckOnlyTollRoad();
    var registered = pricing.getLocationSchemes().getFactory().registerNew("trucks",
        PricedModeSelection.named(List.of(car, heavyGoods)), List.of(truckOnly), List.of(), startOf(1.0));

    assertEquals(Set.of(heavyGoods), registered.first().getChargedModes());
    assertTrue(registered.second().has(PricingProblemKind.MODE_NOT_PRICED_BY_TOLL_ROADS));

    var exempting = pricing.getLocationSchemes().getFactory().registerNew("m4",
        PricedModeSelection.exempting(List.of(bus)), List.of(m4), List.of(), startOf(1.0));
    assertEquals(Set.of(car, heavyGoods), exempting.first().getChargedModes());
    assertTrue(exempting.second().has(PricingProblemKind.EXEMPT_MODE_NOT_PRICED_BY_TOLL_ROADS));
  }

  /**
   * A mode scheme naming no modes, on a network with car, heavy goods, bus, bicycle and an ambulance, charges car and
   * heavy goods; a scheme on toll roads naming none charges what they price ("Default modes")
   */
  @Test
  public void defaultModes() {
    var scheme = pricing.getModeSchemes().getFactory().registerNew("everyone", PricedModeSelection.unspecified(),
        new ModePricingSchemeLinkSegmentCoverage(), List.of(), startOf(1.0)).first();
    assertEquals(Set.of(car, heavyGoods), scheme.getChargedModes());

    var onTollRoad = pricing.getLocationSchemes().getFactory().registerNew("trucks", PricedModeSelection.unspecified(),
        List.of(truckOnlyTollRoad()), List.of(), startOf(1.0)).first();
    assertEquals(Set.of(heavyGoods), onTollRoad.getChargedModes());
  }

  /**
   * An override for car on a scheme charging heavy goods only loses car, and being left without modes is dropped, each
   * reported, while the scheme is added
   */
  @Test
  public void overrideForAModeNotChargedIsDropped() {
    var carOverride = new PriceOverrideImpl<StartWithPerKmPriceRule>(PricedModeSelection.named(List.of(car)), null,
        2.0);
    var schemePricing = new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(1.0, null, List.of()),
        List.of(carOverride), List.of(), List.of(), List.of());
    var registered = pricing.getLocationSchemes().getFactory().registerNew("trucks", PricedModeSelection.unspecified(),
        List.of(truckOnlyTollRoad()), List.of(), schemePricing);

    assertNotNull(registered.first());
    assertTrue(registered.second().has(PricingProblemKind.MODE_NOT_CHARGED_BY_SCHEME));
    assertTrue(registered.second().has(PricingProblemKind.LEFT_WITHOUT_MODES));
    assertTrue(registered.first().getPricing().getPriceOverrides().isEmpty());
  }

  /**
   * A time band with two overrides both naming heavy goods refuses its scheme, the problem naming the band ("Mode in
   * two overrides of one band")
   */
  @Test
  public void modeInTwoOverridesOfOneBand() {
    var first = new PriceOverrideImpl<StartWithPerKmPriceRule>(PricedModeSelection.named(List.of(heavyGoods)), null,
        2.0);
    var second = new PriceOverrideImpl<StartWithPerKmPriceRule>(PricedModeSelection.named(List.of(car, heavyGoods)),
        null,
        3.0);
    var peak = new PricingTimeBandImpl<>("peak", List.of(entry(WEEKDAYS, 420, 180)), null, List.of(first, second));
    var registered = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), bandsOf(peak));

    assertNull(registered.first());
    assertTrue(registered.second().stream().anyMatch(problem ->
        problem.getKind() == PricingProblemKind.MODE_IN_TWO_OVERRIDES && problem.getSubject().contains("peak")));
  }

  /**
   * Bands with entries for weekdays 07:00 to 10:00 and 09:00 to 18:00 refuse their scheme, the problem naming both
   * ("Overlapping bands"); with public holidays excluded on one and only on the other the scheme is added ("Holiday
   * entries apart")
   */
  @Test
  public void overlappingBandsAndHolidayEntriesApart() {
    var morning = new PricingTimeBandImpl<StartWithPerKmPriceRule>("morning", List.of(entry(WEEKDAYS, 420, 180)),
        new StartWithPerKmPriceRuleImpl(2.0, null, List.of()), List.of());
    var day = new PricingTimeBandImpl<StartWithPerKmPriceRule>("day", List.of(entry(WEEKDAYS, 540, 540)),
        new StartWithPerKmPriceRuleImpl(1.0, null, List.of()), List.of());
    var overlapping = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), bandsOf(morning, day));
    assertNull(overlapping.first());
    assertTrue(overlapping.second().stream().anyMatch(problem ->
        problem.getKind() == PricingProblemKind.TIME_BANDS_OVERLAP && problem.getDetail().contains("morning") &&
            problem.getDetail().contains("day")));

    var workingDays = new PricingTimeBandImpl<StartWithPerKmPriceRule>("working",
        List.of(entry(WEEKDAYS, HolidayInclusion.EXCLUDED, 420, 180)),
        new StartWithPerKmPriceRuleImpl(2.0, null, List.of()), List.of());
    var holidays = new PricingTimeBandImpl<StartWithPerKmPriceRule>("holiday",
        List.of(entry(Set.of(), HolidayInclusion.ONLY, 420, 180)), new StartWithPerKmPriceRuleImpl(1.0, null,
            List.of()),
        List.of());
    var apart = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), bandsOf(workingDays, holidays));
    assertNotNull(apart.first());
  }

  /** Distance bands that overlap, or count over different periods, refuse their scheme */
  @Test
  public void distanceBandsOverlappingOrMixingPeriodsAreRefused() {
    var overlap = new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(null, 0.1, List.of()), List.of(),
        List.of(),
        List.of(new PricingDistanceBandImpl(PricingPeriod.DAY, null, 30.0, 0),
        new PricingDistanceBandImpl(PricingPeriod.DAY, 20.0, null, 0.5)),
        List.of());
    var mixed = new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(null, 0.1, List.of()), List.of(),
        List.of(),
        List.of(new PricingDistanceBandImpl(PricingPeriod.DAY, null, 20.0, 0),
        new PricingDistanceBandImpl(PricingPeriod.WEEK, 20.0, null, 0.5)),
        List.of());
    var factory = pricing.getLocationSchemes().getFactory();
    assertTrue(factory.registerNew("m4", PricedModeSelection.unspecified(), List.of(m4), List.of(), overlap).second()
        .has(PricingProblemKind.DISTANCE_BANDS_OVERLAP));
    assertTrue(factory.registerNew("m4", PricedModeSelection.unspecified(), List.of(m4), List.of(), mixed).second()
        .has(PricingProblemKind.DISTANCE_BANDS_MIX_PERIODS));
    assertTrue(pricing.getLocationSchemes().isEmpty());
  }

  /**
   * A scheme with a minimum of 2.00 for cars finds that minimum above a cap of 1.50 per use for cars, a problem
   * refusing the cap ("Cap below a minimum"); a cap of 2.00 a day for every mode has none
   */
  @Test
  public void capBelowAMinimum() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(),
        new StartWithPerKmSchemePricingImpl(new StartWithPerKmPriceRuleImpl(1.0, null, List.of()), List.of(),
            List.of(), List.of(), List.of(new ModesMinimumPriceImpl(2.00, PricedModeSelection.named(List.of(car))))))
        .first();
    assertTrue(PriceCapUtils.findMinimumsAboveAmount(List.of(scheme), 1.50,
        PricedModeSelection.named(List.of(car))).has(PricingProblemKind.CAP_BELOW_MINIMUM));
    assertTrue(PriceCapUtils.findMinimumsAboveAmount(List.of(scheme), 2.00, PricedModeSelection.unspecified())
        .isEmpty());
  }

  /**
   * A location scheme on m4 does not price 2-3, which lies on m8: it is tolled, but by another road than the scheme's
   * own, whether or not the layer is given
   */
  @Test
  public void linkSegmentOnAnotherSchemesTollRoadIsNotPriced() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.0)).first();

    assertTrue(m8.containsLinkSegment(segment(2, 3)));
    assertFalse(scheme.isPricedLinkSegment(segment(2, 3)));
    assertFalse(scheme.isPricedLinkSegment(layer, segment(2, 3)));
    assertTrue(scheme.isPricedLinkSegment(layer, segment(0, 1)));
  }

  /**
   * For every link segment of the layer, a location scheme on m4 and a mode scheme on motorways each answer whether
   * they charge on it the same with the layer given as without
   */
  @Test
  public void overloadWithALayerGivesWhatTheOneWithoutGives() {
    var onTollRoad = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.0)).first();
    var motorways = new ModePricingSchemeLinkSegmentCoverage();
    motorways.includeLinkSegmentTypes(List.of(motorway));
    var byType = pricing.getModeSchemes().getFactory().registerNew("trucks", PricedModeSelection.named(
        List.of(heavyGoods)), motorways, List.of(), startOf(1.0)).first();

    for (PricingScheme scheme : List.of(onTollRoad, byType)) {
      for (var linkSegment : layer.getLinkSegments()) {
        assertEquals(scheme.isPricedLinkSegment(linkSegment), scheme.isPricedLinkSegment(layer, linkSegment),
            linkSegment.getIdsAsString());
      }
    }
    assertTrue(byType.isPricedLinkSegment(layer, segment(2, 3)));
    assertFalse(byType.isPricedLinkSegment(layer, segment(4, 8)));
  }

  /**
   * A section listing 1-2 and one of motorways: the first contains 1-2 only, the second every motorway link segment but
   * not the trunk road 4-8; a section with nothing in it, and a gate once its only link segment is taken off, say so; a
   * scheme on m4 charges cars but not bicycles
   */
  @Test
  public void sectionsAndGatesTellWhatTheyHoldAndSchemesTheirModes() {
    var listed = pricing.getSections().getFactory().registerNew(List.of(segment(1, 2)), List.of());
    var byType = pricing.getSections().getFactory().registerNew(List.of(), List.of(motorway));
    var empty = pricing.getSections().getFactory().registerNew(List.of(), List.of());
    assertTrue(listed.contains(segment(1, 2)));
    assertFalse(listed.contains(segment(0, 1)));
    assertTrue(byType.contains(segment(0, 1)));
    assertFalse(byType.contains(segment(4, 8)));
    assertTrue(listed.hasLinkSegmentsOrLinkSegmentTypes());
    assertTrue(byType.hasLinkSegmentsOrLinkSegmentTypes());
    assertFalse(empty.hasLinkSegmentsOrLinkSegmentTypes());

    var gate = pricing.getGates().getFactory().registerNew(List.of(segment(2, 3)), List.of());
    assertTrue(gate.hasEntryOrExitLinkSegments());
    pricing.getGates().update(gate, held -> ((PricingSchemeGateImpl) held).removeEntriesAndExitsMatching(
        linkSegment -> true, linkSegment -> true));
    assertFalse(gate.hasEntryOrExitLinkSegments());

    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.00)).first();
    assertTrue(scheme.chargesMode(car));
    assertFalse(scheme.chargesMode(bicycle));
  }

  /**
   * A gate entered at 2-3 and left at 3-4, and one left at 2-3: looking up gates by entry 2-3 finds the first only, by
   * exit 2-3 the second only, by exit 3-4 the first, by entry 3-4 none, and the lookup by link segment both at 2-3
   */
  @Test
  public void gatesFoundByEntryAndByExit() {
    var gates = pricing.getGates();
    var through = gates.getFactory().registerNew(List.of(segment(2, 3)), List.of(segment(3, 4)));
    var leaving = gates.getFactory().registerNew(List.of(), List.of(segment(2, 3)));
    assertEquals(List.of(through), gates.getByEntryLinkSegment(segment(2, 3)));
    assertEquals(List.of(leaving), gates.getByExitLinkSegment(segment(2, 3)));
    assertEquals(List.of(through), gates.getByExitLinkSegment(segment(3, 4)));
    assertTrue(gates.getByEntryLinkSegment(segment(3, 4)).isEmpty());
    assertEquals(2, gates.getByLinkSegment(segment(2, 3)).size());
  }

  /**
   * A location scheme on m4 and a mode scheme over the network, both leaving their modes to the defaults, registered
   * before a motorbike joins the network and its layer: both charge it from then on, as their charged modes are worked
   * out from their selection when asked, not kept from when they were registered
   */
  @Test
  public void chargedModesFollowAModeAddedToTheLayer() {
    var onM4 = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.00)).first();
    var overTheNetwork = pricing.getModeSchemes().getFactory().registerNew("all", PricedModeSelection.unspecified(),
        new ModePricingSchemeLinkSegmentCoverage(), List.of(), startOf(1.00)).first();
    var motorBike = network.getModes().getFactory().registerNew(PredefinedModeType.MOTOR_BIKE);
    assertFalse(onM4.chargesMode(motorBike));
    assertFalse(overTheNetwork.chargesMode(motorBike));

    layer.registerSupportedModes(List.of(motorBike));
    assertTrue(onM4.chargesMode(motorBike));
    assertTrue(overTheNetwork.chargesMode(motorBike));
  }
}
