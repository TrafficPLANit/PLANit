package org.goplanit.test.zoning;

import org.goplanit.cost.physical.initial.InitialMacroscopicLinkSegmentCost;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.transport.TransportModelNetworkImpl;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimBuilder;
import org.goplanit.zoning.zonetozone.OdSkimBuilderSettings;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Building zone to zone skims of the distance and time along least-cost paths, on small layers drawn in each test's
 * comment, each built on the network joined to the zoning and the join undone after. {@code ==} is a two way road
 * with its length and speed, {@code --} an OD connectoid with its length; A, B and C are zones, the numbers nodes.
 * Distances are in km, times in hours, as the skims hold them.
 *
 * @author markr
 */
public class OdSkimBuilderTest extends LayerTestBase {

  /** tolerance on skim values, which are held as single precision floating point numbers */
  private static final double TOLERANCE = 1e-5;

  /**
   * Settings on the given number of threads, otherwise the defaults
   *
   * @param threads to run on
   * @return the settings
   */
  private static OdSkimBuilderSettings settings(int threads) {
    var settings = new OdSkimBuilderSettings();
    settings.setNumberOfThreads(threads);
    return settings;
  }

  /** the network of the layer */
  private MacroscopicNetwork network;

  /** zoning skimmed */
  private Zoning zoning;

  /** car mode */
  private Mode car;

  /** bus mode */
  private Mode bus;

  /** walk mode */
  private Mode walk;

  /** 20 km/h for car and bus */
  private MacroscopicLinkSegmentType slow;

  /** 60 km/h for car and bus */
  private MacroscopicLinkSegmentType quick;

  /** 60 km/h for bus only */
  private MacroscopicLinkSegmentType quickBusOnly;

  /**
   * Register an OD zone
   *
   * @param xmlId of the zone
   * @return the zone
   */
  private OdZone zone(String xmlId) {
    var zone = zoning.getOdZones().getFactory().registerNew();
    zone.setXmlId(xmlId);
    return zone;
  }

  /**
   * Register an OD zone with its centroid at a point, for the straight lines of inferred cells
   *
   * @param xmlId of the zone
   * @param x metres east
   * @param y metres north
   * @return the zone
   */
  private OdZone zoneAt(String xmlId, double x, double y) {
    var zone = zone(xmlId);
    zone.getCentroid().setPosition(PlanitJtsUtils.createPoint(x, y));
    return zone;
  }

  /**
   * Register an OD connectoid of a zone at a node
   *
   * @param zone of the connectoid
   * @param node index of its node
   * @param lengthKm of the connectoid
   * @param modes it allows, none meaning every mode
   */
  private void connect(OdZone zone, int node, double lengthKm, Mode... modes) {
    var connectoid = zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        zone, nodes.get(node), ZoneConnectoidType.ZONE_ACCESS_EGRESS, lengthKm);
    if (modes.length > 0) {
      connectoid.getAccessZoneEntry(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).addAllowedModes(List.of(modes));
    }
  }

  /**
   * A two way road between two nodes
   *
   * @param a index of node A
   * @param b index of node B
   * @param lengthKm of the road
   * @param type of both segments
   * @return the link
   */
  private MacroscopicLink road(int a, int b, double lengthKm, MacroscopicLinkSegmentType type) {
    var link = twoWay(a, b, type);
    link.setLengthKm(lengthKm);
    return link;
  }

  /**
   * Skim the zoning with a builder on the network joined to the zoning through its connectoids, the join undone after
   *
   * @param settings of the builder
   * @param build builds the skims with the builder
   * @param skimTypes to build
   * @return the skims by type
   */
  private Map<SkimSubOutputType, OdSkimMatrix> skim(OdSkimBuilderSettings settings,
      Function<OdSkimBuilder, Map<SkimSubOutputType, OdSkimMatrix>> build, SkimSubOutputType... skimTypes) {
    var transportNetwork = new TransportModelNetworkImpl(network, zoning);
    transportNetwork.integrateTransportNetworkViaConnectoids(true);
    try {
      return build.apply(new OdSkimBuilder(transportNetwork, settings, skimTypes));
    } finally {
      transportNetwork.removeVirtualNetworkFromPhysicalNetwork(false);
    }
  }

  /**
   * Skim the zoning for a mode on free-flow times, the distance and the time, on one thread
   *
   * @param mode to skim for
   * @return the skims by type
   */
  private Map<SkimSubOutputType, OdSkimMatrix> skim(Mode mode) {
    return skim(settings(1), builder -> builder.build(mode),
        SkimSubOutputType.DISTANCE, SkimSubOutputType.TRAVEL_TIME);
  }

  /**
   * The two roads of the first scenarios, A at node 0 and B at node 2, both connectoids of 0 km
   *
   * <pre>
   *            1
   *     1 km  //  \\  1 km, both 20 km/h
   *  A -- 0          2 -- B
   *   1.5 km  \\  //  1.5 km, both 60 km/h of the given type
   *            3
   * </pre>
   *
   * @param threeKmType type of the 3 km road
   * @return zones A and B
   */
  private List<OdZone> twoRoads(MacroscopicLinkSegmentType threeKmType) {
    createNodes(4);
    road(0, 1, 1, slow);
    road(1, 2, 1, slow);
    road(0, 3, 1.5, threeKmType);
    road(3, 2, 1.5, threeKmType);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0);
    connect(b, 2, 0);
    return List.of(a, b);
  }

  /** A network with car, bus and walk on one layer, its segment types, and an empty zoning, before each test */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    network = new MacroscopicNetwork(token);
    network.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    walk = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, bus, walk));
    slow = layer.getLinkSegmentTypes().getFactory().registerNew("slow", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(slow, 20, List.of(car, bus));
    quick = layer.getLinkSegmentTypes().getFactory().registerNew("quick", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(quick, 60, List.of(car, bus));
    quickBusOnly = layer.getLinkSegmentTypes().getFactory().registerNew("quickBusOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(quickBusOnly, 60, List.of(bus));
    nodes.clear();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
  }

  /** The 3 km road at 60 km/h is faster than the 2 km road at 20 km/h: 3 km and 3 minutes */
  @Test
  public void testFasterPathOverShorter() {
    var zones = twoRoads(quick);
    var skims = skim(car);
    assertEquals(3.0, skims.get(SkimSubOutputType.DISTANCE).getValue(zones.get(0), zones.get(1)), TOLERANCE);
    assertEquals(3.0 / 60, skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(zones.get(0), zones.get(1)), TOLERANCE);
  }

  /** Given costs of 10 minutes on the 3 km road and 6 minutes on the 2 km road: 2 km and 6 minutes */
  @Test
  public void testCostsGiven() {
    var zones = twoRoads(quick);
    var costs = new InitialMacroscopicLinkSegmentCost(IdGroupingToken.collectGlobalToken());
    costs.setSegmentCost(car, segment(0, 3), 5.0 / 60);
    costs.setSegmentCost(car, segment(3, 2), 5.0 / 60);
    costs.setSegmentCost(car, segment(0, 1), 3.0 / 60);
    costs.setSegmentCost(car, segment(1, 2), 3.0 / 60);
    var skims = skim(settings(1), builder -> builder.build(car, costs),
        SkimSubOutputType.DISTANCE, SkimSubOutputType.TRAVEL_TIME);
    assertEquals(2.0, skims.get(SkimSubOutputType.DISTANCE).getValue(zones.get(0), zones.get(1)), TOLERANCE);
    assertEquals(6.0 / 60, skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(zones.get(0), zones.get(1)), TOLERANCE);
  }

  /**
   * Asked for the distance only, the distance skim alone is built, 3 km; asked for the time only, the time skim alone,
   * 3 minutes
   */
  @Test
  public void testOnlyTheSkimTypesGiven() {
    var zones = twoRoads(quick);
    var distanceOnly = skim(settings(1), builder -> builder.build(car), SkimSubOutputType.DISTANCE);
    var timeOnly = skim(settings(1), builder -> builder.build(car), SkimSubOutputType.TRAVEL_TIME);
    assertEquals(Set.of(SkimSubOutputType.DISTANCE), distanceOnly.keySet());
    assertEquals(3.0, distanceOnly.get(SkimSubOutputType.DISTANCE).getValue(zones.get(0), zones.get(1)), TOLERANCE);
    assertEquals(Set.of(SkimSubOutputType.TRAVEL_TIME), timeOnly.keySet());
    assertEquals(3.0 / 60, timeOnly.get(SkimSubOutputType.TRAVEL_TIME).getValue(zones.get(0), zones.get(1)),
        TOLERANCE);
  }

  /** A skim of cost or of none, among others or alone, and no skim type at all, are refused */
  @Test
  public void testSkimTypesRefused() {
    twoRoads(quick);
    for (var skimTypes : List.of(new SkimSubOutputType[] {SkimSubOutputType.COST},
        new SkimSubOutputType[] {SkimSubOutputType.NONE},
        new SkimSubOutputType[] {SkimSubOutputType.DISTANCE, SkimSubOutputType.COST}, new SkimSubOutputType[0])) {
      assertThrows(PlanItRunTimeException.class, () -> skim(settings(1), builder -> builder.build(car), skimTypes));
    }
  }

  /** A network the zoning is not joined to through its connectoids is refused */
  @Test
  public void testNotJoinedRefused() {
    twoRoads(quick);
    assertThrows(PlanItRunTimeException.class, () -> new OdSkimBuilder(
        new TransportModelNetworkImpl(network, zoning), settings(1), SkimSubOutputType.DISTANCE));
  }

  /** The 3 km road is for bus only, so the car skim takes the 2 km road */
  @Test
  public void testLinkSegmentTheModeMayNotUse() {
    var zones = twoRoads(quickBusOnly);
    assertEquals(2.0, skim(car).get(SkimSubOutputType.DISTANCE).getValue(zones.get(0), zones.get(1)), TOLERANCE);
    assertEquals(3.0, skim(bus).get(SkimSubOutputType.DISTANCE).getValue(zones.get(0), zones.get(1)), TOLERANCE);
  }

  /**
   * Both connectoids' lengths count: 2.35 km
   *
   * <pre>
   *   A -- 0.25 km -- 0 ==== 2 km, 60 km/h ==== 1 -- 0.1 km -- B
   * </pre>
   */
  @Test
  public void testConnectoidLengthCounted() {
    createNodes(2);
    road(0, 1, 2, quick);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0.25);
    connect(b, 1, 0.1);
    var skims = skim(car);
    assertEquals(2.35, skims.get(SkimSubOutputType.DISTANCE).getValue(a, b), TOLERANCE);
    assertEquals(2.35, skims.get(SkimSubOutputType.DISTANCE).getValue(b, a), TOLERANCE);
    assertEquals(0.35 / 25 + 2.0 / 60, skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(a, b), TOLERANCE);
  }

  /**
   * Node 1 is half a minute nearer B than node 0, but its connectoid is 400 m longer, 0.96 minutes at 25 km/h: the
   * skim leaves through node 0, 1.2 km
   *
   * <pre>
   *   A -- 0.2 km -- 0 ==== 1 km, 60 km/h ==== 2 -- 0 km -- B
   *   A -- 0.6 km -- 1 == 0.5 km, 60 km/h ==== 2
   * </pre>
   */
  @Test
  public void testConnectoidSpeedDecidesTheWayIn() {
    createNodes(3);
    road(0, 2, 1, quick);
    road(1, 2, 0.5, quick);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0.2);
    connect(a, 1, 0.6);
    connect(b, 2, 0);
    var skims = skim(car);
    assertEquals(1.2, skims.get(SkimSubOutputType.DISTANCE).getValue(a, b), TOLERANCE);
    assertEquals(0.2 / 25 + 1.0 / 60, skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(a, b), TOLERANCE);
  }

  /**
   * A's only connectoid allows walk, so the car skim has no path from or to A
   *
   * <pre>
   *   A -- walk only -- 0 ==== 2 km, 60 km/h ==== 1 -- B
   * </pre>
   */
  @Test
  public void testConnectoidForAnotherModeOnly() {
    createNodes(2);
    road(0, 1, 2, quick);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0, walk);
    connect(b, 1, 0);
    var skims = skim(car);
    assertTrue(Double.isNaN(skims.get(SkimSubOutputType.DISTANCE).getValue(a, b)));
    assertTrue(Double.isNaN(skims.get(SkimSubOutputType.DISTANCE).getValue(b, a)));
    assertTrue(Double.isNaN(skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(a, b)));
  }

  /**
   * A stop's transfer zone has car connectoids of 100 m at both ends of the 5 km road, 0.48 minutes through the stop
   * against 5 minutes by road: the car skim from A to B still takes the road, 5 km
   *
   * <pre>
   *              stop
   *     0.1 km --    -- 0.1 km
   *   A -- 0 ==== 5 km, 60 km/h ==== 1 -- B
   * </pre>
   */
  @Test
  public void testNoPathThroughAStop() {
    createNodes(2);
    road(0, 1, 5, quick);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0);
    connect(b, 1, 0);
    var stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    for (var towardsNode : List.of(segment(1, 0), segment(0, 1))) {
      zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
          stop, true, towardsNode, 0.1, ZoneConnectoidType.ZONE_ACCESS_EGRESS)
          .getAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS_EGRESS).addAllowedModes(List.of(car));
    }
    var skims = skim(car);
    assertEquals(5.0, skims.get(SkimSubOutputType.DISTANCE).getValue(a, b), TOLERANCE);
    assertEquals(5.0 / 60, skims.get(SkimSubOutputType.TRAVEL_TIME).getValue(a, b), TOLERANCE);
  }

  /**
   * C lies on a part of the network not joined to the rest: no path from A to C or from C to A, never 0
   *
   * <pre>
   *   A -- 0 ==== 1 km ==== 1 -- B
   *   C -- 2 ==== 1 km ==== 3
   * </pre>
   */
  @Test
  public void testDisconnectedZone() {
    createNodes(4);
    road(0, 1, 1, quick);
    road(2, 3, 1, quick);
    var a = zone("A");
    var b = zone("B");
    var c = zone("C");
    connect(a, 0, 0);
    connect(b, 1, 0);
    connect(c, 2, 0);
    var distances = skim(car).get(SkimSubOutputType.DISTANCE);
    assertEquals(1.0, distances.getValue(a, b), TOLERANCE);
    assertTrue(Double.isNaN(distances.getValue(a, c)));
    assertTrue(Double.isNaN(distances.getValue(c, a)));
  }

  /**
   * A and B on two parts of the network not joined, each with a connectoid for car: not one zone searched from reaches
   * another, so the zoning is not joined to the network as it should be, and the build is refused
   *
   * <pre>
   *   A -- 0 ==== 1 km ==== 1
   *        2 ==== 1 km ==== 3 -- B
   * </pre>
   */
  @Test
  public void testNotOneZoneReachesAnotherRefused() {
    createNodes(4);
    road(0, 1, 1, quick);
    road(2, 3, 1, quick);
    connect(zone("A"), 0, 0);
    connect(zone("B"), 3, 0);
    assertThrows(PlanItRunTimeException.class, () -> skim(car));
  }

  /**
   * The cells the search cannot reach inferred, with car's straight line speed at 30 km/h, on a second network made in
   * the same process, whose modes' ids the join recreates: C lies on a part of the network not joined to the rest, 3 km
   * from A in a straight line, so A to C and C to A are 3.9 km, 0.13 hours; within A, half the 1 km and 1 minute to its
   * one neighbour B, 0.5 km and half a minute; within C, which reaches no other zone, the area rule, 0 as C has no
   * polygon
   *
   * <pre>
   *   C -- 2 ==== 1 km ==== 3
   *   |
   *   3 km in a straight line
   *   |
   *   A -- 0 ==== 1 km, 60 km/h ==== 1 -- B
   * </pre>
   */
  @Test
  public void testUnreachableCellsInferred() {
    setUp();
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    createNodes(4);
    road(0, 1, 1, quick);
    road(2, 3, 1, quick);
    var a = zoneAt("A", 0, 0);
    var b = zoneAt("B", 1000, 0);
    var c = zoneAt("C", 0, 3000);
    connect(a, 0, 0);
    connect(b, 1, 0);
    connect(c, 2, 0);
    var settings = settings(1);
    settings.setInferUnreachableCells(true);
    settings.setStraightLineSpeedKmhFor(car, 30);

    var skims = skim(settings, builder -> builder.build(car), SkimSubOutputType.DISTANCE,
        SkimSubOutputType.TRAVEL_TIME);
    var distances = skims.get(SkimSubOutputType.DISTANCE);
    var times = skims.get(SkimSubOutputType.TRAVEL_TIME);
    assertEquals(1.0, distances.getValue(a, b), TOLERANCE);
    assertEquals(3.9, distances.getValue(a, c), TOLERANCE);
    assertEquals(3.9, distances.getValue(c, a), TOLERANCE);
    assertEquals(3.9 / 30, times.getValue(a, c), TOLERANCE);
    assertEquals(0.5, distances.getValue(a, a), TOLERANCE);
    assertEquals(0.5 / 60, times.getValue(a, a), TOLERANCE);
    assertEquals(0.0, distances.getValue(c, c), TOLERANCE);
    assertEquals(0.0, times.getValue(c, c), TOLERANCE);
  }

  /** Times to be inferred for a mode without a straight line speed are refused */
  @Test
  public void testTimesInferredWithoutAStraightLineSpeedRefused() {
    twoRoads(quick);
    var settings = settings(1);
    settings.setInferUnreachableCells(true);
    assertThrows(PlanItRunTimeException.class,
        () -> skim(settings, builder -> builder.build(car), SkimSubOutputType.TRAVEL_TIME));
  }

  /**
   * Taxi has no layer, so its skim is refused, unless the cells the search cannot reach are inferred, when every cell
   * is: A to B, 2 km apart in a straight line, 2.6 km
   *
   * <pre>
   *   A -- 0 ==== 2 km, 60 km/h ==== 1 -- B
   * </pre>
   */
  @Test
  public void testModeWithoutALayer() {
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    var taxi = network.getModes().getFactory().registerNew(PredefinedModeType.TAXI);
    createNodes(2);
    road(0, 1, 2, quick);
    var a = zoneAt("A", 0, 0);
    var b = zoneAt("B", 0, 2000);
    connect(a, 0, 0);
    connect(b, 1, 0);
    assertThrows(PlanItRunTimeException.class,
        () -> skim(settings(1), builder -> builder.build(taxi), SkimSubOutputType.DISTANCE));

    var settings = settings(1);
    settings.setInferUnreachableCells(true);
    var distances = skim(settings, builder -> builder.build(taxi), SkimSubOutputType.DISTANCE)
        .get(SkimSubOutputType.DISTANCE);
    assertEquals(2.6, distances.getValue(a, b), TOLERANCE);
  }

  /**
   * Bus taking the skims of car, on a second network made in the same process, whose modes' ids the join recreates:
   * bus is skimmed as car, on the 2 km road, not on the 3 km road for bus only
   */
  @Test
  public void testProxyMode() {
    setUp();
    var zones = twoRoads(quickBusOnly);
    var settings = settings(1);
    settings.setProxyModeFor(bus, car);
    var distances = skim(settings, builder -> builder.build(bus), SkimSubOutputType.DISTANCE)
        .get(SkimSubOutputType.DISTANCE);
    assertEquals(2.0, distances.getValue(zones.get(0), zones.get(1)), TOLERANCE);
  }

  /**
   * A grid of 5 by 5 nodes with roads of 1, 1.5 and 2 km and zones at six nodes, skimmed on one thread and on eight:
   * every value the same
   *
   * <pre>
   *   20 == 21 == 22 == 23 == 24
   *   ||    ||    ||    ||    ||
   *   15 == 16 == 17 == 18 == 19
   *   ..                      ..
   *    0 ==  1 ==  2 ==  3 ==  4
   * </pre>
   */
  @Test
  public void testSameSkimOnOneAndEightThreads() {
    createNodes(25);
    int roads = 0;
    for (int row = 0; row < 5; ++row) {
      for (int column = 0; column < 5; ++column) {
        int node = row * 5 + column;
        if (column < 4) {
          road(node, node + 1, 1 + 0.5 * (roads++ % 3), roads % 2 == 0 ? quick : slow);
        }
        if (row < 4) {
          road(node, node + 5, 1 + 0.5 * (roads++ % 3), roads % 2 == 0 ? quick : slow);
        }
      }
    }
    int[] zoneNodes = {0, 4, 12, 17, 20, 24};
    var zones = new ArrayList<OdZone>();
    for (int node : zoneNodes) {
      var zone = zone("Z" + node);
      connect(zone, node, 0.1);
      zones.add(zone);
    }

    var onOne = skim(car);
    var onEight = skim(settings(Math.min(8, Math.max(1, Runtime.getRuntime().availableProcessors() - 1))),
        builder -> builder.build(car), SkimSubOutputType.DISTANCE, SkimSubOutputType.TRAVEL_TIME);
    for (var type : List.of(SkimSubOutputType.DISTANCE, SkimSubOutputType.TRAVEL_TIME)) {
      for (var from : zones) {
        for (var to : zones) {
          assertEquals(onOne.get(type).getValue(from, to), onEight.get(type).getValue(from, to));
        }
      }
    }
  }

  /**
   * A second network made in the same process, as when a network is read after another, its modes' ids following those
   * of the first, and connectoids allowing car only: the car skim finds the 1 km road, as on the first network
   *
   * <pre>
   *   A -- car only -- 0 ==== 1 km, 60 km/h ==== 1 -- car only -- B
   * </pre>
   */
  @Test
  public void testNetworkMadeAfterAnother() {
    setUp();
    createNodes(2);
    road(0, 1, 1, quick);
    var a = zone("A");
    var b = zone("B");
    connect(a, 0, 0, car);
    connect(b, 1, 0, car);
    assertEquals(1.0, skim(car).get(SkimSubOutputType.DISTANCE).getValue(a, b), TOLERANCE);
  }
}
