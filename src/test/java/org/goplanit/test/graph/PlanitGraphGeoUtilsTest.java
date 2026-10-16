package org.goplanit.test.graph;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the geo helpers that take graph elements. Most cases use a projected crs in metres, where the expected values
 * are plain plane geometry, on three parallel edges:
 * <pre>
 *   y=20   n4 ---------------- n5     top
 *
 *   y=5    n2 ---------------- n3     middle
 *   y=0    n0 ---------------- n1     bottom
 *          x=0                x=10
 * </pre>
 *
 * @author markr
 */
public class PlanitGraphGeoUtilsTest extends LayerTestBase {

  /** tolerance for plane geometry results in metres */
  private static final double PLANE_TOLERANCE = 1e-9;

  /** projected crs in metres, so distances are those of the plane */
  private final PlanitJtsCrsUtils projected = new PlanitJtsCrsUtils(PlanitJtsCrsUtils.DEFAULT_PROJECTED_CRS_EPSG_3857);

  /** edge along y=0 */
  private MacroscopicLink bottom;

  /** edge along y=5 */
  private MacroscopicLink middle;

  /** edge along y=20 */
  private MacroscopicLink top;

  /**
   * An empty layer to build on, with the three parallel edges of the class description
   */
  @BeforeEach
  public void setUp() {
    layer = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken())
        .getTransportLayers().getFactory().registerNew();
    bottom = straightLink(nodeAt(0, 0), nodeAt(10, 0));
    middle = straightLink(nodeAt(0, 5), nodeAt(10, 5));
    top = straightLink(nodeAt(0, 20), nodeAt(10, 20));
  }

  /**
   * Register a node at the given position
   *
   * @param x coordinate
   * @param y coordinate
   * @return index of the node
   */
  private int nodeAt(double x, double y) {
    createNodes(1);
    nodes.get(nodes.size() - 1).setPosition(PlanitJtsUtils.createPoint(x, y));
    return nodes.size() - 1;
  }

  /**
   * Connect two positioned nodes with a segment in each direction and a straight geometry from a to b
   *
   * @param a index of node A
   * @param b index of node B
   * @return the link
   */
  private MacroscopicLink straightLink(int a, int b) {
    var link = twoWay(a, b);
    link.setGeometry(PlanitJtsUtils.createLineString(
        nodes.get(a).getPosition().getCoordinate(), nodes.get(b).getPosition().getCoordinate()));
    return link;
  }

  // ---------------------------------------------------------------------------------------------------------------
  // closest vertex
  // ---------------------------------------------------------------------------------------------------------------

  /** The closest vertex is found among those with a position, by index, and none is found without any position */
  @Test
  public void closestVertexIgnoresVerticesWithoutPosition() {
    createNodes(1);
    var withoutPosition = nodes.get(nodes.size() - 1);

    assertEquals(1, PlanitGraphGeoUtils.findVertexClosestTo(
        new Coordinate(8, 1), projected, withoutPosition, nodes.get(1), nodes.get(0)));
    assertEquals(2, PlanitGraphGeoUtils.findVertexClosestTo(
        new Coordinate(1, 4), projected, withoutPosition, nodes.get(0), nodes.get(2)));
    assertEquals(-1, PlanitGraphGeoUtils.findVertexClosestTo(new Coordinate(1, 1), projected, withoutPosition));
  }

  // ---------------------------------------------------------------------------------------------------------------
  // closest edges
  // ---------------------------------------------------------------------------------------------------------------

  /** The closest edge to a point is the one its projection onto is shortest */
  @Test
  public void closestEdgeToPoint() {
    var point = PlanitJtsUtils.createPoint(5, 1);
    var edges = List.of(top, middle, bottom);

    assertSame(bottom, PlanitGraphGeoUtils.findEdgeClosestToPoint(point, edges, projected));
    assertSame(bottom, PlanitGraphGeoUtils.findEdgeClosest(point, edges, projected));
  }

  /** The closest edge to a line string is the one closest to any of its coordinates */
  @Test
  public void closestEdgeToLineStringUsesItsClosestCoordinate() {
    /* (5,30) is nearest the top edge, but (5,6) is closer still to the middle one */
    var lineString = PlanitJtsUtils.createLineString(new Coordinate(5, 30), new Coordinate(5, 6));
    var edges = List.of(top, middle, bottom);

    assertSame(middle, PlanitGraphGeoUtils.findEdgeClosestToLineString(lineString, edges, projected));
    assertSame(middle, PlanitGraphGeoUtils.findEdgeClosest(lineString, edges, projected));
  }

  /** Edges within the buffer beyond the closest distance are returned with it, those further away are not */
  @Test
  public void edgesWithinBufferOfClosestAreReturnedWithIt() {
    var point = PlanitJtsUtils.createPoint(5, 1);
    var edges = List.of(top, middle, bottom);

    /* bottom at 1, middle at 4, top at 19 */
    var withMiddle = PlanitGraphGeoUtils.findEdgesClosestToPoint(point, edges, 3.5, projected);
    assertSame(bottom, withMiddle.first());
    assertEquals(Set.of(middle), withMiddle.second());

    var closestOnly = PlanitGraphGeoUtils.findEdgesClosest(point, edges, 2, projected);
    assertSame(bottom, closestOnly.first());
    assertTrue(closestOnly.second().isEmpty());

    var lineString = PlanitJtsUtils.createLineString(new Coordinate(5, 30), new Coordinate(5, 6));
    /* middle at 1, bottom at 6, top at 10 */
    var withBottom = PlanitGraphGeoUtils.findEdgesClosestToLineString(lineString, edges, 5, projected);
    assertSame(middle, withBottom.first());
    assertEquals(Set.of(bottom), withBottom.second());
  }

  /** The distances of the edges within the buffer of the closest are reported, for a polygon taken to its ring */
  @Test
  public void edgesWithinClosestDistanceDeltaCarryTheirDistances() {
    var edges = List.of(top, middle, bottom);

    var toPoint = PlanitGraphGeoUtils.findEdgesWithinClosestDistanceDeltaToGeometry(
        PlanitJtsUtils.createPoint(5, 1), edges, 3.5, projected);
    assertEquals(2, toPoint.size());
    assertEquals(1, toPoint.get(bottom), PLANE_TOLERANCE);
    assertEquals(4, toPoint.get(middle), PLANE_TOLERANCE);

    /* a square from (4,11) to (6,13): 6 from the middle edge, 7 from the top one, 11 from the bottom one */
    var square = PlanitJtsUtils.create2DPolygon(new Envelope(4, 6, 11, 13));
    var toPolygon = PlanitGraphGeoUtils.findEdgesWithinClosestDistanceDeltaToGeometry(square, edges, 1.5, projected);
    assertEquals(2, toPolygon.size());
    assertEquals(6, toPolygon.get(middle), PLANE_TOLERANCE);
    assertEquals(7, toPolygon.get(top), PLANE_TOLERANCE);

    var closest = PlanitGraphGeoUtils.findEdgesClosestToGeometry(square, edges, 1.5, projected);
    assertSame(middle, closest.first());
    assertEquals(Set.of(top), closest.second());
  }

  /** Only points, line strings and polygons can be measured to */
  @Test
  public void edgesWithinClosestDistanceDeltaRejectUnsupportedGeometry() {
    var points = new GeometryFactory().createMultiPointFromCoords(
        new Coordinate[]{new Coordinate(1, 1), new Coordinate(2, 2)});
    assertThrows(PlanItRunTimeException.class, () -> PlanitGraphGeoUtils.findEdgesWithinClosestDistanceDeltaToGeometry(
        points, List.of(top, bottom), 1, projected));
  }

  /** A single edge is the closest without measuring, and nothing is found without a geometry or edges to search */
  @Test
  public void closestEdgeOfSingleOrMissingInput() {
    var farAway = PlanitJtsUtils.createPoint(500, 500);
    assertSame(top, PlanitGraphGeoUtils.findEdgeClosest(farAway, List.of(top), projected));
    assertNull(PlanitGraphGeoUtils.findEdgeClosest(null, List.of(top, bottom), projected));
    assertNull(PlanitGraphGeoUtils.findEdgeClosest(farAway, null, projected));
  }

  // ---------------------------------------------------------------------------------------------------------------
  // closest line segment of an edge segment
  // ---------------------------------------------------------------------------------------------------------------

  /** The line segment of an edge's geometry closest to a reference geometry is returned in the direction of the edge
   * segment asked for, reversed for the segment travelling against the geometry */
  @Test
  public void closestLineSegmentFollowsTheEdgeSegmentDirection() {
    /*
     *   n6 ------+ (10,-10)      bent link n6-n7, its geometry running from n6 (0,-10) via (10,-10) to
     *            |               n7 (10,-20)
     *            n7
     */
    int n6 = nodeAt(0, -10);
    int n7 = nodeAt(10, -20);
    var bent = twoWay(n6, n7);
    bent.setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(0, -10), new Coordinate(10, -10), new Coordinate(10, -20)));
    var ab = segment(n6, n7);
    var ba = segment(n7, n6);

    var nearSecondPiece = PlanitJtsUtils.createPoint(12, -18);
    var abSegment = PlanitGraphGeoUtils.extractClosestLineSegmentTo(nearSecondPiece, ab, projected);
    assertEquals(new Coordinate(10, -10), abSegment.p0);
    assertEquals(new Coordinate(10, -20), abSegment.p1);
    var baSegment = PlanitGraphGeoUtils.extractClosestLineSegmentTo(nearSecondPiece, ba, projected);
    assertEquals(new Coordinate(10, -20), baSegment.p0);
    assertEquals(new Coordinate(10, -10), baSegment.p1);

    var nearFirstPiece = PlanitJtsUtils.createPoint(3, -12);
    var firstAb = PlanitGraphGeoUtils.extractClosestLineSegmentTo(nearFirstPiece, ab, projected);
    assertEquals(new Coordinate(0, -10), firstAb.p0);
    assertEquals(new Coordinate(10, -10), firstAb.p1);
  }

  /** Without a geometry on its edge there is no line segment to extract */
  @Test
  public void closestLineSegmentNeedsAnEdgeGeometry() {
    var noGeometry = twoWay(0, 3);
    assertThrows(PlanItRunTimeException.class, () -> PlanitGraphGeoUtils.extractClosestLineSegmentTo(
        PlanitJtsUtils.createPoint(1, 1), noGeometry.getLinkSegmentAb(), projected));
  }

  // ---------------------------------------------------------------------------------------------------------------
  // bounding area and hull
  // ---------------------------------------------------------------------------------------------------------------

  /** A vertex outside a bounding box is near it when within the given distance of its boundary */
  @Test
  public void vertexNearBoundingBox() {
    var box = new Envelope(-100, -90, -100, -90);
    var node = nodes.get(nodeAt(-88, -95));

    assertTrue(PlanitGraphGeoUtils.isVertexNearBoundingBox(node, box, 2, projected));
    assertFalse(PlanitGraphGeoUtils.isVertexNearBoundingBox(node, box, 1.5, projected));
  }

  /** The convex hull is the tightest convex outline around the positioned vertices, those without a position are
   * ignored, and without any position it is empty */
  @Test
  public void convexHullAroundPositionedVertices() {
    /* the six nodes of the three edges span the rectangle from (0,0) to (10,20); the inner ones lie on its sides */
    createNodes(1);
    var hull = PlanitGraphGeoUtils.createConvexHull(nodes);
    assertEquals(200, hull.getArea(), PLANE_TOLERANCE);
    assertTrue(hull.covers(PlanitJtsUtils.createPoint(5, 5)));
    assertFalse(hull.covers(PlanitJtsUtils.createPoint(11, 5)));

    var withoutPosition = nodes.get(nodes.size() - 1);
    assertTrue(PlanitGraphGeoUtils.createConvexHull(List.of(withoutPosition)).isEmpty());
  }

  // ---------------------------------------------------------------------------------------------------------------
  // distance along an edge segment
  // ---------------------------------------------------------------------------------------------------------------

  /** From a point on a link, each segment's distance runs to its own downstream vertex; from the upstream vertex it
   * is the whole link */
  @Test
  public void distanceToDownstreamVertexFollowsTheSegmentDirection() {
    /*
     *   A ---------- X ---------- B      link A-B with shape point X, one segment each way, in WGS84
     */
    var geographic = new PlanitJtsCrsUtils(PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
    var a = new Coordinate(151.2089, -33.87);
    var x = new Coordinate(151.2095, -33.87);
    var b = new Coordinate(151.2101, -33.8705);

    int nodeA = nodeAt(a.x, a.y);
    int nodeB = nodeAt(b.x, b.y);
    var link = twoWay(nodeA, nodeB);
    link.setGeometry(PlanitJtsUtils.createLineString(a, x, b));

    var pointX = PlanitJtsUtils.createPoint(x);
    assertEquals(geographic.getDistanceInMetres(x, b),
        PlanitGraphGeoUtils.getDistanceToDownstreamVertexInMetres(segment(nodeA, nodeB), pointX, geographic), 1e-6);
    assertEquals(geographic.getDistanceInMetres(x, a),
        PlanitGraphGeoUtils.getDistanceToDownstreamVertexInMetres(segment(nodeB, nodeA), pointX, geographic), 1e-6);
    assertEquals(geographic.getDistanceInKilometres(link.getGeometry()) * 1000,
        PlanitGraphGeoUtils.getDistanceToDownstreamVertexInMetres(
            segment(nodeA, nodeB), nodes.get(nodeA).getPosition(), geographic), 1e-6);
  }

  /** A position that is not one of the edge's coordinates cannot be measured from */
  @Test
  public void distanceToDownstreamVertexNeedsAPositionOnTheGeometry() {
    assertThrows(PlanItRunTimeException.class, () -> PlanitGraphGeoUtils.getDistanceToDownstreamVertexInMetres(
        bottom.getLinkSegmentAb(), PlanitJtsUtils.createPoint(5, 0), projected));
    assertEquals(10, PlanitGraphGeoUtils.getDistanceToDownstreamVertexInMetres(
        bottom.getLinkSegmentAb(), nodes.get(0).getPosition(), projected), PLANE_TOLERANCE);
  }
}
