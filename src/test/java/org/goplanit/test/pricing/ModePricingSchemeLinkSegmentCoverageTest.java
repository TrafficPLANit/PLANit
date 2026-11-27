package org.goplanit.test.pricing;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the link segments a mode scheme charges on, as its include and exclude give them: by layer, by link segment
 * type and one by one, tested on demand
 * <pre>
 * 0 --motorway--> 1 --motorway--> 2
 *                 |
 *           motorway_link
 *                 v
 *                 3
 *
 * 4 --primary--> 5
 * </pre>
 *
 * @author markr
 */
public class ModePricingSchemeLinkSegmentCoverageTest extends LayerTestBase {

  private MacroscopicNetwork network;
  private MacroscopicLinkSegmentType motorway;
  private MacroscopicLinkSegmentType motorwayLink;
  private MacroscopicLinkSegmentType primary;

  /**
   * The link segments covered, from the network's layers
   *
   * @param coverage to stream
   * @return the link segments
   */
  private Set<MacroscopicLinkSegment> streamed(ModePricingSchemeLinkSegmentCoverage coverage) {
    return coverage.stream(network.getTransportLayers()).collect(Collectors.toSet());
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car));
    motorway = layer.getLinkSegmentTypes().getFactory().registerNew("motorway", 2000, 180);
    motorwayLink = layer.getLinkSegmentTypes().getFactory().registerNew("motorway_link", 1500, 180);
    primary = layer.getLinkSegmentTypes().getFactory().registerNew("primary", 1200, 180);

    createNodes(6);
    link(0, 1, motorway, null);
    link(1, 2, motorway, null);
    link(1, 3, motorwayLink, null);
    link(4, 5, primary, null);
  }

  /** Nothing included or excluded: every link segment of every layer, as a mode scheme over the whole network */
  @Test
  public void emptyCoverageHoldsEveryLinkSegment() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    assertEquals(layer.getLinkSegments().stream().collect(Collectors.toSet()), streamed(coverage));
    assertTrue(coverage.contains(segment(1, 3)));
  }

  /**
   * Motorways and their ramps less the ramps, the primary road 4-&gt;5 one by one, less the motorway 1-&gt;2 one by
   * one: an excluded type or link segment wins over an included one
   */
  @Test
  public void typesAndLinkSegmentsIncludedAndExcluded() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegmentTypes(List.of(motorway, motorwayLink));
    coverage.excludeLinkSegmentTypes(List.of(motorwayLink));
    coverage.includeLinkSegments(List.of(segment(4, 5)));
    coverage.excludeLinkSegments(List.of(segment(1, 2)));

    assertTrue(coverage.contains(segment(0, 1)));
    assertFalse(coverage.contains(segment(1, 2)));
    assertFalse(coverage.contains(segment(1, 3)));
    assertTrue(coverage.contains(segment(4, 5)));
    assertEquals(Set.of(segment(0, 1), segment(4, 5)), streamed(coverage));
  }

  /**
   * Layers considered narrow the coverage to their link segments; a whole layer is a layer considered with nothing
   * included
   */
  @Test
  public void consideredLayersNarrowTheCoverage() {
    var otherLayer = network.getTransportLayers().getFactory().registerNew();
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.considerLayers(List.of(otherLayer));
    assertFalse(coverage.contains(segment(0, 1)));
    assertTrue(streamed(coverage).isEmpty());

    coverage.considerLayers(List.of(layer));
    assertTrue(coverage.contains(segment(0, 1)));
    assertEquals(layer.getLinkSegments().stream().collect(Collectors.toSet()), streamed(coverage));
  }

  /** A link segment the network removes is taken off the link segments included or excluded one by one */
  @Test
  public void removedLinkSegmentLeavesTheLists() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegments(List.of(segment(4, 5)));
    coverage.excludeLinkSegments(List.of(segment(1, 2)));

    assertTrue(coverage.removeLinkSegment(segment(4, 5)));
    assertTrue(coverage.removeLinkSegment(segment(1, 2)));
    assertFalse(coverage.removeLinkSegment(segment(0, 1)));
    assertTrue(coverage.getIncludedLinkSegments().isEmpty());
    assertTrue(coverage.getExcludedLinkSegments().isEmpty());
  }

  /**
   * A link segment type the network removes is taken off the types included or excluded; left with neither an included
   * type nor an included link segment, the coverage says so, as it would then hold every link segment
   */
  @Test
  public void removedTypeLeavesTheLists() {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    coverage.includeLinkSegmentTypes(List.of(motorway));
    coverage.excludeLinkSegmentTypes(List.of(motorwayLink));
    assertTrue(coverage.hasIncludedLinkSegmentTypesOrLinkSegments());

    assertTrue(coverage.removeLinkSegmentType(motorway));
    assertTrue(coverage.removeLinkSegmentType(motorwayLink));
    assertFalse(coverage.removeLinkSegmentType(primary));
    assertTrue(coverage.getIncludedLinkSegmentTypes().isEmpty());
    assertTrue(coverage.getExcludedLinkSegmentTypes().isEmpty());
    assertFalse(coverage.hasIncludedLinkSegmentTypesOrLinkSegments());
  }
}
