package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests collapsing chosen modes onto one centre node per spatial intersection after consolidation, and how their bans
 * are carried over
 *
 * @author markr
 */
public class CollapseTest extends IntersectionConsolidationTestBase {

  /** collapse the given modes on the given spatial intersections */
  private IntersectionCollapseModifierResult collapse(
      List<SpatialIntersection> spatialIntersections, List<Mode> collapsedModes) {
    return layer.getLayerModifier().collapseIntersectionsToNonSpatialForModes(
        spatialIntersections, collapsedModes, PlanitJtsCrsUtils.CARTESIANCRS);
  }

  /** true when the turn from one segment onto the other is banned for the mode */
  private boolean isTurnBanned(MacroscopicLinkSegment from, MacroscopicLinkSegment onto, Mode mode) {
    return MacroscopicNetworkLayerUtils.isTurnBannedForMode(layer, from, onto, mode);
  }

  /** consolidate with the default values, then collapse the given modes on every spatial intersection */
  private IntersectionCollapseModifierResult consolidateThenCollapse(List<Mode> collapsedModes) {
    consolidate();
    return collapse(layer.getIntersections().streamSpatial().collect(Collectors.toList()), collapsedModes);
  }

  /** indices of the footway ends P and Q added by {@link #dividedRoadsCrossingWithFootways(IntConsumer)} */
  private static final int P = 12, Q = 13;

  /**
   * The divided roads crossing, its four corners signalised, with a footway crossing the westbound carriageway between
   * A and B, and footways from P to A and from Q to B. Roads allow cars only, footways pedestrians only
   * <pre>
   *     P                Q        P (-20, 30), Q (35, 30)
   *       \            /
   *         A ~~~~~~ B            ~ footway, 15 m
   *         C        D            roads as in dividedRoadsCrossing
   * </pre>
   *
   * @param signalisedJunctionAt registers the signalised junction at each corner, with the approaches it is to have
   */
  private void dividedRoadsCrossingWithFootways(IntConsumer signalisedJunctionAt) {
    dividedRoadsCrossing();
    node(-20, 30);
    node(35, 30);
    link(A, B, 15, true, footway);
    link(P, A, 25, true, footway);
    link(Q, B, 25, true, footway);
    List.of(A, B, C, D).forEach(signalisedJunctionAt::accept);
  }

  /** register a link segment type allowing cars and pedestrians */
  private MacroscopicLinkSegmentType carAndPedestrianType() {
    var carAndPedestrian = layer.getLinkSegmentTypes().getFactory().registerNew("shared", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carAndPedestrian, 50, List.of(car, pedestrian));
    return carAndPedestrian;
  }

  /**
   * Collapsing pedestrians moves the footways P-A and Q-B to a new centre node, and the footway between A and B, left
   * without access, is removed. The roads still meet at the four member nodes, and the movements stay as they were:
   * 16 for cars and 2 between the footways, the left turn for cars still not banned for them
   */
  @Test
  public void footwaysCollapsedRoadsKept() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var footwayAtA = layer.getLinks().stream().filter(link -> link.getNodeA() == nodes.get(P))
        .findFirst().orElseThrow();

    var result = consolidateThenCollapse(List.of(pedestrian));

    var consolidated = onlySpatialIntersection();
    assertEquals(5, consolidated.getMemberNodes().size());
    var centre = footwayAtA.getNodeB();
    assertTrue(consolidated.hasMemberNode(centre));
    assertFalse(List.of(A, B, C, D).stream().anyMatch(index -> nodes.get(index) == centre));
    assertEquals(2, result.getLinksMovedToCentreNode().size());
    assertTrue(layer.getLinks().stream().noneMatch(
        link -> Set.of(link.getNodeA(), link.getNodeB()).equals(Set.of(nodes.get(A), nodes.get(B))) &&
            link.getLinkSegmentAb().getLinkSegmentType() == footway));
    assertTrue(consolidated.getInternalSegments().containsAll(
        List.of(segment(A, C), segment(B, A), segment(D, B), segment(C, D))));
    assertEquals(18, consolidated.getMovements().size());
    assertEquals(18, layer.getSpatialMovements().size());
    var leftTurn = movementBetween(consolidated, segment(4, A), segment(D, 11));
    assertEquals(0.030, leftTurn.getLengthKm(), 1e-9);
    assertTrue(bannedModes(consolidated, leftTurn).isEmpty());
    assertTrue(result.getLinksAddedToCentreNode().isEmpty());
  }

  /**
   * The road arriving at B from the east also allows pedestrians. Collapsing pedestrians keeps that road on B, and
   * joins the centre to B with a link for pedestrians only, so they can still reach the collapsed footways
   */
  @Test
  public void sharedRoadKeepsTheCollapsedModeConnected() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    segment(8, B).setLinkSegmentType(carAndPedestrian);

    var result = consolidateThenCollapse(List.of(pedestrian));

    assertEquals(1, result.getLinksAddedToCentreNode().size());
    var added = result.getLinksAddedToCentreNode().get(0);
    assertTrue(Set.of(added.getNodeA(), added.getNodeB()).contains(nodes.get(B)));
    for (var segment : added.getEdgeSegments()) {
      assertEquals(Set.of(pedestrian), ((MacroscopicLinkSegment) segment).getAllowedModes());
      assertTrue(onlySpatialIntersection().getInternalSegments().contains((MacroscopicLinkSegment) segment));
    }
    assertSame(nodes.get(B), segment(8, B).getDownstreamVertex());
  }

  /**
   * Collapsing every mode leaves one node: a non-spatial intersection on the centre node replaces the spatial one,
   * every arm ends at the centre, and the inner links, the former member nodes and the movements are gone
   */
  @Test
  public void everyModeCollapsedLeavesOneNode() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var corners = List.of(nodes.get(A), nodes.get(B), nodes.get(C), nodes.get(D));

    consolidateThenCollapse(List.of(car, pedestrian));

    var consolidated = onlyNonSpatialIntersection();
    assertEquals(1, consolidated.getMemberNodes().size());
    var centre = consolidated.getNode();
    assertEquals(10, centre.getEdges().size());
    corners.forEach(corner -> assertNull(layer.getNodes().get(corner.getId())));
    assertEquals(10, layer.getLinks().size());
    assertTrue(layer.getSpatialMovements().isEmpty());
  }

  /**
   * Cars may not turn from A-C onto C-D at member node C, which stops them going from the northern approach to the
   * eastern exit. Once every mode is collapsed, that ban is carried to the centre node: the turn there from the
   * northern approach onto the eastern exit is banned for cars, while the turn onto the southern exit, which cars could
   * make, is not
   */
  @Test
  public void banCarriedToTheCentreNode() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D), List.of(car));
    var north = segment(4, A);
    var east = segment(D, 11);
    var south = segment(C, 5);

    var result = consolidateThenCollapse(List.of(car, pedestrian));

    var centre = onlyNonSpatialIntersection().getNode();
    var bansFromNorth = layer.getBannedTurns().getBySegment(north).stream()
        .filter(ban -> ban.getSegmentFrom() == north).collect(Collectors.toList());
    var northToEast = (MacroscopicBannedTurn) bansFromNorth.stream()
        .filter(ban -> ban.getSegmentTo() == east).findFirst().orElseThrow();
    assertSame(centre, northToEast.getCentreVertex());
    assertTrue(northToEast.isModeBanned(car));
    assertTrue(result.getBannedTurnsAddedOrExtendedForCollapsedModes().stream().anyMatch(ban -> ban == northToEast));
    assertTrue(bansFromNorth.stream().noneMatch(ban -> ban.getSegmentTo() == south));
  }

  /**
   * Pedestrians are collapsed. The footways F1 (P-A) and F2 (Q-B) move to the centre node, while the roads R1 (B-7)
   * and R2 (B-8), which allow cars and pedestrians, stay on B. A turn at B banned for pedestrians, from the footway A-B
   * onto R1, stops them going from F1 to R1, while they can go from F1 to R2 and from F2 to R1. After collapse,
   * pedestrians from F1 to R1 turn at the centre onto the new link to B, and at B onto R1: the first turn is shared
   * with F1 to R2, the second with F2 to R1. So no new turn is banned for it, and the ban is counted as not held
   * <pre>
   *     P            7   Q
   *       \          ^  /         F1 = P-A, F2 = Q-B and A-B are footways; R1 = B-7 and R2 = B-8 allow cars and
   *         A ~~~~~~ B ---> 8     pedestrians; the other roads are those of the divided roads crossing
   *         C        D
   * </pre>
   */
  @Test
  public void banThatNoNewTurnCanHoldIsCountedAsNotHeld() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var r1 = segment(B, 7);
    r1.setLinkSegmentType(carAndPedestrian);
    var r2 = layer.getLinkSegments().getFactory().registerNew(segment(8, B).getParent(), false, true);
    r2.setLinkSegmentType(carAndPedestrian);
    var f1 = segment(P, A);
    layer.getBannedTurns().getFactory().registerNew(segment(A, B), r1, List.of(pedestrian));

    var result = consolidateThenCollapse(List.of(pedestrian));

    var centreToB = result.getLinksAddedToCentreNode().stream().flatMap(link -> link.getEdgeSegments().stream())
        .map(MacroscopicLinkSegment.class::cast).filter(segment -> segment.getDownstreamVertex() == nodes.get(B))
        .findFirst().orElseThrow();
    for (var turn : List.of(List.of(f1, centreToB), List.of(centreToB, r1))) {
      assertTrue(layer.getBannedTurns().getBySegment(turn.get(0)).stream().noneMatch(
          ban -> ban.getSegmentFrom() == turn.get(0) && ban.getSegmentTo() == turn.get(1) &&
              ((MacroscopicBannedTurn) ban).isModeBanned(pedestrian)));
    }
    assertEquals(1, result.getCollapsedModeBansNotHeld().size());
    var notHeld = result.getCollapsedModeBansNotHeld().get(0);
    assertSame(f1, notHeld.first().getLinkSegmentFrom());
    assertSame(r1, notHeld.first().getLinkSegmentTo());
    assertSame(pedestrian, notHeld.second());
  }

  /**
   * Banned turns on internal segments lose the collapsed modes: once pedestrians are collapsed, the turn from A-C onto
   * C-D banned for cars and pedestrians is banned for cars only, and the turn from C-D onto D-B banned for pedestrians
   * only is removed
   */
  @Test
  public void bannedTurnsOnInternalSegmentsLoseTheCollapsedModes() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var bans = layer.getBannedTurns().getFactory();
    var carAndPedestrianBan = bans.registerNew(segment(A, C), segment(C, D), List.of(car, pedestrian));
    var pedestrianBan = bans.registerNew(segment(C, D), segment(D, B), List.of(pedestrian));

    var result = consolidateThenCollapse(List.of(pedestrian));

    assertSame(carAndPedestrianBan, layer.getBannedTurns().get(carAndPedestrianBan.getId()));
    assertEquals(Set.of(car), carAndPedestrianBan.getExplicitlyBannedModes());
    assertTrue(result.getBannedTurnsThatLostCollapsedModes().stream().anyMatch(ban -> ban == carAndPedestrianBan));
    assertNull(layer.getBannedTurns().get(pedestrianBan.getId()));
    assertTrue(result.getRemovedBannedTurns().stream().anyMatch(ban -> ban == pedestrianBan));
  }

  /**
   * Link A-B is two-way. Cars may not turn from the northern approach 4-A onto the exit A-9, nor from A-C onto C-D, so
   * a car could reach A-9 only by driving along A-B and straight back, which no driver does inside a junction. Once
   * every mode is collapsed, the turn at the centre node from 4-A onto A-9 is banned for cars
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <--> B <----- 8        A-B two-way; 4-A onto A-9 and A-C onto C-D banned for cars
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void banGotRoundOnlyByGoingBackAlongAnInnerLinkIsCarriedToTheCentreNode() {
    dividedRoadsCrossing();
    layer.getLinkSegments().getFactory().registerNew(segment(B, A).getParent(), false, true).setLinkSegmentType(road);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    layer.getBannedTurns().getFactory().registerNew(segment(4, A), segment(A, 9), List.of(car));
    layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D), List.of(car));
    var north = segment(4, A);
    var west = segment(A, 9);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(north, west, car));
  }

  /**
   * The northern approach 4-A and the exit B-7 allow cars and pedestrians, and pedestrians may also walk from A to B on
   * link A-B. D-B is a footway, so cars cannot get from A to B. Nothing is banned, yet only pedestrians can go from 4-A
   * to B-7. Once every mode is collapsed, the turn at the centre node from 4-A onto B-7 is banned for cars, and not for
   * pedestrians
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <~~> B <----- 8        4-A and B-7 cars and pedestrians; A to B and D-B footways
   *              |      ~
   *              v      ~
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void turnOnlyOtherModesCouldMakeIsBannedForTheCollapsedModes() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossing();
    segment(4, A).setLinkSegmentType(carAndPedestrian);
    segment(B, 7).setLinkSegmentType(carAndPedestrian);
    segment(D, B).setLinkSegmentType(footway);
    layer.getLinkSegments().getFactory().registerNew(segment(B, A).getParent(), false, true)
        .setLinkSegmentType(footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var north = segment(4, A);
    var exitAtB = segment(B, 7);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(north, exitAtB, car));
    assertFalse(isTurnBanned(north, exitAtB, pedestrian));
  }

  /**
   * D-B is a footway, so cars arriving from the south at D can only go on east along D-11: no mode goes from the
   * southern approach 6-D to the exit B-7. Once every mode is collapsed, the turn at the centre node from 6-D onto B-7
   * is banned for cars, while the turn from the eastern approach 8-B onto B-7, which cars could make, is not
   */
  @Test
  public void turnNoModeCouldMakeIsBannedAtTheCentreNode() {
    dividedRoadsCrossing();
    segment(D, B).setLinkSegmentType(footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var south = segment(6, D);
    var east = segment(8, B);
    var exitAtB = segment(B, 7);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(south, exitAtB, car));
    assertFalse(isTurnBanned(east, exitAtB, car));
  }

  /**
   * Only the roads are approaches, as vehicle signals give them, so no movement starts from the footways P-A and Q-B.
   * While the intersection was spatial, pedestrians could still walk from P to Q, and from Q to P, over the footway
   * A-B. Once pedestrians are collapsed, the turns at the centre node from P onto Q and from Q onto P stay open
   */
  @Test
  public void entryThatIsNotAnApproachKeepsTheTurnsItCouldMake() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionWithRoadApproachesAt);
    var pIn = segment(P, A);
    var pOut = segment(A, P);
    var qIn = segment(Q, B);
    var qOut = segment(B, Q);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(pedestrian));

    assertFalse(isTurnBanned(pIn, qOut, pedestrian));
    assertFalse(isTurnBanned(qIn, pOut, pedestrian));
  }

  /**
   * A spatial intersection that is not registered on the layer is refused before anything changes, even when it comes
   * after a registered one: the registered intersection keeps its four member nodes, and no node or link is added or
   * removed
   */
  @Test
  public void intersectionNotOnTheLayerIsRefusedBeforeAnyChange() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    consolidate();
    var registered = onlySpatialIntersection();
    var notRegistered = intersections.getFactory().createSpatial(
        List.of(nodes.get(P), nodes.get(Q)), IntersectionControlType.SIGNALISED, List.of(IntersectionType.JUNCTION));
    var nodesBefore = layer.getNodes().size();
    var linksBefore = layer.getLinks().size();

    assertThrows(PlanItRunTimeException.class,
        () -> collapse(List.of(registered, notRegistered), List.of(pedestrian)));

    assertEquals(4, registered.getMemberNodes().size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());
  }

  /**
   * Collapse given no modes, or no intersections, changes nothing: the consolidated intersection stays, with its four
   * member nodes, no node or link is added or removed, and neither result lists a change
   */
  @Test
  public void noModesOrNoIntersectionsChangeNothing() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    consolidate();
    var consolidated = onlySpatialIntersection();
    var nodesBefore = layer.getNodes().size();
    var linksBefore = layer.getLinks().size();

    var noModes = collapse(List.of(consolidated), List.of());
    var noIntersections = collapse(List.of(), List.of(pedestrian));

    for (var result : List.of(noModes, noIntersections)) {
      assertTrue(result.getLinksMovedToCentreNode().isEmpty());
      assertTrue(result.getLinksAddedToCentreNode().isEmpty());
      assertTrue(result.getRemovedBannedTurns().isEmpty());
      assertTrue(result.getModeAccessCleanup().isEmpty());
    }
    assertSame(consolidated, onlySpatialIntersection());
    assertEquals(4, consolidated.getMemberNodes().size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());
  }
}
