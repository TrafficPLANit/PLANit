package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.unit.Unit;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.IntrazonalDistanceMethod;
import org.goplanit.zoning.zonetozone.OdDemandMatrix;
import org.goplanit.zoning.zonetozone.OdSkimBuilderSettings;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.goplanit.zoning.zonetozone.OdSkimMatrixUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The utilities of {@link OdSkimMatrixUtils}: the cells of a skim its search cannot reach inferred, a cell keeping the
 * skim's value where it has a path, the straight line times the detour factor where it has none, and the intrazonal
 * rule within a zone; and the skims of several modes created from the network, modes taking one skim sharing it. Zones
 * lie in a plane in metres, their points at their centroids unless they have a polygon; distances are in km, times in
 * hours.
 *
 * @author markr
 */
public class OdSkimMatrixUtilsTest extends LayerTestBase {

  /** tolerance on skim values, the skims holding single precision floating point numbers */
  private static final double TOLERANCE = 1e-5;

  /**
   * Builder settings on one thread that infer the cells the search cannot reach, otherwise the defaults
   *
   * @return the settings
   */
  private static OdSkimBuilderSettings inferringOnOneThread() {
    var settings = new OdSkimBuilderSettings();
    settings.setNumberOfThreads(1);
    settings.setInferUnreachableCells(true);
    return settings;
  }

  /** the network, for the skim built from it */
  private MacroscopicNetwork network;

  /** zoning of the zones */
  private Zoning zoning;

  /** car mode, on the layer */
  private Mode car;

  /** bus mode, on no layer */
  private Mode bus;

  /** taxi mode, on no layer unless a test puts it on one */
  private Mode taxi;

  /**
   * Register an OD zone with its centroid at a point
   *
   * @param x metres east
   * @param y metres north
   * @return the zone
   */
  private OdZone zoneAt(double x, double y) {
    var zone = zoning.getOdZones().getFactory().registerNew();
    zone.setXmlId(String.valueOf(zone.getId()));
    zone.getCentroid().setPosition(PlanitJtsUtils.createPoint(x, y));
    return zone;
  }

  /**
   * A distance skim of the zoning's OD zones without a value in any cell
   *
   * @return the skim
   */
  private OdSkimMatrix emptySkim() {
    return OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.DISTANCE);
  }

  /**
   * A car skim with the cells its search cannot reach inferred
   *
   * @param carSkim the car skim, inferred in place
   * @param settings to use
   * @return the car skim
   */
  private OdSkimMatrix infer(OdSkimMatrix carSkim, OdSkimBuilderSettings settings) {
    OdSkimMatrixUtils.inferUnreachableCells(carSkim, car, zoning, settings);
    return carSkim;
  }

  /** A network in metres with car and bus, and an empty zoning in the same crs, before each test */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    network = new MacroscopicNetwork(token);
    network.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    taxi = network.getModes().getFactory().registerNew(PredefinedModeType.TAXI);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car));
    nodes.clear();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
  }

  /** The car skim from A to B is 3 km, so the inferred car skim holds 3 km from A to B */
  @Test
  public void testFromTheSkim() {
    var a = zoneAt(0, 0);
    var b = zoneAt(1000, 0);
    var skim = emptySkim();
    skim.setValue(a, b, 3.0);
    assertEquals(3.0, infer(skim, new OdSkimBuilderSettings()).getValue(a, b), TOLERANCE);
  }

  /** A and C 3 km apart, the car skim without a path between them, a detour factor of 1.3: 3.9 km */
  @Test
  public void testNoPath() {
    var a = zoneAt(0, 0);
    var c = zoneAt(3000, 0);
    var carSkim = infer(emptySkim(), new OdSkimBuilderSettings());
    assertEquals(3.9, carSkim.getValue(a, c), TOLERANCE);
    assertEquals(3.9, carSkim.getValue(c, a), TOLERANCE);
  }

  /**
   * A's skim distances to other zones of 2, 4, 6 and 10 km, with 3 nearest zones and a factor of 0.5: a trip within A
   * is half the mean of 2, 4 and 6, 2 km; with only one zone reached it is half that zone's distance
   */
  @Test
  public void testNearestNeighbours() {
    var a = zoneAt(0, 0);
    var others = List.of(zoneAt(1000, 0), zoneAt(2000, 0), zoneAt(3000, 0), zoneAt(4000, 0));
    var lonely = zoneAt(9000, 0);
    var skim = emptySkim();
    double[] kilometres = {10, 2, 6, 4};
    for (int index = 0; index < others.size(); ++index) {
      skim.setValue(a, others.get(index), kilometres[index]);
    }
    skim.setValue(a, a, 0.5);
    skim.setValue(lonely, a, 7.0);

    var carSkim = infer(skim, new OdSkimBuilderSettings());
    assertEquals(2.0, carSkim.getValue(a, a), TOLERANCE);
    assertEquals(3.5, carSkim.getValue(lonely, lonely), TOLERANCE);
  }

  /**
   * A zone whose row of the skim has no path gets the area rule: 0.52 times the square root of its area, 1.04 km for
   * B's 4 square kilometres, and 0 for A without a polygon
   */
  @Test
  public void testNearestNeighboursWithoutAPathFallsBackToTheArea() {
    var a = zoneAt(0, 0);
    var b = zoneAt(1000, 0);
    b.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(0, 2000, -1000, 1000)));
    var carSkim = infer(emptySkim(), new OdSkimBuilderSettings());
    assertEquals(0.0, carSkim.getValue(a, a), TOLERANCE);
    assertEquals(1.04, carSkim.getValue(b, b), TOLERANCE);
  }

  /** By the area rule a zone of 4 square kilometres gets 1.04 km within it, and one without a polygon 0 km */
  @Test
  public void testArea() {
    var a = zoneAt(1000, 1000);
    a.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(0, 2000, 0, 2000)));
    var b = zoneAt(5000, 0);
    var skim = emptySkim();
    skim.setValue(a, b, 3.0);
    skim.setValue(b, a, 3.0);
    var settings = new OdSkimBuilderSettings();
    settings.setIntrazonalDistanceMethod(IntrazonalDistanceMethod.AREA);
    var carSkim = infer(skim, settings);
    assertEquals(1.04, carSkim.getValue(a, a), TOLERANCE);
    assertEquals(0.0, carSkim.getValue(b, b), TOLERANCE);
  }

  /**
   * In a time skim, with car's straight line speed at 30 km/h: A to C, 1 km apart in a straight line, is the 1.3 km of
   * the detour factor at 30 km/h; within A, half the 0.1 hours to its one neighbour B, 0.05 hours; within C, without a
   * path to another zone, the area rule's 1.04 km for its 4 square kilometres at 30 km/h
   */
  @Test
  public void testTimesInferred() {
    var a = zoneAt(0, 0);
    var b = zoneAt(3000, 0);
    var c = zoneAt(0, 1000);
    c.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(-1000, 1000, 0, 2000)));
    var skim = OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.TRAVEL_TIME);
    skim.setValue(a, b, 0.1);
    var settings = new OdSkimBuilderSettings();
    settings.setStraightLineSpeedKmhFor(car, 30);

    var carTimes = infer(skim, settings);
    assertEquals(1.3 / 30, carTimes.getValue(a, c), TOLERANCE);
    assertEquals(0.05, carTimes.getValue(a, a), TOLERANCE);
    assertEquals(1.04 / 30, carTimes.getValue(c, c), TOLERANCE);
  }

  /**
   * Cells inferred in the skim's own unit, with car's straight line speed at 30 km/h: A to C, 1 km apart in a straight
   * line, is 1,300 in a distance skim in metres and 2.6 in a time skim in minutes; within C, without a path to another
   * zone, the area rule's 1.04 km for its 4 square kilometres, 1,040 metres and 2.08 minutes
   */
  @Test
  public void testInferredInTheSkimsOwnUnit() {
    var a = zoneAt(0, 0);
    var c = zoneAt(0, 1000);
    c.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(-1000, 1000, 0, 2000)));
    var settings = new OdSkimBuilderSettings();
    settings.setStraightLineSpeedKmhFor(car, 30);

    var metres = infer(
        OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.DISTANCE, Unit.METER),
        settings);
    var minutes = infer(
        OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.TRAVEL_TIME, Unit.MINUTE),
        settings);
    assertEquals(1_300.0, metres.getValue(a, c), 1e-2);
    assertEquals(1_040.0, metres.getValue(c, c), 1e-2);
    assertEquals(2.6, minutes.getValue(a, c), TOLERANCE);
    assertEquals(2.08, minutes.getValue(c, c), TOLERANCE);
  }

  /** Skims in km are all in km, but not once one of them is in metres */
  @Test
  public void testAreInUnit() {
    zoneAt(0, 0);
    var inKm = emptySkim();
    var inMetres =
        OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.DISTANCE, Unit.METER);
    assertTrue(OdSkimMatrixUtils.areInUnit(List.of(inKm, inKm), Unit.KM));
    assertFalse(OdSkimMatrixUtils.areInUnit(List.of(inKm, inMetres), Unit.KM));
  }

  /** Times inferred for a mode without a straight line speed are refused */
  @Test
  public void testTimesWithoutAStraightLineSpeedRefused() {
    zoneAt(0, 0);
    var skim = OdSkimMatrixUtils.createSkimWithoutValues(zoning.getOdZones(), SkimSubOutputType.TRAVEL_TIME);
    assertThrows(PlanItRunTimeException.class, () -> infer(skim, new OdSkimBuilderSettings()));
  }

  /**
   * A skim built from the network has the cells its search cannot reach inferred, and bus, on no layer, gets one of
   * straight lines times the factor, 6.5 km
   *
   * <pre>
   *   A -- 0 ==== 2 km ==== 1 -- B
   * </pre>
   */
  @Test
  public void testSkimBuiltFromTheNetwork() {
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    createNodes(2);
    twoWay(0, 1, road).setLengthKm(2);
    var a = zoneAt(0, 0);
    var b = zoneAt(5000, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        a, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        b, nodes.get(1), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);

    var skims = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, inferringOnOneThread(), SkimSubOutputType.DISTANCE, List.of(car, bus));
    assertEquals(2.0, skims.get(car).getValue(a, b), TOLERANCE);
    assertEquals(1.3 * 5.0, skims.get(bus).getValue(a, b), TOLERANCE);
  }

  /**
   * Once the skims are built from the network, the network and zoning hold the same entities with the same ids, the
   * nodes are joined to nothing else, and a second build gives the same distances as the first
   *
   * <pre>
   *   A -- 0 ==== 1 km ==== 1 ==== 1 km ==== 2 -- B
   * </pre>
   */
  @Test
  public void testNetworkAndZoningUnchanged() {
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    createNodes(3);
    twoWay(0, 1, road).setLengthKm(1);
    twoWay(1, 2, road).setLengthKm(1);
    var a = zoneAt(0, 0);
    var b = zoneAt(5000, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        a, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        b, nodes.get(2), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    var nodeIds = IdAbleUtils.collectSortedIds(layer.getNodes().toCollection());
    var linkIds = IdAbleUtils.collectSortedIds(layer.getLinks().toCollection());
    var linkSegmentIds = IdAbleUtils.collectSortedIds(layer.getLinkSegments().toCollection());
    var zoneIds = IdAbleUtils.collectSortedIds(zoning.getOdZones().toCollection());
    var connectoidIds = IdAbleUtils.collectSortedIds(zoning.getOdConnectoids().toCollection());
    var edgesOfNodes = nodes.stream().map(n -> n.getEdges().size()).collect(Collectors.toList());

    var settings = inferringOnOneThread();
    var first = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, settings, SkimSubOutputType.DISTANCE, List.of(car)).get(car);
    var second = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, settings, SkimSubOutputType.DISTANCE, List.of(car)).get(car);

    assertEquals(first.getValue(a, b), second.getValue(a, b));
    assertEquals(2.0, second.getValue(a, b), TOLERANCE);
    assertEquals(nodeIds, IdAbleUtils.collectSortedIds(layer.getNodes().toCollection()));
    assertEquals(linkIds, IdAbleUtils.collectSortedIds(layer.getLinks().toCollection()));
    assertEquals(linkSegmentIds, IdAbleUtils.collectSortedIds(layer.getLinkSegments().toCollection()));
    assertEquals(zoneIds, IdAbleUtils.collectSortedIds(zoning.getOdZones().toCollection()));
    assertEquals(connectoidIds, IdAbleUtils.collectSortedIds(zoning.getOdConnectoids().toCollection()));
    assertEquals(edgesOfNodes, nodes.stream().map(n -> n.getEdges().size()).collect(Collectors.toList()));
    assertTrue(zoning.getVirtualNetwork().getLayer().getConnectoidSegments().isEmpty());
  }

  /**
   * Taxi may use the road but not the connectoids, which allow car only, so its own skim has no path and its trip from
   * A to B is the straight line times the factor, 6.5 km. With car as its proxy mode it takes car's skim, the same
   * matrix: 2 km, and within A half the 2 km to its one neighbour
   *
   * <pre>
   *   A -- car only -- 0 ==== 2 km ==== 1 -- car only -- B
   * </pre>
   */
  @Test
  public void testTaxiOnCarsSkimFromTheNetwork() {
    layer.registerSupportedModes(List.of(taxi));
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car, taxi));
    createNodes(2);
    twoWay(0, 1, road).setLengthKm(2);
    var a = zoneAt(0, 0);
    var b = zoneAt(5000, 0);
    for (var zoneAndNode : List.of(Map.entry(a, 0), Map.entry(b, 1))) {
      zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
          zoneAndNode.getKey(), nodes.get(zoneAndNode.getValue()), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0)
          .getAccessZoneEntry(zoneAndNode.getKey(), ZoneConnectoidType.ZONE_ACCESS_EGRESS)
          .addAllowedModes(List.of(car));
    }
    var settings = inferringOnOneThread();

    var ownSkims = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, settings, SkimSubOutputType.DISTANCE, List.of(car, taxi));
    assertEquals(1.3 * 5.0, ownSkims.get(taxi).getValue(a, b), TOLERANCE);

    settings.setProxyModeFor(taxi, car);
    var carsSkims = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, settings, SkimSubOutputType.DISTANCE, List.of(car, taxi));
    assertSame(carsSkims.get(car), carsSkims.get(taxi));
    assertEquals(2.0, carsSkims.get(taxi).getValue(a, b), TOLERANCE);
    assertEquals(1.0, carsSkims.get(taxi).getValue(a, a), TOLERANCE);
  }

  /**
   * With car trips from A to B only, the car skim is searched from A alone: A to B is the 2 km road, while B to A,
   * from a zone without trips, is the straight line times the factor, 6.5 km
   *
   * <pre>
   *   A -- 0 ==== 2 km ==== 1 -- B
   * </pre>
   */
  @Test
  public void testSearchedOnlyFromZonesWithTrips() {
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    createNodes(2);
    twoWay(0, 1, road).setLengthKm(2);
    var a = zoneAt(0, 0);
    var b = zoneAt(5000, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        a, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        b, nodes.get(1), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    var carTrips = new OdDemandMatrix(zoning.getOdZones());
    carTrips.setValue(a, b, 1.0);

    var carSkim = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, inferringOnOneThread(), SkimSubOutputType.DISTANCE, Map.of(car, carTrips)).get(car);
    assertEquals(2.0, carSkim.getValue(a, b), TOLERANCE);
    assertEquals(1.3 * 5.0, carSkim.getValue(b, a), TOLERANCE);
  }

  /**
   * A and B on two roads not joined, each with a connectoid allowing every mode, and car trips from A to B: A, the one
   * zone searched from, reaches no other zone although B has a connectoid for car too, so the zoning is not joined to
   * the network and the skims are refused
   *
   * <pre>
   *   A -- 0 ==== 1 km ==== 1
   *        2 ==== 1 km ==== 3 -- B
   * </pre>
   */
  @Test
  public void testNotOneZoneWithTripsReachesAnotherRefused() {
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    createNodes(4);
    twoWay(0, 1, road).setLengthKm(1);
    twoWay(2, 3, road).setLengthKm(1);
    var a = zoneAt(0, 0);
    var b = zoneAt(1000, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        a, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        b, nodes.get(3), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0);
    var carTrips = new OdDemandMatrix(zoning.getOdZones());
    carTrips.setValue(a, b, 1.0);

    assertThrows(PlanItRunTimeException.class, () -> OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, inferringOnOneThread(), SkimSubOutputType.DISTANCE, Map.of(car, carTrips)));
  }

  /**
   * Taxi may use the road but not the connectoids, which allow car only: with taxi trips from A to B and taxi on its
   * own skim, no zone has a connectoid for taxi, so the skim is made, the trip a straight line times the factor,
   * 6.5 km
   *
   * <pre>
   *   A -- car only -- 0 ==== 2 km ==== 1 -- car only -- B
   * </pre>
   */
  @Test
  public void testModeWithoutConnectoidsNotRefused() {
    layer.registerSupportedModes(List.of(taxi));
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car, taxi));
    createNodes(2);
    twoWay(0, 1, road).setLengthKm(2);
    var a = zoneAt(0, 0);
    var b = zoneAt(5000, 0);
    for (var zoneAndNode : List.of(Map.entry(a, 0), Map.entry(b, 1))) {
      zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
          zoneAndNode.getKey(), nodes.get(zoneAndNode.getValue()), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0)
          .getAccessZoneEntry(zoneAndNode.getKey(), ZoneConnectoidType.ZONE_ACCESS_EGRESS)
          .addAllowedModes(List.of(car));
    }
    var taxiTrips = new OdDemandMatrix(zoning.getOdZones());
    taxiTrips.setValue(a, b, 1.0);

    var taxiSkim = OdSkimMatrixUtils.createSkimsByMode(
        network, zoning, inferringOnOneThread(), SkimSubOutputType.DISTANCE, Map.of(taxi, taxiTrips)).get(taxi);
    assertEquals(1.3 * 5.0, taxiSkim.getValue(a, b), TOLERANCE);
  }
}
