package org.goplanit.test.zoning;

import org.goplanit.converter.zoning.StopLocationOnLinkSegment;
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
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Choosing the link a waiting area's stop goes on: only segments a stop location can be made on count, the link
 * closest to the waiting area wins, unless a link within the buffer of the closest one has a higher capacity, which
 * wins for a road mode. Closeness is the distance to a link's geometry, or to its stop location: how close its usable
 * places come to the waiting area's centre. The access segments to choose from
 * are the segments of the nearby links on the waiting area's side of the road, and on the chosen link the stop goes
 * where an end node is reused, else at the nearest location.
 * <pre>
 *   W  (0,20) ===================================== (10,20)
 *
 *   Z  (0,5)  ===================================== (10,5)
 *   Y  (0,3)  ===================================== (10,3)
 *   X  (0,1)  ===================================== (10,1)
 *                                o (5,0) waiting area
 * </pre>
 * Each link runs from west (A) to east (B), so the waiting area lies right of its A to B segment and left of its B to A
 * segment; with traffic on the left, a bus may only use the B to A segment.
 */
public class StopLocationLinkChoiceTest extends LayerTestBase {

  /** distances in metres */
  private static final PlanitJtsCrsUtils GEO_UTILS =
      new PlanitJtsCrsUtils(PlanitJtsCrsUtils.DEFAULT_PROJECTED_CRS_EPSG_3857);

  /** the waiting area, a point south of every link */
  private static final Point WAITING_AREA = PlanitJtsUtils.createPoint(5, 0);

  /** the buffer beyond the closest link within which links are compared further */
  private static final double BUFFER_METERS = 8;

  private Mode bus;

  private Mode train;

  private MacroscopicLinkSegmentType capacity1000;

  private MacroscopicLinkSegmentType capacity1500;

  private MacroscopicLinkSegmentType capacity2000;

  private MacroscopicLinkSegmentType segmentType(String name, double capacityPerLane) {
    var type = layer.getLinkSegmentTypes().getFactory().registerNew(name, capacityPerLane, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(type, 50, List.of(bus, train));
    return type;
  }

  /** a straight link from (0,y) to (10,y) with a segment per given type, none when the type is null */
  private MacroscopicLink linkAt(double y, MacroscopicLinkSegmentType typeAb, MacroscopicLinkSegmentType typeBa) {
    createNodes(2);
    var nodeA = nodes.get(nodes.size() - 2);
    var nodeB = nodes.get(nodes.size() - 1);
    nodeA.setPosition(PlanitJtsUtils.createPoint(0, y));
    nodeB.setPosition(PlanitJtsUtils.createPoint(10, y));
    var link = link(nodes.size() - 2, nodes.size() - 1, typeAb, typeBa);
    link.setGeometry(PlanitJtsUtils.createLineString(new Coordinate(0, y), new Coordinate(10, y)));
    return link;
  }

  private MacroscopicLink choose(List<MacroscopicLinkSegment> candidates, Mode mode) {
    var result = ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToLinkGeometry(
        WAITING_AREA, candidates, List.of(mode), (segment, accessMode) -> true, BUFFER_METERS, GEO_UTILS);
    return result == null ? null : result.first();
  }

  /** a link of the given type through the given points, its end nodes at the first and last point */
  private MacroscopicLink linkThrough(MacroscopicLinkSegmentType type, Coordinate... points) {
    createNodes(2);
    nodes.get(nodes.size() - 2).setPosition(PlanitJtsUtils.createPoint(points[0]));
    nodes.get(nodes.size() - 1).setPosition(PlanitJtsUtils.createPoint(points[points.length - 1]));
    var link = link(nodes.size() - 2, nodes.size() - 1, type, type);
    link.setGeometry(PlanitJtsUtils.createLineString(points));
    return link;
  }

  /** a stop location at the segment's end node, for choosing by stop location */
  private static StopLocationOnLinkSegment stopAtEnd(
      MacroscopicLinkSegment segment, double metersFromCentre, boolean fromFallbackCheck) {
    return new StopLocationOnLinkSegment(
        segment.getDownstreamVertex().getPosition(), metersFromCentre, fromFallbackCheck);
  }

  private MacroscopicLink chooseByStopLocation(
      List<MacroscopicLinkSegment> candidates,
      Mode mode,
      Map<MacroscopicLinkSegment, StopLocationOnLinkSegment> stops) {
    var result = ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToStopLocation(
        candidates, List.of(mode), (segment, accessMode) -> stops.get(segment), BUFFER_METERS);
    return result == null ? null : result.first();
  }

  /**
   * A layer for buses and trains with segment types of three capacities that both modes may use
   */
  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    train = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus, train));
    capacity1000 = segmentType("capacity1000", 1000);
    capacity1500 = segmentType("capacity1500", 1500);
    capacity2000 = segmentType("capacity2000", 2000);
  }

  /** a link whose segments get no stop location is not chosen, however close */
  @Test
  public void onlySegmentsWithAStopLocationCount() {
    var x = linkAt(1, capacity2000, null);
    var z = linkAt(5, capacity2000, null);

    var result = ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToLinkGeometry(
        WAITING_AREA, List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb()), List.of(bus),
        (segment, accessMode) -> segment.getParent() != x, BUFFER_METERS, GEO_UTILS);

    assertSame(z, result.first());
    assertEquals(Set.of(z.getLinkSegmentAb()), result.second());
    assertNull(ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToLinkGeometry(
        WAITING_AREA, List.of(x.getLinkSegmentAb()), List.of(bus), (segment, accessMode) -> false, BUFFER_METERS,
        GEO_UTILS));
  }

  /** a link beyond the buffer of the closest one is not compared on capacity: the closest wins */
  @Test
  public void closestLinkWinsOverAHigherCapacityLinkBeyondTheBuffer() {
    var x = linkAt(1, capacity1000, null);
    var w = linkAt(20, capacity2000, null);
    var candidates = List.of(x.getLinkSegmentAb(), w.getLinkSegmentAb());

    assertSame(x, choose(candidates, bus));
  }

  /** within the buffer of the closest link, a road mode prefers the link with the higher capacity */
  @Test
  public void higherCapacityLinkWithinTheBufferWinsForARoadMode() {
    var x = linkAt(1, capacity1500, null);
    var z = linkAt(5, capacity2000, null);
    var candidates = List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb());

    assertSame(z, choose(candidates, bus));
  }

  /** a rail mode does not prefer capacity: the closest link wins */
  @Test
  public void railModeTakesTheClosestLinkWhateverItsCapacity() {
    var x = linkAt(1, capacity1500, null);
    var z = linkAt(5, capacity2000, null);
    var candidates = List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb());

    assertSame(x, choose(candidates, train));
  }

  /**
   * Link X has the highest capacity access segment but a lower capacity segment the other way. Only access segments
   * are compared, so Y is dropped and the closer X wins over Z
   */
  @Test
  public void linkWithALowerCapacityOtherDirectionWinsWhenItsAccessSegmentHasTheHighest() {
    var x = linkAt(1, capacity2000, capacity1000);
    var y = linkAt(3, capacity1500, null);
    var z = linkAt(5, capacity2000, capacity2000);
    var candidates = List.of(x.getLinkSegmentAb(), y.getLinkSegmentAb(), z.getLinkSegmentAb());

    assertSame(x, choose(candidates, bus));
  }

  /**
   * <pre>
   *  (0,4) +-----------------------------------------------+ (80,4)     platform, its centre at (40,2)
   *  (0,0) +-----------------------------------------------+ (80,0)
   *                                                          + (82,-1) ======== (120,-1)   track Y bends in at the end
   * (-20,-3) ================================================================= (100,-3)    track X along the platform
   *                                                          |
   *                                                          + (80,-30)
   * </pre>
   * Track Y comes nearest to the platform, 2.2 m from its corner, X runs 3 m from its long edge. By distance to the
   * geometry Y is chosen, by distance to the stop location X is: its stop lies 5 m from the platform centre, Y's 42 m
   */
  @Test
  public void byDistanceToStopLocationTheLinkAlongThePlatformWinsOverOneBendingInAtItsEnd() {
    var x = linkThrough(capacity2000, new Coordinate(-20, -3), new Coordinate(100, -3));
    var y = linkThrough(capacity2000, new Coordinate(80, -30), new Coordinate(82, -1), new Coordinate(120, -1));
    var platform = PlanitJtsUtils.createPolygon(new Coordinate[]{
        new Coordinate(0, 0), new Coordinate(80, 0), new Coordinate(80, 4), new Coordinate(0, 4),
        new Coordinate(0, 0)});
    var candidates = List.of(x.getLinkSegmentAb(), y.getLinkSegmentAb());

    var byDistanceToLinkGeometry = ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToLinkGeometry(
        platform, candidates, List.of(train), (segment, accessMode) -> true, BUFFER_METERS, GEO_UTILS);
    var byDistanceToStopLocation = ZoningConverterUtils.findMostAppropriateStopLocationLinkByDistanceToStopLocation(
        candidates, List.of(train),
        (segment, accessMode) -> ZoningConverterUtils.findStopLocationForWaitingAreaOnLinkSegment(
            "platform", platform, segment, segment.getParent().getXmlId(), accessMode, 25,
            ZoningConverterUtils.DEFAULT_MAX_END_NODE_TO_IDEAL_STOP_POINT_METERS,
            ZoningConverterUtils.DEFAULT_MAX_SHAPE_POINT_TO_IDEAL_STOP_POINT_METERS, null, null, null,
            CountryNames.AUSTRALIA, GEO_UTILS),
        BUFFER_METERS);

    assertSame(y, byDistanceToLinkGeometry.first());
    assertSame(x, byDistanceToStopLocation.first());
    assertEquals(Set.of(x.getLinkSegmentAb()), byDistanceToStopLocation.second());
  }

  /**
   * A link whose only stop location comes from the fallback check ranks after every link with a stop location of its
   * own, however near; among fallback locations only, the nearest wins
   */
  @Test
  public void byDistanceToStopLocationALinkWithOnlyAFallbackLocationRanksLast() {
    var x = linkAt(1, capacity2000, null);
    var z = linkAt(5, capacity2000, null);
    var stops = Map.of(
        x.getLinkSegmentAb(), stopAtEnd(x.getLinkSegmentAb(), 1, true),
        z.getLinkSegmentAb(), stopAtEnd(z.getLinkSegmentAb(), 12, false));

    assertSame(z, chooseByStopLocation(List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb()), train, stops));

    var fallbacksOnly = Map.of(
        x.getLinkSegmentAb(), stopAtEnd(x.getLinkSegmentAb(), 7, true),
        z.getLinkSegmentAb(), stopAtEnd(z.getLinkSegmentAb(), 3, true));
    assertSame(z, chooseByStopLocation(List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb()), train, fallbacksOnly));
  }

  /**
   * By distance to the stop location, a road mode prefers the higher capacity link among those whose stop lies within
   * the buffer of the nearest stop, and takes the nearest stop beyond it
   */
  @Test
  public void byDistanceToStopLocationHigherCapacityWithinTheBufferWinsForARoadMode() {
    var x = linkAt(1, capacity1500, null);
    var z = linkAt(5, capacity2000, null);
    var w = linkAt(20, capacity2000, null);
    var stops = Map.of(
        x.getLinkSegmentAb(), stopAtEnd(x.getLinkSegmentAb(), 1, false),
        z.getLinkSegmentAb(), stopAtEnd(z.getLinkSegmentAb(), 5, false),
        w.getLinkSegmentAb(), stopAtEnd(w.getLinkSegmentAb(), 20, false));

    assertSame(z, chooseByStopLocation(List.of(x.getLinkSegmentAb(), z.getLinkSegmentAb()), bus, stops));
    assertSame(x, chooseByStopLocation(List.of(x.getLinkSegmentAb(), w.getLinkSegmentAb()), bus, stops));
    assertNull(chooseByStopLocation(List.of(x.getLinkSegmentAb()), bus, Map.of()));
  }

  /**
   * On one link, a location at an end node of its segment, its end or its start, wins over a nearer one that breaks
   * the link; otherwise the nearest wins, on the segment with the lowest id when equally near; a last resort location
   * only counts when no segment has another
   */
  @Test
  public void onALinkAnEndNodeLocationWinsThenTheNearestThenALastResort() {
    var x = linkAt(1, capacity2000, capacity2000);
    var ab = x.getLinkSegmentAb();
    var ba = x.getLinkSegmentBa();
    var endOfAb = ab.getDownstreamVertex().getPosition();
    var breakPoint = PlanitJtsUtils.createPoint(5, 1);
    var otherBreakPoint = PlanitJtsUtils.createPoint(6, 1);

    assertSame(endOfAb, ZoningConverterUtils.chooseStopLocationOnAccessLink(List.of(ba, ab),
        segment -> segment == ab ? endOfAb : breakPoint, location -> location == endOfAb ? 9 : 2,
        (segment, location) -> false));
    var startOfAb = ab.getUpstreamVertex().getPosition();
    assertSame(startOfAb, ZoningConverterUtils.chooseStopLocationOnAccessLink(List.of(ba, ab),
        segment -> segment == ab ? startOfAb : breakPoint, location -> location == startOfAb ? 9 : 2,
        (segment, location) -> false));
    var lowerIdSegment = ab.getId() < ba.getId() ? ab : ba;
    assertSame(lowerIdSegment == ab ? breakPoint : otherBreakPoint, ZoningConverterUtils.chooseStopLocationOnAccessLink(
        List.of(ba, ab), segment -> segment == ab ? breakPoint : otherBreakPoint, location -> 4,
        (segment, location) -> false));
    assertSame(otherBreakPoint, ZoningConverterUtils.chooseStopLocationOnAccessLink(List.of(ab, ba),
        segment -> segment == ab ? breakPoint : otherBreakPoint, location -> location == breakPoint ? 1 : 9,
        (segment, location) -> segment == ab));
    assertSame(breakPoint, ZoningConverterUtils.chooseStopLocationOnAccessLink(List.of(ab, ba),
        segment -> segment == ab ? breakPoint : null, location -> 1, (segment, location) -> true));
  }

  /**
   * The access segments of nearby links: a bus only gets the segment with the waiting area on its left, as traffic
   * drives on the left; a train may use either direction
   */
  @Test
  public void accessSegmentsFollowTheWaitingAreaSideOfTheRoadForAModeThatMustNotCrossTraffic() {
    var x = linkAt(1, capacity2000, capacity2000);

    var forBus = ZoningConverterUtils.findAccessLinkSegmentsForWaitingAreaOnLinks(
        "waiting area", WAITING_AREA, List.of(x), MacroscopicLink::getXmlId, List.of(bus), mode -> null,
        null, CountryNames.AUSTRALIA, GEO_UTILS);
    var forTrain = ZoningConverterUtils.findAccessLinkSegmentsForWaitingAreaOnLinks(
        "waiting area", WAITING_AREA, List.of(x), MacroscopicLink::getXmlId, List.of(train), mode -> null,
        null, CountryNames.AUSTRALIA, GEO_UTILS);

    assertEquals(Set.of(x.getLinkSegmentBa()), forBus);
    assertEquals(Set.of(x.getLinkSegmentAb(), x.getLinkSegmentBa()), forTrain);
  }
}
