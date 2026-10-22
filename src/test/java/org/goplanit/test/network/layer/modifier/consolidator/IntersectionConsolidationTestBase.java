package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base for tests consolidating the intersections of a layer through the layer modifier, on small layers built by hand.
 * Positions are in metres on a flat plane, x east and y north, and link lengths are given in metres. The layer has cars
 * and pedestrians, a road type for cars and a footway type for pedestrians
 *
 * @author markr
 */
public abstract class IntersectionConsolidationTestBase extends LayerTestBase {

  protected MacroscopicNetwork network;
  protected IntersectionsImpl intersections;
  protected Mode car;
  protected Mode pedestrian;
  protected MacroscopicLinkSegmentType road;
  protected MacroscopicLinkSegmentType footway;

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, pedestrian));
    intersections = (IntersectionsImpl) layer.getIntersections();

    road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car));
    footway = layer.getLinkSegmentTypes().getFactory().registerNew("footway", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(footway, 5, List.of(pedestrian));
  }

  /**
   * Register a node at a position
   *
   * @param x metres east
   * @param y metres north
   * @return index of the node
   */
  protected int node(double x, double y) {
    var node = layer.getNodes().getFactory().registerNew();
    node.setPosition(PlanitJtsUtils.createPoint(x, y));
    nodes.add(node);
    return nodes.size() - 1;
  }

  /**
   * Join two nodes with a link of the given length and type, one way from a to b or in both directions
   *
   * @param a index of node A
   * @param b index of node B
   * @param meters length of the link
   * @param twoWay true for a segment in each direction, false for a to b only
   * @param type of the segments
   * @return the link
   */
  protected MacroscopicLink link(int a, int b, double meters, boolean twoWay, MacroscopicLinkSegmentType type) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), meters / 1000, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true).setLinkSegmentType(type);
    if (twoWay) {
      layer.getLinkSegments().getFactory().registerNew(link, false, true).setLinkSegmentType(type);
    }
    return link;
  }

  /** a road from a to b only */
  protected MacroscopicLink oneWayRoad(int a, int b, double meters) {
    return link(a, b, meters, false, road);
  }

  /** a road in both directions */
  protected MacroscopicLink twoWayRoad(int a, int b, double meters) {
    return link(a, b, meters, true, road);
  }

  /**
   * Register an intersection at a node with every segment entering it as an approach
   *
   * @param node index of the node
   * @param controlType of the intersection
   * @param type its kind
   * @return the intersection
   */
  protected Intersection intersectionAt(int node, IntersectionControlType controlType, IntersectionType type) {
    return intersectionAt(node, controlType, type, segment -> true);
  }

  /**
   * Register an intersection at a node with the segments entering it that meet a condition as approaches
   *
   * @param node index of the node
   * @param controlType of the intersection
   * @param type its kind
   * @param isApproach true for an entering segment to make an approach
   * @return the intersection
   */
  protected Intersection intersectionAt(int node, IntersectionControlType controlType, IntersectionType type,
      Predicate<MacroscopicLinkSegment> isApproach) {
    var intersection = intersections.getFactory().createNonSpatial(nodes.get(node), controlType, type);
    nodes.get(node).streamEntrySegments().map(MacroscopicLinkSegment.class::cast).filter(isApproach)
        .forEach(intersection::addApproachSegment);
    return intersections.getFactory().register(intersection);
  }

  /** a signalised junction at the node, every segment entering it an approach */
  protected Intersection signalisedJunctionAt(int node) {
    return intersectionAt(node, IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
  }

  /** a signalised junction at the node, only the roads entering it approaches, as vehicle signals give them */
  protected Intersection signalisedJunctionWithRoadApproachesAt(int node) {
    return intersectionAt(node, IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION,
        segment -> segment.isModeAllowed(car));
  }

  /** a signalised junction at the node whose only approach is the segment from the other node, as when its one vehicle
   * signal stands on that road */
  protected Intersection signalisedJunctionApproachedOnlyFrom(int node, int from) {
    return intersectionAt(node, IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION,
        segment -> segment.getUpstreamVertex() == nodes.get(from));
  }

  /** a signalised crossing at the node, only the segments entering it that a motorised road or rail mode may use
   * approaches, as a reader gives them */
  protected Intersection signalisedCrossingAt(int node) {
    return intersectionAt(node, IntersectionControlType.SIGNALISED, IntersectionType.CROSSING,
        segment -> segment.isAnyModeAllowed(Mode::isMotorisedRoadOrRailMode));
  }

  /** consolidate with the default values */
  protected IntersectionConsolidationModifierResult consolidate(
      Predicate<MacroscopicLinkSegment> hasSignalOfItsOwn) {
    return layer.getLayerModifier().consolidateIntersections(
        PlanitJtsCrsUtils.CARTESIANCRS, new IntersectionConsolidatorSettings(), hasSignalOfItsOwn);
  }

  /** consolidate with the default values, no segment with a signal of its own */
  protected IntersectionConsolidationModifierResult consolidate() {
    return consolidate(segment -> false);
  }

  /** consolidate with the default values except those the given change sets, no segment with a signal of its own */
  protected IntersectionConsolidationModifierResult consolidateWith(Consumer<IntersectionConsolidatorSettings> change) {
    var settings = new IntersectionConsolidatorSettings();
    change.accept(settings);
    return layer.getLayerModifier().consolidateIntersections(
        PlanitJtsCrsUtils.CARTESIANCRS, settings, segment -> false);
  }

  /** the one registered intersection */
  protected Intersection onlyIntersection() {
    assertEquals(1, intersections.size());
    return intersections.getFirst();
  }

  /** the one registered intersection, a spatial one */
  protected SpatialIntersection onlySpatialIntersection() {
    var intersection = onlyIntersection();
    assertTrue(intersection instanceof SpatialIntersection);
    return (SpatialIntersection) intersection;
  }

  /** the one registered intersection, a non-spatial one */
  protected NonSpatialIntersection onlyNonSpatialIntersection() {
    var intersection = onlyIntersection();
    assertTrue(intersection instanceof NonSpatialIntersection);
    return (NonSpatialIntersection) intersection;
  }

  /**
   * The movement of a spatial intersection from one segment to another
   *
   * @param intersection to look in
   * @param from segment the movement starts on
   * @param to segment the movement ends on
   * @return the movement
   */
  protected static SpatialMovement movementBetween(SpatialIntersection intersection, MacroscopicLinkSegment from,
      MacroscopicLinkSegment to) {
    return intersection.getMovements().stream()
        .filter(movement -> movement.getLinkSegmentFrom() == from && movement.getLinkSegmentTo() == to)
        .findFirst().orElseThrow();
  }

  /** the modes a movement of a spatial intersection is banned for, directly or implicitly, worked out from the layer */
  protected Set<Mode> bannedModes(SpatialIntersection intersection, SpatialMovement movement) {
    return SpatialMovementUtils.findBannedModes(layer, intersection, movement, true, true);
  }

  /** the indices of the nodes A to D of {@link #dividedRoadsCrossing()} */
  protected static final int A = 0, B = 1, C = 2, D = 3;

  /**
   * Two divided roads crossing, 15 m between the carriageways, arms of 85 m. Nodes A to D are where the carriageways
   * cross; the arm ends follow from index 4. No intersections yet
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <--- B <----- 8
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  protected void dividedRoadsCrossing() {
    dividedRoadsCrossingAt(0, 0);
  }

  /**
   * The divided roads crossing of {@link #dividedRoadsCrossing()}, moved by the given distances, its nodes and links
   * made in the same order. No intersections yet
   *
   * @param east metres to move it east
   * @param north metres to move it north
   * @return the indices of its nodes where the carriageways cross, as A, B, C and D there
   */
  protected int[] dividedRoadsCrossingAt(double east, double north) {
    int a = node(east, north + 15);
    int b = node(east + 15, north + 15);
    int c = node(east, north);
    int d = node(east + 15, north);
    int n = node(east, north + 100);
    int s = node(east, north - 85);
    int se = node(east + 15, north - 85);
    int ne = node(east + 15, north + 100);
    int en = node(east + 100, north + 15);
    int wn = node(east - 85, north + 15);
    int ws = node(east - 85, north);
    int es = node(east + 100, north);
    oneWayRoad(n, a, 85); oneWayRoad(a, c, 15); oneWayRoad(c, s, 85);
    oneWayRoad(se, d, 85); oneWayRoad(d, b, 15); oneWayRoad(b, ne, 85);
    oneWayRoad(en, b, 85); oneWayRoad(b, a, 15); oneWayRoad(a, wn, 85);
    oneWayRoad(ws, c, 85); oneWayRoad(c, d, 15); oneWayRoad(d, es, 85);
    return new int[] {a, b, c, d};
  }

  /**
   * Register a tram mode and a link type only a tram may use
   *
   * @return the tram link type
   */
  protected MacroscopicLinkSegmentType tramway() {
    var tram = network.getModes().getFactory().registerNew(PredefinedModeType.TRAM);
    layer.registerSupportedModes(List.of(tram));
    var tramway = layer.getLinkSegmentTypes().getFactory().registerNew("tramway", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(tramway, 50, List.of(tram));
    return tramway;
  }
}
