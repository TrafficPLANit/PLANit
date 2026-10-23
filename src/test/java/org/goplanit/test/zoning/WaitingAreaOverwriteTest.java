package org.goplanit.test.zoning;

import org.goplanit.converter.zoning.ZoningConverterUtils;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The user may attach a node to a waiting area, or nominate the access link of a waiting area. Either lets a segment
 * serve the waiting area even when a bus on it would cross traffic to reach it. A node attached to another waiting
 * area gives no such exemption.
 * <pre>
 *   A (0,1) ======================================= B (10,1)    link X, a segment each way
 *
 *                           o (5,0) waiting area W
 * </pre>
 * The waiting area lies right of the A to B segment, so with traffic on the left a bus on that segment would cross
 * traffic to reach it.
 */
public class WaitingAreaOverwriteTest extends LayerTestBase {

  /** distances in metres */
  private static final PlanitJtsCrsUtils GEO_UTILS =
      new PlanitJtsCrsUtils(PlanitJtsCrsUtils.DEFAULT_PROJECTED_CRS_EPSG_3857);

  /** waiting area W, south of link X */
  private static final Point WAITING_AREA = PlanitJtsUtils.createPoint(5, 0);

  private Mode bus;

  private MacroscopicLink x;

  /**
   * Whether the A to B segment of X may serve waiting area W, given the user's overwrites
   *
   * @param waitingAreaOfNode the waiting area the user attached a node to, null when none
   * @param accessLinkOfWaitingArea the access link the user nominated for a waiting area, null when none
   * @return true when the segment may serve W
   */
  private boolean mayServeWaitingArea(
      Function<Node, String> waitingAreaOfNode, Function<String, String> accessLinkOfWaitingArea) {
    var segment = x.getLinkSegmentAb();
    return ZoningConverterUtils.isPotentialAccessEntryLinkSegmentForWaitingArea(
        "W", WAITING_AREA, segment, "X", segment.getDownstreamNode(), bus, waitingAreaOfNode,
        accessLinkOfWaitingArea, CountryNames.AUSTRALIA, GEO_UTILS);
  }

  /**
   * A layer with link X for buses
   */
  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus));
    var type = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(type, 50, List.of(bus));
    createNodes(2);
    nodes.get(0).setPosition(PlanitJtsUtils.createPoint(0, 1));
    nodes.get(1).setPosition(PlanitJtsUtils.createPoint(10, 1));
    x = link(0, 1, type, type);
    x.setGeometry(PlanitJtsUtils.createLineString(new Coordinate(0, 1), new Coordinate(10, 1)));
  }

  /** without overwrites the segment across traffic does not serve the waiting area */
  @Test
  public void segmentAcrossTrafficDoesNotServeTheWaitingArea() {
    assertFalse(mayServeWaitingArea(null, null));
  }

  /** the user attached the segment's node to W, so it serves W */
  @Test
  public void segmentAcrossTrafficServesTheWaitingAreaItsNodeIsAttachedTo() {
    assertTrue(mayServeWaitingArea(node -> "W", null));
  }

  /** the user attached the segment's node to another waiting area, which gives no exemption for W */
  @Test
  public void segmentAcrossTrafficDoesNotServeAWaitingAreaItsNodeIsNotAttachedTo() {
    assertFalse(mayServeWaitingArea(node -> "V", null));
  }

  /** the user nominated link X for W, so its segment serves W */
  @Test
  public void segmentAcrossTrafficServesTheWaitingAreaItsLinkIsNominatedFor() {
    assertTrue(mayServeWaitingArea(null, waitingArea -> "X"));
  }

  /** among the access link segments of X for W, the one across traffic is left out, unless its node is attached to W */
  @Test
  public void accessLinkSegmentsKeepTheOneAcrossTrafficOnlyWhenItsNodeIsAttachedToTheWaitingArea() {
    var withoutOverwrite = ZoningConverterUtils.findAccessLinkSegmentsForWaitingAreaOnLinks("W", WAITING_AREA,
        List.of(x), link -> "X", List.of(bus), mode -> null, null, CountryNames.AUSTRALIA, GEO_UTILS);
    var nodeAttached = ZoningConverterUtils.findAccessLinkSegmentsForWaitingAreaOnLinks("W", WAITING_AREA,
        List.of(x), link -> "X", List.of(bus), mode -> null, node -> "W", CountryNames.AUSTRALIA, GEO_UTILS);

    assertFalse(withoutOverwrite.contains(x.getLinkSegmentAb()));
    assertTrue(nodeAttached.contains(x.getLinkSegmentAb()));
  }
}
