package org.goplanit.test.pricing;

import org.goplanit.pricing.StartWithPerKmPriceRuleImpl;
import org.goplanit.pricing.SectionPriceRuleImpl;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.pricing.ModePricingScheme;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingProblemKind;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.problem.Problems;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Currency;
import java.util.List;
import org.goplanit.pricing.PriceCapUtils;

import static org.goplanit.test.pricing.PricingTestUtils.baseOf;
import static org.goplanit.test.pricing.PricingTestUtils.gatePairsOf;
import static org.goplanit.test.pricing.PricingTestUtils.startOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the network pricing component: ids across the kinds of scheme, the rules across schemes, caps, and copying
 *
 * @author markr
 */
public class NetworkPricingTest extends NetworkPricingTestBase {

  /**
   * A mode scheme charging heavy goods on every motorway
   *
   * @return the scheme, null when refused, and the problems found
   */
  private Pair<ModePricingScheme, Problems<PricingProblemKind>> truckChargeOnMotorways() {
    var motorways = new ModePricingSchemeLinkSegmentCoverage();
    motorways.includeLinkSegmentTypes(List.of(motorway));
    return pricing.getModeSchemes().getFactory().registerNew("truck charge",
        PricedModeSelection.named(List.of(heavyGoods)), motorways, List.of(), startOf(0.5));
  }

  /**
   * A network without a network pricing keeps its toll roads as they are, and a network pricing made on it holds
   * nothing until schemes are added ("Network without pricing")
   */
  @Test
  public void networkWithoutPricing() {
    assertEquals(2, layer.getTollRoads().size());
    assertSame(network, pricing.getParentNetwork());
    assertEquals(0, pricing.streamSchemes().count());
    assertTrue(pricing.getSections().isEmpty());
    assertTrue(pricing.getCaps().isEmpty());
  }

  /**
   * A mode scheme, a location scheme and an entry-exit scheme each have an id no other scheme of any kind has, and an
   * id within their own kind; the currency and year are kept ("Ids across kinds")
   */
  @Test
  public void idsAcrossKinds() {
    pricing.setCurrency(Currency.getInstance("AUD"));
    pricing.setYear(Year.of(2025));
    var modeScheme = truckChargeOnMotorways().first();
    var locationScheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.0)).first();
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var entryExitScheme = pricing.getEntryExitSchemes().getFactory().registerNew("m8",
        PricedModeSelection.unspecified(), List.of(m8), List.of(north), gatePairsOf()).first();

    assertEquals(List.of(0L, 1L, 2L), List.of(modeScheme.getId(), locationScheme.getId(), entryExitScheme.getId()));
    assertEquals(0, modeScheme.getModePricingSchemeId());
    assertEquals(0, locationScheme.getLocationSchemeId());
    assertEquals(0, entryExitScheme.getEntryExitSchemeId());
    assertEquals(3, pricing.streamSchemes().count());
    assertEquals("AUD", pricing.getCurrency().getCurrencyCode());
    assertEquals(2025, pricing.getYear().getValue());
  }

  /** Removing a scheme removes its sections and gates from the pricing, as each belongs to its scheme */
  @Test
  public void removingASchemeRemovesItsSectionsAndGates() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1)), List.of());
    var locationScheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), startOf(1.0)).first();
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(3, 4)));
    var entryExitScheme = pricing.getEntryExitSchemes().getFactory().registerNew("m8",
        PricedModeSelection.unspecified(), List.of(m8), List.of(north), gatePairsOf()).first();

    pricing.getModifier().removePricingScheme(locationScheme);
    pricing.getModifier().removePricingScheme(entryExitScheme);

    assertTrue(pricing.getSections().isEmpty());
    assertTrue(pricing.getGates().isEmpty());
  }

  /**
   * A second mode scheme charging heavy goods is refused, while the first stays ("Second mode scheme for a mode")
   */
  @Test
  public void secondModeSchemeForAMode() {
    var first = truckChargeOnMotorways().first();
    var second = pricing.getModeSchemes().getFactory().registerNew("trucks everywhere",
        PricedModeSelection.named(List.of(heavyGoods)), new ModePricingSchemeLinkSegmentCoverage(), List.of(),
            startOf(1.0));

    assertNull(second.first());
    assertTrue(second.second().has(PricingProblemKind.SECOND_MODE_SCHEME_FOR_MODE));
    assertSame(first, pricing.getModeSchemes().getByPricedMode(heavyGoods));
    assertNull(pricing.getModeSchemes().getByPricedMode(car));
  }

  /**
   * A mode scheme charging heavy goods on every motorway and a location scheme on m4 both charge a truck on m4, as the
   * charges of all schemes a trip uses add up, with no problem across them ("Truck charge and toll road")
   */
  @Test
  public void truckChargeAndTollRoad() {
    var truckCharge = truckChargeOnMotorways().first();
    var tollRoad = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(1.0));

    assertTrue(tollRoad.second().isEmpty());
    for (PricingScheme scheme : List.of(truckCharge, tollRoad.first())) {
      assertTrue(scheme.isPricedLinkSegment(segment(0, 1)));
      assertTrue(scheme.getChargedModes().contains(heavyGoods));
    }
  }

  /**
   * Two location schemes both charging car on m4 are reported once, naming the other, and both are kept ("Same road in
   * two location schemes")
   */
  @Test
  public void sameRoadInTwoLocationSchemes() {
    var factory = pricing.getLocationSchemes().getFactory();
    var first = factory.registerNew("m4", PricedModeSelection.unspecified(), List.of(m4), List.of(), startOf(1.0));
    var second = factory.registerNew("m4 again", PricedModeSelection.unspecified(), List.of(m4), List.of(),
        startOf(2.0));

    assertTrue(first.second().isEmpty());
    assertEquals(1, second.second().stream().filter(problem ->
        problem.getKind() == PricingProblemKind.SCHEMES_SHARE_LINK_SEGMENT).count());
    assertNotNull(second.first());
    assertEquals(2, pricing.getLocationSchemes().size());
  }

  /**
   * A cap covering no scheme, or giving a count per use, is refused when made ("Cap covering no scheme", "Count cap per
   * use"); one giving neither an amount nor a count per day allows one charged use per day, and is found from its
   * scheme ("Once a day")
   */
  @Test
  public void capsRefusedOrOnceADay() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("cordon", PricedModeSelection.unspecified(),
        List.of(m4), List.of(), startOf(5.0)).first();
    var caps = pricing.getCaps().getFactory();
    assertThrows(IllegalArgumentException.class, () -> caps.registerNew(List.of(), 10.0, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified()));
    assertThrows(IllegalArgumentException.class, () -> caps.registerNew(List.of(scheme), null, 2,
        PricingPeriod.USE, PricedModeSelection.unspecified()));

    var onceADay = caps.registerNew(List.of(scheme), null, null, PricingPeriod.DAY, PricedModeSelection.unspecified());
    assertEquals(1, onceADay.getActivateAboveCount());
    assertNull(onceADay.getAmountInCurrency());
    assertEquals(List.of(onceADay), pricing.getCaps().getByScheme(scheme));
  }

  /** A cap naming only a mode its scheme does not charge is left without modes, reported */
  @Test
  public void capForAModeNotChargedIsLeftWithoutModes() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("trucks", PricedModeSelection.unspecified(),
        List.of(truckOnlyTollRoad()), List.of(), startOf(5.0)).first();
    var problems = new Problems<PricingProblemKind>();
    assertNull(PriceCapUtils.limitToChargedModes(List.of(scheme), PricedModeSelection.named(List.of(car)), problems));
    assertTrue(problems.has(PricingProblemKind.LEFT_WITHOUT_MODES));
  }

  /**
   * A deep copy keeps referring to the same network and toll roads, and holds copies of the schemes, sections and caps
   * that refer to one another: its scheme's section price rule refers to the copy's section, its cap to the copy's
   * scheme
   */
  @Test
  public void deepCopyKeepsReferringToTheSameNetwork() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(0, 1)), List.of());
    var schemePricing =
        baseOf(new StartWithPerKmPriceRuleImpl(1.0, null, List.of(new SectionPriceRuleImpl(section, 0.5, null))));
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("m4", PricedModeSelection.unspecified(),
        List.of(m4), List.of(section), schemePricing).first();
    var cap = pricing.getCaps().getFactory().registerNew(List.of(scheme), 10.0, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());

    var copy = pricing.deepClone();

    assertSame(network, copy.getParentNetwork());
    var copiedScheme = copy.getLocationSchemes().get(scheme.getId());
    var copiedSection = copy.getSections().get(section.getId());
    assertNotSame(scheme, copiedScheme);
    assertNotSame(section, copiedSection);
    assertSame(m4, copiedScheme.getTollRoads().get(0));
    assertSame(copiedSection, copiedScheme.getSections().get(0));
    assertSame(copiedSection, copiedScheme.getPricing().getBasePriceRule().getSectionPriceRules().get(0).getSection());
    assertSame(copiedScheme, copy.getCaps().get(cap.getId()).getSchemes().get(0));
    assertEquals(List.of(copy.getCaps().get(cap.getId())), copy.getCaps().getByScheme(copiedScheme));
    assertSame(section, scheme.getPricing().getBasePriceRule().getSectionPriceRules().get(0).getSection());
    assertFalse(copy.getCaps().getByScheme(scheme).contains(cap));
  }
}
