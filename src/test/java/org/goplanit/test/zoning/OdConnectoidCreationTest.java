package org.goplanit.test.zoning;

import org.goplanit.converter.zoning.connectoid.OdConnectoidAttachment;
import org.goplanit.converter.zoning.connectoid.OdConnectoidCreationResult;
import org.goplanit.converter.zoning.connectoid.OdConnectoidCreationSettings;
import org.goplanit.converter.zoning.connectoid.OdConnectoidZoneOutcome;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creating OD connectoids for the zones of a zoning that have none, on small layers drawn in each test's comment. The
 * zone's point z lies at (0, 0) unless said otherwise, distances are in metres east (x) and north (y); {@code ==} is a
 * two way link, {@code ->} a one way link. A zone is a square centred on its point, so a zone of half side h has radius
 * h and, at the default factor, a preferred distance of h. Links are 50 km/h for car and walk unless said otherwise.
 *
 * @author markr
 */
public class OdConnectoidCreationTest extends LayerTestBase {

  /** tolerance on lengths in km */
  private static final double LENGTH_TOLERANCE_KM = 1e-9;

  /**
   * Give a link the straight geometry between its nodes
   *
   * @param link to give it to
   * @return the link
   */
  private static MacroscopicLink straight(MacroscopicLink link) {
    link.setGeometry(PlanitJtsUtils.createLineString(
        link.getNodeA().getPosition().getCoordinate(), link.getNodeB().getPosition().getCoordinate()));
    return link;
  }

  /**
   * Settings for the given modes and number of connectoids per zone, otherwise the defaults
   *
   * @param numberPerZone connectoids per zone
   * @param modes to create them for
   * @return the settings
   */
  private static OdConnectoidCreationSettings settings(int numberPerZone, Mode... modes) {
    var settings = new OdConnectoidCreationSettings();
    settings.setNumberOfConnectoidsPerZone(numberPerZone);
    settings.setModes(modes);
    return settings;
  }

  /** the network of the layer */
  private MacroscopicNetwork network;

  /** zoning the connectoids are created for */
  private Zoning zoning;

  /** car mode */
  private Mode car;

  /** walk mode */
  private Mode walk;

  /** 50 km/h for car and walk */
  private MacroscopicLinkSegmentType local;

  /** 50 km/h for car only */
  private MacroscopicLinkSegmentType carOnly;

  /** 5 km/h for walk only */
  private MacroscopicLinkSegmentType walkOnly;

  /** 80 km/h for car only, a motorway */
  private MacroscopicLinkSegmentType fast;

  /**
   * A network in metres with car and walk on one layer, its segment types, and an empty zoning
   */
  private void createNetworkAndZoning() {
    var token = IdGroupingToken.collectGlobalToken();
    network = new MacroscopicNetwork(token);
    network.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    walk = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, walk));
    local = layer.getLinkSegmentTypes().getFactory().registerNew("local", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(local, 50, List.of(car, walk));
    carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));
    walkOnly = layer.getLinkSegmentTypes().getFactory().registerNew("walkOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(walkOnly, 5, List.of(walk));
    fast = layer.getLinkSegmentTypes().getFactory().registerNew("fast", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(fast, 80, List.of(car));
    nodes.clear();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
  }

  /**
   * Register an OD zone, a square centred on its point
   *
   * @param x of its point
   * @param y of its point
   * @param halfSide of the square, its radius
   * @return the zone
   */
  private OdZone zoneAt(double x, double y, double halfSide) {
    var zone = zoning.getOdZones().getFactory().registerNew();
    zone.setXmlId(String.valueOf(zone.getId()));
    zone.getCentroid().setPosition(PlanitJtsUtils.createPoint(x, y));
    zone.setGeometry(PlanitJtsUtils.create2DPolygon(new Envelope(x - halfSide, x + halfSide, y - halfSide, y + halfSide)));
    return zone;
  }

  /**
   * Connect nodes a and b both ways with a straight geometry
   *
   * @param a index of node A
   * @param b index of node B
   * @param type of both segments
   * @return the link
   */
  private MacroscopicLink straightTwoWay(int a, int b, MacroscopicLinkSegmentType type) {
    return straight(twoWay(a, b, type));
  }

  /**
   * The OD connectoids of a zone, by id
   *
   * @param zone to collect them for
   * @return its connectoids
   */
  private List<OdConnectoid> connectoidsOf(OdZone zone) {
    return zoning.getOdConnectoids().stream().filter(c -> c.hasAccessZoneEntry(zone))
        .sorted(Comparator.comparingLong(OdConnectoid::getId)).collect(Collectors.toList());
  }

  /**
   * The nodes of the OD connectoids of a zone
   *
   * @param zone to collect them for
   * @return the nodes
   */
  private Set<Node> nodesOf(OdZone zone) {
    return connectoidsOf(zone).stream().map(c -> (Node) c.getReferenceVertex()).collect(Collectors.toSet());
  }

  /**
   * The modes a connectoid allows a zone
   *
   * @param connectoid to ask
   * @param zone of the entry
   * @return the modes
   */
  private Set<Mode> modesOf(Connectoid connectoid, OdZone zone) {
    return Set.copyOf(connectoid.getAllowedModesFrom(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS, List.of(car, walk)));
  }

  /**
   * Create the connectoids for the zoning
   *
   * @param settings to use
   * @return the result
   */
  private OdConnectoidCreationResult create(OdConnectoidCreationSettings settings) {
    return ZoningUtils.createOdConnectoids(network, zoning, settings);
  }

  /**
   * A description of every OD connectoid, by id: its zone, the position of its node, its modes and its length
   *
   * @return the descriptions
   */
  private List<String> describeConnectoids() {
    return zoning.getOdConnectoids().stream().sorted(Comparator.comparingLong(OdConnectoid::getId))
        .flatMap(c -> c.getAccessZoneStream().map(zone -> String.format("%s %s %s %.6f", zone.getXmlId(),
            ((Node) c.getReferenceVertex()).getPosition().getCoordinate(),
            modesOf(c, (OdZone) zone).stream().map(Mode::getXmlId).sorted().collect(Collectors.toList()),
            c.getLengthKm(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).orElseThrow())))
        .collect(Collectors.toList());
  }

  /**
   * Build the network and zoning of the reproducibility test
   *
   * <pre>
   *                      3 (0,250)
   *                      ||
   *   0 (-300,0) ======= z ======= 1 (100,0) == 2 (120,0)
   *                      ||
   *                      4 (0,-400)              z2 at (500,500)
   * </pre>
   */
  private void buildTwoZones() {
    node(-300, 0);
    node(100, 0);
    node(120, 0);
    node(0, 250);
    node(0, -400);
    straightTwoWay(0, 1, local);
    straightTwoWay(1, 2, local);
    straightTwoWay(3, 1, local);
    straightTwoWay(4, 0, local);
    zoneAt(0, 0, 450);
    zoneAt(500, 500, 600);
  }

  /** A network with car and walk on one layer and an empty zoning, before each test */
  @BeforeEach
  public void setUp() {
    createNetworkAndZoning();
  }

  /**
   * Two asked for, for car and walk, at nodes both can use: two connectoids, each allowing both
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0)
   * </pre>
   */
  @Test
  public void testSharedByCarAndWalk() {
    node(-300, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    create(settings(2, car, walk));
    var connectoids = connectoidsOf(zone);
    assertEquals(2, connectoids.size());
    connectoids.forEach(c -> assertEquals(Set.of(car, walk), modesOf(c, zone)));
  }

  /**
   * One asked for, for car and walk; the nearest node only car can use, the next only walk: one connectoid each
   *
   * <pre>
   *                   3 (0,600)
   *                   || walk
   *                   2 (0,200)
   *
   *   0 (-500,0) ==== z ==== 1 (100,0)        car
   * </pre>
   */
  @Test
  public void testEveryModeCovered() {
    node(-500, 0);
    node(100, 0);
    node(0, 200);
    node(0, 600);
    straightTwoWay(0, 1, carOnly);
    straightTwoWay(2, 3, walkOnly);
    var zone = zoneAt(0, 0, 400);
    create(settings(1, car, walk));
    var connectoids = connectoidsOf(zone);
    assertEquals(2, connectoids.size());
    assertEquals(nodes.get(1), connectoids.get(0).getReferenceVertex());
    assertEquals(Set.of(car), modesOf(connectoids.get(0), zone));
    assertEquals(nodes.get(2), connectoids.get(1).getReferenceVertex());
    assertEquals(Set.of(walk), modesOf(connectoids.get(1), zone));
  }

  /**
   * Two asked for, nodes 100 m and 120 m east and 300 m west, all within the preferred distance of 400 m: one at the
   * node 100 m east, the second at the node 300 m west, as the node 120 m east lies in the direction already served
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0) == 2 (120,0)
   * </pre>
   */
  @Test
  public void testTwoOnOppositeSides() {
    node(-300, 0);
    node(100, 0);
    node(120, 0);
    straightTwoWay(0, 1, local);
    straightTwoWay(1, 2, local);
    var zone = zoneAt(0, 0, 400);
    create(settings(2, car, walk));
    assertEquals(Set.of(nodes.get(1), nodes.get(0)), nodesOf(zone));
  }

  /**
   * Two asked for, a preferred distance of 500 m, and the only node in the second direction 900 m away: one
   * connectoid
   *
   * <pre>
   *   0 (-900,0) ================== z == 1 (100,0)
   * </pre>
   */
  @Test
  public void testSecondDroppedBeyondPreferredDistance() {
    node(-900, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 500);
    var result = create(settings(2, car, walk));
    assertEquals(Set.of(nodes.get(1)), nodesOf(zone));
    assertEquals(1, result.getNumberOfZones(OdConnectoidZoneOutcome.FEWER_THAN_ASKED_FOR));
  }

  /**
   * The nearest node 900 m away, a preferred distance of 500 m and a maximum of 2,000 m: one connectoid, at that node
   *
   * <pre>
   *   0 (-1200,0) ========================== z ================== 1 (900,0)
   * </pre>
   */
  @Test
  public void testOneWithinMaximumDistance() {
    node(-1200, 0);
    node(900, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 500);
    var settings = settings(2, car, walk);
    settings.setMaximumDistanceMetres(2000);
    var result = create(settings);
    assertEquals(Set.of(nodes.get(1)), nodesOf(zone));
    assertEquals(1, result.getNumberOfZones(OdConnectoidZoneOutcome.NONE_WITHIN_PREFERRED_DISTANCE));
  }

  /**
   * The nearest node 3 km away and a maximum of 2 km: no connectoid, the zone counted among those without one
   *
   * <pre>
   *   z  . . . . . . . . . . . . 0 (3000,0) ==== 1 (4000,0)
   * </pre>
   */
  @Test
  public void testNoneBeyondMaximumDistance() {
    node(3000, 0);
    node(4000, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 500);
    var settings = settings(1, car, walk);
    settings.setMaximumDistanceMetres(2000);
    var result = create(settings);
    assertTrue(connectoidsOf(zone).isEmpty());
    assertEquals(1, result.getNumberOfZones(OdConnectoidZoneOutcome.NONE_FOR_A_MODE));
    assertEquals(Set.of(zone), Set.copyOf(result.getZonesWithoutConnectoid(car)));
    assertEquals(Set.of(zone), Set.copyOf(result.getZonesWithoutConnectoid(walk)));
    assertEquals(0, result.getNumberOfConnectoidsCreated());
  }

  /**
   * The zone's point 100 m from the middle of a 1 km link, links may be broken: the link is broken at a new node at
   * (0,100), and the connectoid goes there, 0.1 km long
   *
   * <pre>
   *   0 (-500,100) ========= x ========= 1 (500,100)
   *
   *                          z
   * </pre>
   */
  @Test
  public void testLinkBrokenAtNewNode() {
    node(-500, 100);
    node(500, 100);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    var settings = settings(1, car, walk);
    settings.setAttachment(OdConnectoidAttachment.NEW_NODE_ON_LINK);
    int linksBefore = layer.getLinks().size();
    var result = create(settings);
    var connectoids = connectoidsOf(zone);
    assertEquals(1, connectoids.size());
    var node = (Node) connectoids.get(0).getReferenceVertex();
    assertTrue(node.isPositionEqual2D(new Coordinate(0, 100), Precision.EPSILON_6));
    assertEquals(linksBefore + 1, layer.getLinks().size());
    assertEquals(1, result.getNumberOfLinksBroken());
    assertEquals(0.1, connectoids.get(0).getLengthKm(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).orElseThrow(),
        LENGTH_TOLERANCE_KM);
  }

  /**
   * The closest point of the link 5 m from its node 0, a minimum distance of 25 m (0.1 times a preferred 250 m): the
   * connectoid goes to node 0 and no link is broken
   *
   * <pre>
   *   0 (-5,100) x ======================== 1 (995,100)
   *
   *              z
   * </pre>
   */
  @Test
  public void testCloseToNodeUsesNode() {
    node(-5, 100);
    node(995, 100);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 250);
    var settings = settings(1, car, walk);
    settings.setAttachment(OdConnectoidAttachment.NEW_NODE_ON_LINK);
    int linksBefore = layer.getLinks().size();
    var result = create(settings);
    assertEquals(Set.of(nodes.get(0)), nodesOf(zone));
    assertEquals(linksBefore, layer.getLinks().size());
    assertEquals(0, result.getNumberOfLinksBroken());
  }

  /**
   * Links may not be broken: none is, and the connectoid goes to the nearest node, node 0 by the lower id as both lie
   * 510 m away
   *
   * <pre>
   *   0 (-500,100) ===================== 1 (500,100)
   *
   *                          z
   * </pre>
   */
  @Test
  public void testExistingNodesOnly() {
    node(-500, 100);
    node(500, 100);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    int linksBefore = layer.getLinks().size();
    var result = create(settings(1, car, walk));
    assertEquals(Set.of(nodes.get(0)), nodesOf(zone));
    assertEquals(linksBefore, layer.getLinks().size());
    assertEquals(0, result.getNumberOfLinksBroken());
  }

  /**
   * The nearest nodes lie on an 80 km/h motorway and the maximum speed is 60 km/h: the car connectoid goes to the
   * nearest node on a slower road, node 2
   *
   * <pre>
   *   0 (-100,50) ==80== 1 (100,50)
   *     ||80              ||80
   *     ||        z       ||
   *     ||                ||
   *   2 (-150,-200) ===== 3 (300,-200)
   * </pre>
   */
  @Test
  public void testMotorwayNodeLeftOut() {
    node(-100, 50);
    node(100, 50);
    node(-150, -200);
    node(300, -200);
    straightTwoWay(0, 1, fast);
    straightTwoWay(0, 2, fast);
    straightTwoWay(1, 3, fast);
    straightTwoWay(2, 3, carOnly);
    var zone = zoneAt(0, 0, 400);
    create(settings(1, car));
    assertEquals(Set.of(nodes.get(2)), nodesOf(zone));
  }

  /**
   * The nearest node can only be left by car, never entered: it is not used for car, even outside the main network
   * check
   *
   * <pre>
   *   2 (-400,0) ========== z  0 (50,0) --&gt; 1 (200,0)
   *       ||                                  ||
   *       ==================================== (2 to 1)
   * </pre>
   */
  @Test
  public void testNodeOnlyLeftNotUsed() {
    node(50, 0);
    node(200, 0);
    node(-400, 0);
    straight(link(0, 1, carOnly, null));
    straightTwoWay(1, 2, carOnly);
    var zone = zoneAt(0, 0, 500);
    var settings = settings(1, car);
    settings.setMainNetworkOnly(false);
    create(settings);
    assertEquals(Set.of(nodes.get(1)), nodesOf(zone));
  }

  /**
   * The nearest nodes lie on an island, not connected to the rest: they are not used
   *
   * <pre>
   *               z  0 (50,0) == 1 (80,0)      island
   *
   *   3 (-400,0) ====== 2 (200,0)              main
   *       ||
   *   4 (0,-500)
   * </pre>
   */
  @Test
  public void testIslandLeftOut() {
    node(50, 0);
    node(80, 0);
    node(200, 0);
    node(-400, 0);
    node(0, -500);
    straightTwoWay(0, 1, carOnly);
    straightTwoWay(2, 3, carOnly);
    straightTwoWay(3, 4, carOnly);
    var zone = zoneAt(0, 0, 500);
    create(settings(1, car));
    assertEquals(Set.of(nodes.get(2)), nodesOf(zone));
  }

  /**
   * A connectoid at a node 340 m from the zone's point is 0.34 km long
   *
   * <pre>
   *   z ------------ 0 (340,0) ==== 1 (1000,0)
   * </pre>
   */
  @Test
  public void testLengthIsStraightLineToNode() {
    node(340, 0);
    node(1000, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    create(settings(1, car, walk));
    var connectoids = connectoidsOf(zone);
    assertEquals(1, connectoids.size());
    assertEquals(0.34, connectoids.get(0).getLengthKm(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).orElseThrow(),
        LENGTH_TOLERANCE_KM);
  }

  /**
   * A zone with an OD connectoid at node 0 keeps it and gets no other, as replacing is not asked for
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0)
   * </pre>
   */
  @Test
  public void testZoneWithConnectoidKept() {
    node(-300, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        zone, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0.3);
    var result = create(settings(2, car, walk));
    assertEquals(Set.of(nodes.get(0)), nodesOf(zone));
    assertEquals(1, zoning.getOdConnectoids().size());
    assertEquals(1, result.getNumberOfZones(OdConnectoidZoneOutcome.KEPT_EXISTING));
  }

  /**
   * A zone with an OD connectoid at node 0, replacing asked for: it gets the nearest node instead, node 1
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0)
   * </pre>
   */
  @Test
  public void testZoneWithConnectoidReplacedWhenAsked() {
    node(-300, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        zone, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0.3);
    var settings = settings(1, car, walk);
    settings.setReplaceExistingConnectoids(true);
    create(settings);
    assertEquals(Set.of(nodes.get(1)), nodesOf(zone));
    assertEquals(1, zoning.getOdConnectoids().size());
  }

  /**
   * A zone whose only OD connectoid, at node 0, allows walk keeps it when car and walk are asked for without replacing,
   * and gets none for car: it is reported without a connectoid for car, as a zone that kept its own, and not for walk
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0)
   * </pre>
   */
  @Test
  public void testKeptZoneWithoutAnAskedModeReported() {
    node(-300, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        zone, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0.3)
        .getAccessZoneEntry(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).addAllowedModes(List.of(walk));
    var result = create(settings(1, car, walk));
    assertEquals(1, zoning.getOdConnectoids().size());
    assertEquals(1, result.getNumberOfZones(OdConnectoidZoneOutcome.KEPT_EXISTING));
    assertEquals(List.of(zone), result.getZonesWithoutConnectoid(car));
    assertEquals(List.of(zone), result.getZonesWithoutConnectoid(car, OdConnectoidZoneOutcome.KEPT_EXISTING));
    assertTrue(result.getZonesWithoutConnectoid(walk).isEmpty());
  }

  /**
   * Replacing with car asked for removes the zone's walk only connectoid too and makes one for car, so walk is reported
   * as left without OD connectoids by replacing; replacing that car connectoid again, car asked for, reports none
   *
   * <pre>
   *   0 (-300,0) ========= z ==== 1 (100,0)
   * </pre>
   */
  @Test
  public void testReplacingReportsModesNotAskedFor() {
    node(-300, 0);
    node(100, 0);
    straightTwoWay(0, 1, local);
    var zone = zoneAt(0, 0, 400);
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
        zone, nodes.get(0), ZoneConnectoidType.ZONE_ACCESS_EGRESS, 0.3)
        .getAccessZoneEntry(zone, ZoneConnectoidType.ZONE_ACCESS_EGRESS).addAllowedModes(List.of(walk));
    var settings = settings(1, car);
    settings.setReplaceExistingConnectoids(true);

    var result = create(settings);
    assertEquals(Set.of(walk), result.getModesLeftWithoutConnectoidsByReplacing());
    assertEquals(1, connectoidsOf(zone).size());
    assertEquals(Set.of(car), modesOf(connectoidsOf(zone).get(0), zone));

    assertTrue(create(settings).getModesLeftWithoutConnectoidsByReplacing().isEmpty());
  }

  /**
   * The same zoning, network and settings give the same connectoids, built twice from scratch, see
   * {@link #buildTwoZones()}
   */
  @Test
  public void testSameConnectoidsTwice() {
    buildTwoZones();
    create(settings(3, car, walk));
    var first = describeConnectoids();
    IdGenerator.reset();
    createNetworkAndZoning();
    buildTwoZones();
    create(settings(3, car, walk));
    assertTrue(first.size() > 2);
    assertEquals(first, describeConnectoids());
  }
}
