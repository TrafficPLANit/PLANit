package org.goplanit.test.pricing;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.pricing.*;
import org.goplanit.pricing.GateFromToAmountImpl;
import org.goplanit.utils.pricing.modifier.event.RemovePricingSchemeEvent;
import org.goplanit.pricing.modifier.event.handler.UpdatePricingOnNetworkChangeHandler;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModificationEvent;
import org.goplanit.utils.pricing.modifier.event.NetworkPricingModifierListener;
import org.goplanit.utils.test.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.goplanit.test.pricing.PricingTestUtils.baseOf;
import static org.goplanit.test.pricing.PricingTestUtils.gatePairsOf;
import static org.goplanit.test.pricing.PricingTestUtils.startOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a network pricing follows the changes the layer modifier makes to its network: breaks and removals of
 * link segments, link segment types removed, and toll roads removed
 * <pre>
 * 5 --> 0 ==road==> 2 --road--> 3 --other--> 4
 *            (1)
 * </pre>
 * The toll road "road" holds 0-2, a link that can be broken at node 1, and 2-3; the toll road "other" holds 3-4. Both
 * are priced for car, the only mode, which every link allows
 *
 * @author markr
 */
public class PricingFollowsNetworkChangesTest extends LayerTestBase {

  /** a listener for scheme removals, from outside */
  private static class RemovalListener implements NetworkPricingModifierListener {
    private final List<NetworkPricingModificationEvent> heard = new ArrayList<>();
    @Override public void onNetworkPricingModifierEvent(NetworkPricingModificationEvent event) { heard.add(event); }
    @Override
    public EventType[] getKnownSupportedEventTypes() {
      return new EventType[] {RemovePricingSchemeEvent.EVENT_TYPE};
    }
  }

  private NetworkPricing pricing;
  private MacroscopicLink breakable;
  private TollRoad road;
  private TollRoad other;

  /** break the link 0-2 at node 1 through the layer modifier */
  private void breakAtNodeOne() {
    layer.getLayerModifier().breakAt(List.of(breakable), nodes.get(1), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedMode(car);
    createNodesOnLine(6);
    breakable = breakableOneWay(0, 2);
    oneWay(2, 3);
    oneWay(3, 4);
    oneWay(5, 0);
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    layer.getLinkSegments().forEach(linkSegment -> linkSegment.setLinkSegmentType(road));
    var tollRoads = layer.getTollRoads().getFactory();
    this.road = tollRoads.registerNew(List.of(segment(0, 2), segment(2, 3)), PricedModeSelection.unspecified());
    other = tollRoads.registerNew(List.of(segment(3, 4)), PricedModeSelection.unspecified());
    pricing = new NetworkPricing(IdGroupingToken.collectGlobalToken(), network);
  }

  /**
   * Breaking the link of a cordon's crossing leaves both new link segments priced, on its one toll road, so crossing it
   * is one use paying the start once ("Break a link in a cordon")
   */
  @Test
  public void breakALinkInACordon() {
    var cordon = pricing.getLocationSchemes().getFactory().registerNew("cordon", PricedModeSelection.unspecified(),
        List.of(road), List.of(), startOf(5.0)).first();

    breakAtNodeOne();

    assertTrue(cordon.isPricedLinkSegment(segment(0, 1)));
    assertTrue(cordon.isPricedLinkSegment(segment(1, 2)));
    assertEquals(3, road.getLinkSegments().size());
  }

  /**
   * Breaking a link of a section leaves both new link segments in it, so the scheme charges on both in that section
   * ("Break a link on a toll road"), and the section keeps one price rule, so a pass pays its start of 3.00 once
   * ("Breaking a link does not charge twice")
   */
  @Test
  public void breakALinkOnATollRoadKeepsBothPartsInTheSection() {
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(0, 2)), List.of());
    var schemePricing =
        baseOf(new StartWithPerKmPriceRuleImpl(1.0, null, List.of(new SectionPriceRuleImpl(section, 3.0, null))));
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("road", PricedModeSelection.unspecified(),
        List.of(road), List.of(section), schemePricing).first();

    breakAtNodeOne();

    assertSame(section, scheme.findSection(segment(0, 1)));
    assertSame(section, scheme.findSection(segment(1, 2)));
    assertEquals(Set.of(segment(0, 1), segment(1, 2)), Set.copyOf(section.getLinkSegments()));
    assertEquals(1, scheme.getPricing().getBasePriceRule().getSectionPriceRules().size());
  }

  /** Breaking a gate's entry keeps as its entry the part starting where the original started */
  @Test
  public void brokenEntryKeepsThePartStartingWhereItStarted() {
    var south = pricing.getGates().getFactory().registerNew(List.of(segment(0, 2)), List.of());
    var north = pricing.getGates().getFactory().registerNew(List.of(), List.of(segment(2, 3)));
    var pairs = gatePairsOf(new GateFromToAmountImpl(south, north, 2.0));
    var scheme = pricing.getEntryExitSchemes().getFactory().registerNew("road", PricedModeSelection.unspecified(),
        List.of(road), List.of(south, north), pairs);
    assertTrue(scheme.second().isEmpty());

    breakAtNodeOne();

    assertEquals(List.of(segment(0, 1)), south.getEntryLinkSegments());
    assertSame(south, pricing.getGates().getByLinkSegment(segment(0, 1)).get(0));
  }

  /**
   * Removing the only link segment of a scheme's only toll road removes the toll road and the scheme, with one warning
   * naming the scheme, and an outside listener hears of the removal ("Toll road removed")
   */
  @Test
  public void tollRoadRemoved() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("other", PricedModeSelection.unspecified(),
        List.of(other), List.of(), startOf(1.0)).first();
    var listener = new RemovalListener();
    pricing.getModifier().addListener(listener);

    assertEquals(1, LogRecorder.countWarnings(UpdatePricingOnNetworkChangeHandler.class,
        () -> layer.getLayerModifier().removeEdgeSegment(segment(3, 4))));

    assertNull(layer.getTollRoads().get(other.getId()));
    assertTrue(pricing.getLocationSchemes().isEmpty());
    assertEquals(1, listener.heard.size());
    assertSame(scheme, ((RemovePricingSchemeEvent) listener.heard.get(0)).getRemovedScheme());
  }

  /** Removing every link segment of one of a scheme's two toll roads leaves it on the other ("One of two toll roads
   * removed") */
  @Test
  public void oneOfTwoTollRoadsRemoved() {
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("both", PricedModeSelection.unspecified(),
        List.of(road, other), List.of(), startOf(1.0)).first();

    layer.getLayerModifier().removeEdgeSegment(segment(3, 4));

    assertEquals(List.of(road), scheme.getTollRoads());
    assertSame(scheme, pricing.getLocationSchemes().get(scheme.getId()));
  }

  /** A link segment a mode scheme lists in its own priced link segments, once removed, is no longer listed, and the
   * scheme still charges on the rest ("Listed link segment removed") */
  @Test
  public void listedLinkSegmentRemoved() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegments(List.of(segment(2, 3), segment(3, 4)));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("cars", PricedModeSelection.unspecified(),
        coverage, List.of(), startOf(1.0)).first();
    var kept = segment(3, 4);

    layer.getLayerModifier().removeEdgeSegment(segment(2, 3));

    assertEquals(List.of(kept), scheme.getLinkSegmentCoverage().getIncludedLinkSegments());
    assertTrue(scheme.isPricedLinkSegment(kept));
  }

  /** Breaking the link of a link segment a mode scheme lists leaves both new link segments listed and charged ("Break a
   * listed link segment") */
  @Test
  public void breakAListedLinkSegment() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegments(List.of(segment(0, 2)));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("cars", PricedModeSelection.unspecified(),
        coverage, List.of(), startOf(1.0)).first();

    breakAtNodeOne();

    assertEquals(Set.of(segment(0, 1), segment(1, 2)), Set.copyOf(coverage.getIncludedLinkSegments()));
    assertTrue(scheme.isPricedLinkSegment(segment(0, 1)));
    assertTrue(scheme.isPricedLinkSegment(segment(1, 2)));
  }

  /**
   * A mode scheme including link segments only, all of them removed, is removed with one warning naming it, and its
   * removal is announced, rather than it charging on every link segment of the layers ("Last included link segment
   * removed")
   */
  @Test
  public void lastIncludedLinkSegmentRemoved() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegments(List.of(segment(2, 3)));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("cars", PricedModeSelection.unspecified(),
        coverage, List.of(), startOf(1.0)).first();
    var listener = new RemovalListener();
    pricing.getModifier().addListener(listener);

    assertEquals(1, LogRecorder.countWarnings(UpdatePricingOnNetworkChangeHandler.class,
        () -> layer.getLayerModifier().removeEdgeSegment(segment(2, 3))));

    assertTrue(pricing.getModeSchemes().isEmpty());
    assertEquals(1, listener.heard.size());
    assertSame(scheme, ((RemovePricingSchemeEvent) listener.heard.get(0)).getRemovedScheme());
  }

  /** A mode scheme including a type and a link segment, the link segment removed, stays and charges on the link
   * segments of the type ("Last included link segment removed while types remain") */
  @Test
  public void lastIncludedLinkSegmentRemovedWhileTypesRemain() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegmentTypes(List.of(segment(3, 4).getLinkSegmentType()));
    coverage.includeLinkSegments(List.of(segment(2, 3)));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("cars", PricedModeSelection.unspecified(),
        coverage, List.of(), startOf(1.0)).first();

    assertEquals(0, LogRecorder.countWarnings(UpdatePricingOnNetworkChangeHandler.class,
        () -> layer.getLayerModifier().removeEdgeSegment(segment(2, 3))));

    assertSame(scheme, pricing.getModeSchemes().get(scheme.getId()));
    assertTrue(scheme.isPricedLinkSegment(segment(3, 4)));
  }

  /**
   * A link segment type a mode scheme includes and a section holds, removed with the infrastructure no mode may use,
   * leaves both; the scheme keeps its other type and the section its link segment ("Link segment type removed")
   */
  @Test
  public void linkSegmentTypeRemoved() {
    var roadType = segment(2, 3).getLinkSegmentType();
    var closed = layer.getLinkSegmentTypes().getFactory().registerNew("closed", 1800, 180);
    segment(5, 0).setLinkSegmentType(closed);
    var section = pricing.getSections().getFactory().registerNew(List.of(segment(2, 3)), List.of(closed));
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegmentTypes(List.of(roadType, closed));
    var scheme = pricing.getModeSchemes().getFactory().registerNew("cars", PricedModeSelection.unspecified(),
        coverage, List.of(section), startOf(1.0)).first();

    layer.getLayerModifier().removeInfrastructureWithoutModeAccess();

    assertNull(layer.getLinkSegmentTypes().get(closed.getId()));
    assertEquals(List.of(roadType), scheme.getLinkSegmentCoverage().getIncludedLinkSegmentTypes());
    assertTrue(section.getLinkSegmentTypes().isEmpty());
    assertSame(section, pricing.getSections().get(section.getId()));
    assertSame(scheme, pricing.getModeSchemes().get(scheme.getId()));
  }

  /**
   * A section whose every link segment is removed is removed, its section price rule dropped with one warning, and the
   * scheme keeps its other section ("Section left empty")
   */
  @Test
  public void sectionLeftEmpty() {
    var first = pricing.getSections().getFactory().registerNew(List.of(segment(0, 2)), List.of());
    var second = pricing.getSections().getFactory().registerNew(List.of(segment(2, 3)), List.of());
    var schemePricing = baseOf(new StartWithPerKmPriceRuleImpl(1.0, null, List.of(new SectionPriceRuleImpl(first, 2.0,
        null),
        new SectionPriceRuleImpl(second, 3.0, null))));
    var scheme = pricing.getLocationSchemes().getFactory().registerNew("road", PricedModeSelection.unspecified(),
        List.of(road), List.of(first, second), schemePricing).first();

    assertEquals(1, LogRecorder.countWarnings(UpdatePricingOnNetworkChangeHandler.class,
        () -> layer.getLayerModifier().removeEdgeSegment(segment(0, 2))));

    assertNull(pricing.getSections().get(first.getId()));
    assertEquals(List.of(second), scheme.getSections());
    assertEquals(1, scheme.getPricing().getBasePriceRule().getSectionPriceRules().size());
    assertSame(second, scheme.getPricing().getBasePriceRule().getSectionPriceRules().get(0).getSection());
  }

  /**
   * A removed scheme leaves the caps covering it: a cap covering it alone is removed, one covering it and another
   * covers the other only ("Cap left without its scheme")
   */
  @Test
  public void capLeftWithoutItsScheme() {
    var removed = pricing.getLocationSchemes().getFactory().registerNew("other", PricedModeSelection.unspecified(),
        List.of(other), List.of(), startOf(1.0)).first();
    var kept = pricing.getLocationSchemes().getFactory().registerNew("road", PricedModeSelection.unspecified(),
        List.of(road), List.of(), startOf(1.0)).first();
    var alone = pricing.getCaps().getFactory().registerNew(List.of(removed), 10.0, null, PricingPeriod.DAY,
        PricedModeSelection.unspecified());
    var shared = pricing.getCaps().getFactory().registerNew(List.of(removed, kept), 20.0, null, PricingPeriod.WEEK,
        PricedModeSelection.unspecified());

    layer.getLayerModifier().removeEdgeSegment(segment(3, 4));

    assertNull(pricing.getCaps().get(alone.getId()));
    assertEquals(List.of(kept), shared.getSchemes());
    assertEquals(List.of(shared), pricing.getCaps().getByScheme(kept));
  }
}
