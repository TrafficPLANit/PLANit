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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Point;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where a stop goes on an access link segment. The ideal stop point is the point of the link closest to the platform's
 * centre. The segment's end node, or its start node where another link enters it, is reused when the vehicle can use
 * it and it lies within 10 m of the ideal stop point, the nearer of the two when both do; otherwise the link is broken
 * at a shape point within 5 m of the ideal stop point, or else at the ideal stop point itself. When neither works, the
 * stop goes where the nearest existing point check puts it, marked as such. The side of the road a waiting area lies
 * on is judged from its centre.
 */
public class StopLocationOnLinkSegmentTest extends LayerTestBase {

  /** distances in metres */
  private static final PlanitJtsCrsUtils GEO_UTILS =
      new PlanitJtsCrsUtils(PlanitJtsCrsUtils.DEFAULT_PROJECTED_CRS_EPSG_3857);

  /** search radius from a platform to its stop location */
  private static final double SEARCH_RADIUS_METERS = 25;

  private Mode train;

  private MacroscopicLinkSegmentType track;

  /** a two-way track from the first to the last point, passing through the points in between as shape points */
  private MacroscopicLink trackThrough(Coordinate... points) {
    createNodes(2);
    nodes.get(nodes.size() - 2).setPosition(PlanitJtsUtils.createPoint(points[0]));
    nodes.get(nodes.size() - 1).setPosition(PlanitJtsUtils.createPoint(points[points.length - 1]));
    var link = twoWay(nodes.size() - 2, nodes.size() - 1, track);
    link.setGeometry(PlanitJtsUtils.createLineString(points));
    return link;
  }

  private StopLocationOnLinkSegment stopLocation(Geometry platform, MacroscopicLinkSegment segment) {
    return ZoningConverterUtils.findStopLocationForWaitingAreaOnLinkSegment(
        "platform", platform, segment, segment.getParent().getXmlId(), train, SEARCH_RADIUS_METERS,
        ZoningConverterUtils.DEFAULT_MAX_END_NODE_TO_IDEAL_STOP_POINT_METERS,
        ZoningConverterUtils.DEFAULT_MAX_SHAPE_POINT_TO_IDEAL_STOP_POINT_METERS,
        null, null, null, CountryNames.AUSTRALIA, GEO_UTILS);
  }

  private static void assertAt(double x, double y, Point location) {
    assertNotNull(location);
    assertEquals(x, location.getX(), 1e-6);
    assertEquals(y, location.getY(), 1e-6);
  }

  /** a node at the given position, by its index */
  private int nodeAt(double x, double y) {
    createNodes(1);
    nodes.get(nodes.size() - 1).setPosition(PlanitJtsUtils.createPoint(x, y));
    return nodes.size() - 1;
  }

  /** a one-way track from node a to node b, straight between their positions */
  private MacroscopicLink oneWayTrack(int a, int b) {
    return straightTrack(a, b, false);
  }

  /** a two-way track between node a and node b, straight between their positions */
  private MacroscopicLink twoWayTrack(int a, int b) {
    return straightTrack(a, b, true);
  }

  /** a track from node a to node b, straight between their positions, also from b to a when two-way */
  private MacroscopicLink straightTrack(int a, int b, boolean twoWay) {
    var link = link(a, b, track, twoWay ? track : null);
    link.setGeometry(PlanitJtsUtils.createLineString(
        nodes.get(a).getPosition().getCoordinate(), nodes.get(b).getPosition().getCoordinate()));
    return link;
  }

  /**
   * A layer with a track type trains may use
   */
  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    train = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(train));
    track = layer.getLinkSegmentTypes().getFactory().registerNew("track", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(track, 80, List.of(train));
  }

  /**
   * <pre>
   *   A ========================================================== B    track A-B
   *  (0,0)                                              (94,0)  (100,0)
   *                                                       p (94,3) platform
   * </pre>
   * The ideal stop point (94,0) lies 6 m from end node B, within 10 m: B is reused. Its nearest place to the platform's
   * centre is the ideal stop point, 3 m away
   */
  @Test
  public void endNodeWithinTenMetresOfTheIdealStopPointIsReused() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(100, 0));

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(94, 3), link.getLinkSegmentAb());

    assertAt(100, 0, stopLocation.getLocation());
    assertFalse(stopLocation.isFromFallbackCheck());
    assertEquals(3, stopLocation.getMetersFromWaitingAreaCentreToNearestPlace(), 1e-6);
  }

  /**
   * <pre>
   *   P ==================> A ================================================> B    one-way tracks P-A and A-B
   * (-50,0)               (0,0) (3,0)                                       (100,0)
   *                               p (3,2) platform
   * </pre>
   * Along A to B the ideal stop point (3,0) lies 3 m past start node A, within 10 m, and a train reaches A over P-A:
   * A is reused rather than breaking the link right next to it
   */
  @Test
  public void startNodeWithinTenMetresOfTheIdealStopPointIsReusedWhenAnotherTrackEntersIt() {
    int p = nodeAt(-50, 0);
    int a = nodeAt(0, 0);
    int b = nodeAt(100, 0);
    oneWayTrack(p, a);
    var linkAb = oneWayTrack(a, b);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(3, 2), linkAb.getLinkSegmentAb());

    assertAt(0, 0, stopLocation.getLocation());
    assertFalse(stopLocation.isFromFallbackCheck());
  }

  /**
   * <pre>
   *  A ================================================> B    one-way track A-B, A where the network starts
   * (0,0) (3,0)                                       (100,0)
   *         p (3,2) platform
   * </pre>
   * No track enters A, so no train reaches it: the link is broken at the ideal stop point (3,0)
   */
  @Test
  public void startNodeNoOtherTrackEntersIsNotReused() {
    int a = nodeAt(0, 0);
    int b = nodeAt(100, 0);
    var linkAb = oneWayTrack(a, b);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(3, 2), linkAb.getLinkSegmentAb());

    assertAt(3, 0, stopLocation.getLocation());
  }

  /**
   * <pre>
   *                       Q (0,50)
   *                       ||
   *                       \/
   *   P ==================> A ================================================> B    one-way tracks P-A, Q-A and A-B
   * (-50,0)               (0,0) (3,0)                                       (100,0)
   *                               p (3,2) platform
   * </pre>
   * Trains reach A over P-A and over Q-A, so which of the two the trains stopping at the platform come from cannot be
   * told: the link is broken at the ideal stop point (3,0) rather than A being reused
   */
  @Test
  public void startNodeTwoOtherTracksEnterIsNotReused() {
    int p = nodeAt(-50, 0);
    int q = nodeAt(0, 50);
    int a = nodeAt(0, 0);
    int b = nodeAt(100, 0);
    oneWayTrack(p, a);
    oneWayTrack(q, a);
    var linkAb = oneWayTrack(a, b);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(3, 2), linkAb.getLinkSegmentAb());

    assertAt(3, 0, stopLocation.getLocation());
  }

  /**
   * <pre>
   *                              Q (20,-30)
   *                             ^
   *                           //
   *   P ==================> A ================================================> B    one-way tracks P-A, A-B and A-Q
   * (-50,0)               (0,0) (3,0)                                       (100,0)
   *                               p (3,2) platform
   * </pre>
   * Trains reach A only over P-A, but leave it along A-B and along A-Q, so a train stopping at A may never pass the
   * platform: the link is broken at the ideal stop point (3,0) rather than A being reused
   */
  @Test
  public void startNodeTrainsAlsoLeaveAlongAnotherTrackIsNotReused() {
    int p = nodeAt(-50, 0);
    int a = nodeAt(0, 0);
    int b = nodeAt(100, 0);
    int q = nodeAt(20, -30);
    oneWayTrack(p, a);
    oneWayTrack(a, q);
    var linkAb = oneWayTrack(a, b);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(3, 2), linkAb.getLinkSegmentAb());

    assertAt(3, 0, stopLocation.getLocation());
  }

  /**
   * <pre>
   *   P <=================> A <===============================================> B    two-way tracks P-A and A-B
   * (-50,0)               (0,0) (3,0)                                       (100,0)
   *                               p (3,2) platform
   * </pre>
   * Along A to B, trains reach A only over P-A and leave it only along A-B, turning back to P aside: A is reused
   */
  @Test
  public void startNodeOfTwoWayTrackIsReusedAsTurningBackDoesNotCount() {
    int p = nodeAt(-50, 0);
    int a = nodeAt(0, 0);
    int b = nodeAt(100, 0);
    twoWayTrack(p, a);
    var linkAb = twoWayTrack(a, b);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(3, 2), linkAb.getLinkSegmentAb());

    assertAt(0, 0, stopLocation.getLocation());
  }

  /**
   * <pre>
   *   P ==========> A =================== B ==========> Q    one-way tracks P-A, A-B and B-Q
   * (-50,0)       (0,0)    (5,0)        (12,0)      (60,0)
   *                         p (5,2) platform
   * </pre>
   * Both ends of A to B lie within 10 m of the ideal stop point (5,0): the nearer one, start node A, is reused
   */
  @Test
  public void ofBothEndNodesWithinTenMetresTheOneNearerTheIdealStopPointIsReused() {
    int p = nodeAt(-50, 0);
    int a = nodeAt(0, 0);
    int b = nodeAt(12, 0);
    int q = nodeAt(60, 0);
    oneWayTrack(p, a);
    var linkAb = oneWayTrack(a, b);
    oneWayTrack(b, q);

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(5, 2), linkAb.getLinkSegmentAb());

    assertAt(0, 0, stopLocation.getLocation());
  }

  /**
   * <pre>
   *   A ========================================================== B    track A-B
   *  (0,0)                                        (85,0)        (100,0)
   *                                                 p (85,3) platform
   * </pre>
   * The ideal stop point (85,0) lies 15 m from end node B, beyond 10 m: the link is broken at the ideal stop point
   */
  @Test
  public void endNodeBeyondTenMetresOfTheIdealStopPointLeavesTheLinkBrokenThere() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(100, 0));

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(85, 3), link.getLinkSegmentAb());

    assertAt(85, 0, stopLocation.getLocation());
    assertFalse(stopLocation.isFromFallbackCheck());
    assertEquals(3, stopLocation.getMetersFromWaitingAreaCentreToNearestPlace(), 1e-6);
  }

  /**
   * <pre>
   *   A ============================== S ============================== B    track A-S-B, S a shape point
   *  (0,0)                          (50,0)                          (100,0)
   *                                     p (53,2)     q (57,2) platforms
   * </pre>
   * For p the ideal stop point (53,0) lies 3 m from shape point S, within 5 m: the link is broken at S. For q it lies
   * 7 m from S: the link is broken at the ideal stop point (57,0)
   */
  @Test
  public void linkIsBrokenAtAShapePointWithinFiveMetresOfTheIdealStopPoint() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(50, 0), new Coordinate(100, 0));

    assertAt(50, 0, stopLocation(PlanitJtsUtils.createPoint(53, 2), link.getLinkSegmentAb()).getLocation());
    assertAt(57, 0, stopLocation(PlanitJtsUtils.createPoint(57, 2), link.getLinkSegmentAb()).getLocation());
  }

  /**
   * <pre>
   *             +--------------------------------------------+       platform from (10,2) to (70,4)
   *             |                      c (40,3)              |       c its centre
   *             +--------------------------------------------+
   *   A ========================================================================== B    track A-B
   *  (0,0)                             (40,0)                                   (100,0)
   * </pre>
   * The platform's whole long edge is equally close to the track; the ideal stop point is the point closest to its
   * centre, (40,0), halfway along the platform
   */
  @Test
  public void idealStopPointLiesOppositeThePlatformCentre() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(100, 0));
    var platform = PlanitJtsUtils.createPolygon(new Coordinate[]{
        new Coordinate(10, 2), new Coordinate(70, 2), new Coordinate(70, 4), new Coordinate(10, 4),
        new Coordinate(10, 2)});

    var stopLocation = stopLocation(platform, link.getLinkSegmentAb());

    assertAt(40, 0, stopLocation.getLocation());
    assertEquals(3, stopLocation.getMetersFromWaitingAreaCentreToNearestPlace(), 1e-6);
  }

  /**
   * <pre>
   *          A ================================= B    track A-B, A where the network starts
   *  p     (0,0)                               (20,0)
   * (-3,2) platform, before the start
   * </pre>
   * Along A to B the ideal stop point is A, which a vehicle on the segment has passed already, and end node B lies 20 m
   * from it, beyond 10 m. The stop goes where the nearest existing point check puts it: B, 23.1 m from the platform,
   * within the search radius
   */
  @Test
  public void platformBeforeTheSegmentStartGetsTheLocationOfTheNearestExistingPointCheck() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(20, 0));

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(-3, 2), link.getLinkSegmentAb());

    assertAt(20, 0, stopLocation.getLocation());
    assertTrue(stopLocation.isFromFallbackCheck());
  }

  /**
   * <pre>
   *   A ================================= B                       track A-B, B the end of a pier
   *  (0,0)                              (50,0)          p (70,0) platform, beyond the end
   * </pre>
   * The point of the track closest to the platform is end node B itself, 20 m away: B is the stop, without breaking
   */
  @Test
  public void endNodeThatIsTheIdealStopPointIsTheStop() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(50, 0));

    var stopLocation = stopLocation(PlanitJtsUtils.createPoint(70, 0), link.getLinkSegmentAb());

    assertAt(50, 0, stopLocation.getLocation());
    assertFalse(stopLocation.isFromFallbackCheck());
  }

  /**
   * <pre>
   *  (-50,9) +-----------------------------------------------------------+ (50,9)
   *          |  body                                                     |
   *          |  +--------------------------------------------------------+ (50,5)
   *          |  |
   *          |  |                  A ========== B          piece A-B, eastward, A (0,0), B (10,0)
   *          |  +---------------------------------+ (12,-2)
   *          |  lobe                              |
   * (-50,-3) +------------------------------------+ (12,-3)
   * </pre>
   * The platform's southern lobe comes nearest to B, but its centre lies north, in its body: the platform is judged to
   * lie left of the eastward piece
   */
  @Test
  public void sideOfTheRoadIsJudgedFromTheWaitingAreaCentre() {
    var link = trackThrough(new Coordinate(0, 0), new Coordinate(10, 0));
    var platform = PlanitJtsUtils.createPolygon(new Coordinate[]{
        new Coordinate(-50, -3), new Coordinate(12, -3), new Coordinate(12, -2), new Coordinate(-49, -2),
        new Coordinate(-49, 5), new Coordinate(50, 5), new Coordinate(50, 9), new Coordinate(-50, 9),
        new Coordinate(-50, -3)});

    assertTrue(ZoningConverterUtils.isWaitingAreaLeftOfAccessLineSegment(
        platform, link.getLinkSegmentAb(), new LineSegment(new Coordinate(0, 0), new Coordinate(10, 0)), GEO_UTILS));
  }
}
