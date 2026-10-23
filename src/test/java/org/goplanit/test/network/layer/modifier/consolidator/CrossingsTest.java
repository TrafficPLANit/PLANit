package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.CROSSING_JOINED;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.CROSSING_JOINED_FROM_SLIP_LANE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests which crossings join a junction when it is consolidated: those next to it along the road, and those on a slip
 * lane leaving it
 *
 * @author markr
 */
public class CrossingsTest extends IntersectionConsolidationTestBase {

  /**
   * A junction with a crossing node on its east arm and one on its north arm, each at the given distance along the
   * road. A footway crosses the road at each crossing node, and a footway runs straight from one crossing node to the
   * other. The road from K2 into J is one-way for cars, and pedestrians walk it both ways. A tram track crosses the
   * south footway at its end F2, where no road passes. The test registers the crossings it needs
   * <pre>
   *            N
   *            |
   *   F3 ---- K2 ---- F4              K2-N 72 m, footway 10 m each side
   *            v  \
   *            |    \   F1            footway K2-K, straight
   *            |      \ |
   *   W ------ J ----- K ----- E      J-K and J-K2 as given, K-E 72 m, footway 10 m each side; other roads two-way
   *            |       |
   *            S  PW # F2 # PE        tram track PW-F2-PE, 10 m each side, two-way
   * </pre>
   *
   * @param meters from the junction to each crossing node
   * @return indices of the junction node, the crossing node on the east arm, the one on the north arm and F2
   */
  private int[] junctionWithCrossingsOnEastAndNorthArms(double meters) {
    int junctionNode = node(0, 0);
    twoWayRoad(junctionNode, node(-85, 0), 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int crossingNode = node(meters, 0);
    twoWayRoad(junctionNode, crossingNode, meters);
    twoWayRoad(crossingNode, node(meters + 72, 0), 72);
    link(crossingNode, node(meters, 10), 10, true, footway);
    link(crossingNode, node(meters, -10), 10, true, footway);
    int northCrossingNode = node(0, meters);
    var northArm = oneWayRoad(northCrossingNode, junctionNode, meters);
    layer.getLinkSegments().getFactory().registerNew(northArm, false, true).setLinkSegmentType(footway);
    twoWayRoad(northCrossingNode, node(0, meters + 72), 72);
    link(northCrossingNode, node(-10, meters), 10, true, footway);
    link(northCrossingNode, node(10, meters), 10, true, footway);
    link(northCrossingNode, crossingNode, meters * Math.sqrt(2), true, footway);
    int footwaySouthEnd = crossingNode + 3;
    var tramway = tramway();
    link(footwaySouthEnd, node(meters - 10, -10), 10, true, tramway);
    link(footwaySouthEnd, node(meters + 10, -10), 10, true, tramway);
    return new int[] {junctionNode, crossingNode, northCrossingNode, footwaySouthEnd};
  }

  /**
   * Crossings 13 m along the road from the junction, on its east and north arms, join it. So does the crossing of the
   * tram track at F2: no road reaches it, so it looks for its junction along its own links, and J lies 23 m away over
   * the footway and the road. The junction, on one node, gains the crossings' nodes, so a new spatial intersection replaces
   * it: the four nodes are members, the kind crossing is added, the roads between them and the footways between the
   * crossings become internal, and the road into the east crossing and the tram track into F2 stay approaches. The
   * footways entering the crossings from outside are not approaches: a crossing's approaches are for road and rail
   * traffic only. The
   * footways between the crossings carry the crossings' signals, so each direction becomes an internal segment with a
   * signal. The roads do not, in either direction. Into a crossing, its signal is for road traffic from outside, which
   * keeps its approach, and walking along the road from J to K2 passes no light. Into the junction, the junction's
   * signal stands on its node, which counts for its approaches only
   */
  @Test
  public void crossingsNextToAJunctionJoinIt() {
    var junctionAndCrossings = junctionWithCrossingsOnEastAndNorthArms(13);
    int junctionNode = junctionAndCrossings[0];
    int crossingNode = junctionAndCrossings[1];
    int northCrossingNode = junctionAndCrossings[2];
    int trackCrossingNode = junctionAndCrossings[3];
    var junction = signalisedJunctionAt(junctionNode);
    var crossings = Set.of(signalisedCrossingAt(crossingNode), signalisedCrossingAt(northCrossingNode),
        signalisedCrossingAt(trackCrossingNode));

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertNotSame(junction, consolidated);
    assertTrue(result.getReplacedIntersections().stream().anyMatch(
        pair -> pair.first() == junction && pair.second() == consolidated));
    assertEquals(crossings, Set.copyOf(result.getIntersections(CROSSING_JOINED)));
    assertTrue(List.of(junctionNode, crossingNode, northCrossingNode, trackCrossingNode).stream()
        .allMatch(index -> consolidated.hasMemberNode(nodes.get(index))));
    assertEquals(Set.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), consolidated.getTypes());
    var footwayBetweenCrossings = Set.of(segment(crossingNode, northCrossingNode),
        segment(northCrossingNode, crossingNode), segment(crossingNode, trackCrossingNode),
        segment(trackCrossingNode, crossingNode));
    var roadsBetweenMembers = Set.of(segment(junctionNode, crossingNode), segment(crossingNode, junctionNode),
        segment(junctionNode, northCrossingNode), segment(northCrossingNode, junctionNode));
    var internal = new HashSet<>(footwayBetweenCrossings);
    internal.addAll(roadsBetweenMembers);
    assertEquals(internal, Set.copyOf(consolidated.getInternalSegments()));
    assertEquals(footwayBetweenCrossings, Set.copyOf(consolidated.getInternalSegmentsWithSignal()));
    int east = crossingNode + 1;
    int footwayNorth = crossingNode + 2;
    int trackWest = nodes.size() - 2;
    int trackEast = nodes.size() - 1;
    assertTrue(consolidated.getApproachSegments().containsAll(List.of(segment(east, crossingNode),
        segment(trackWest, trackCrossingNode), segment(trackEast, trackCrossingNode))));
    assertFalse(consolidated.getApproachSegments().contains(segment(footwayNorth, crossingNode)));
  }

  /**
   * A crossing 5 m beyond the joining reach along the road from the nearest junction stays as it is
   */
  @Test
  public void crossingOutOfReachStaysAsItIs() {
    var junctionAndCrossing = junctionWithCrossingsOnEastAndNorthArms(
        IntersectionConsolidatorSettings.DEFAULT_MAXIMUM_JOINING_REACH_METERS + 5);
    signalisedJunctionAt(junctionAndCrossing[0]);
    signalisedCrossingAt(junctionAndCrossing[1]);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(CROSSING_JOINED).isEmpty());
  }

  /**
   * A crossing on a side street that leaves the junction's approach before its stop line stays as it is, though it lies
   * within the joining reach of the junction along the road. The road from it passes X, where traffic
   * comes in from outside and then crosses the stop line into J, so X lies in front of the junction. Joining the
   * crossing would take X in and turn the stop line into an internal segment
   * <pre>
   *              K                   crossing on the side street, K-K2 85 m, one-way away from X
   *              ^
   *              | 10 m
   *   W -------> X --|S|--> J -----> E        W-X 85 m, X-J 18 m with J's stop line, J-E 85 m, one-way
   *                         |
   *                         S                 J-S 85 m, two-way
   * </pre>
   */
  @Test
  public void crossingOnASideStreetBeforeTheJunctionsStopLineStaysApart() {
    int junctionNode = node(0, 0);
    int x = node(-18, 0);
    oneWayRoad(node(-103, 0), x, 85);
    var stopLine = oneWayRoad(x, junctionNode, 18);
    oneWayRoad(junctionNode, node(85, 0), 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int crossingNode = node(-18, 10);
    oneWayRoad(x, crossingNode, 10);
    oneWayRoad(crossingNode, node(-18, 95), 85);
    signalisedJunctionAt(junctionNode);
    signalisedCrossingAt(crossingNode);

    var result = consolidate(segment -> segment == stopLine.getLinkSegmentAb());

    assertTrue(result.getIntersections(CROSSING_JOINED).isEmpty());
    assertEquals(2, intersections.size());
    assertTrue(intersections.stream().noneMatch(intersection -> intersection.hasMemberNode(nodes.get(x))));
  }

  /**
   * A crossing on a road forking off one of the junction's exits joins it: the only traffic coming into the fork F
   * comes out of the junction, so F is not in front of it, and F joins with the crossing
   * <pre>
   *   W --|S|--> J -------> F -------> E      W-J 85 m with J's stop line, J-F 11 m, F-E 85 m, one-way
   *              |          |
   *              S          v 14 m
   *                         K -------> K2     crossing K on the fork road, K-K2 85 m, one-way; J-S 85 m, two-way
   * </pre>
   */
  @Test
  public void crossingOnARoadForkingOffAnExitJoins() {
    int junctionNode = node(0, 0);
    var stopLine = oneWayRoad(node(-85, 0), junctionNode, 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int fork = node(11, 0);
    oneWayRoad(junctionNode, fork, 11);
    oneWayRoad(fork, node(96, 0), 85);
    int crossingNode = node(11, -14);
    oneWayRoad(fork, crossingNode, 14);
    oneWayRoad(crossingNode, node(11, -99), 85);
    signalisedJunctionAt(junctionNode);
    var crossing = signalisedCrossingAt(crossingNode);

    var result = consolidate(segment -> segment == stopLine.getLinkSegmentAb());

    assertEquals(List.of(crossing), result.getIntersections(CROSSING_JOINED));
    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(fork)));
    assertTrue(consolidated.getInternalSegments().containsAll(
        List.of(segment(junctionNode, fork), segment(fork, crossingNode))));
  }

  /**
   * A crossing on the junction's approach joins it, with the junction's stop line past the crossing becoming an
   * internal segment with a signal: the only traffic coming into N comes from the crossing, which counts as part of the
   * junction, so N is not in front of it. The road into the crossing becomes the approach
   * <pre>
   *   W -------> K -------> N --|S|--> J -------> E    W-K 85 m, K-N 15 m, N-J 10 m with J's stop line, one-way
   *                                    |
   *                                    S               J-E and J-S 85 m, J-S two-way
   * </pre>
   */
  @Test
  public void crossingOnAnApproachJoinsPastTheJunctionsStopLine() {
    int junctionNode = node(0, 0);
    int n = node(-10, 0);
    int crossingNode = node(-25, 0);
    int west = node(-110, 0);
    oneWayRoad(west, crossingNode, 85);
    oneWayRoad(crossingNode, n, 15);
    var stopLine = oneWayRoad(n, junctionNode, 10);
    oneWayRoad(junctionNode, node(85, 0), 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    signalisedJunctionAt(junctionNode);
    var crossing = signalisedCrossingAt(crossingNode);

    var result = consolidate(segment -> segment == stopLine.getLinkSegmentAb());

    assertEquals(List.of(crossing), result.getIntersections(CROSSING_JOINED));
    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(n)));
    assertTrue(consolidated.getInternalSegmentsWithSignal().contains(segment(n, junctionNode)));
    assertTrue(consolidated.getApproachSegments().contains(segment(west, crossingNode)));
  }

  /**
   * A crossing past a merge on the junction's exit joins it, though a short road comes into the merge M from outside
   * and the crossing's own stop line stands on the road from M into it. Only a stop line into the junction puts M in
   * front of it; the crossing's own is not one
   * <pre>
   *                       T                T-M 4 m, one-way into M, fed by a road from the north, 85 m
   *                       v
   *   W --|S|--> J -----> M --|s|--> K -----> E    W-J 85 m with J's stop line, J-M 10 m, M-K 7 m with the
   *              |                                 crossing's stop line, K-E 85 m, one-way; J-S 85 m, two-way
   *              S
   * </pre>
   */
  @Test
  public void crossingPastAMergeJoinsDespiteItsOwnStopLine() {
    int junctionNode = node(0, 0);
    var junctionsStopLine = oneWayRoad(node(-85, 0), junctionNode, 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int merge = node(10, 0);
    oneWayRoad(junctionNode, merge, 10);
    int stub = node(10, 4);
    oneWayRoad(node(10, 89), stub, 85);
    oneWayRoad(stub, merge, 4);
    int crossingNode = node(17, 0);
    var crossingsStopLine = oneWayRoad(merge, crossingNode, 7);
    oneWayRoad(crossingNode, node(102, 0), 85);
    signalisedJunctionAt(junctionNode);
    var crossing = signalisedCrossingAt(crossingNode);
    var stopLines = Set.of(junctionsStopLine.getLinkSegmentAb(), crossingsStopLine.getLinkSegmentAb());

    var result = consolidate(stopLines::contains);

    assertEquals(List.of(crossing), result.getIntersections(CROSSING_JOINED));
    assertTrue(onlySpatialIntersection().hasMemberNode(nodes.get(merge)));
  }

  /**
   * A junction with a slip lane: a one-way slip lane leaves the road from the west 44 m before junction X and merges
   * with the exit road south 81 m after it. Optionally a signalised crossing sits halfway along the slip lane, 46 m
   * from X in a straight line and 90 m along the road
   * <pre>
   *             N
   *             |
   *   W ---> SP ---> X ---> E      W-SP 106 m, SP-X 44 m, X-MG 81 m, MG-S 69 m; N, E two-way 85 m
   *           \      |
   *          SL      |             SP-SL 46 m, SL-MG 46 m, one-way
   *             \    v
   *              -> MG ---> S
   * </pre>
   *
   * @param withCrossing true to put a signalised crossing on the slip lane, with a footway across it
   * @return indices of X, SP, SL and MG
   */
  private int[] junctionWithSlipLane(boolean withCrossing) {
    int x = node(0, 0);
    int slipLaneStart = node(-44, 0);
    int slipLaneEnd = node(0, -81);
    oneWayRoad(node(-150, 0), slipLaneStart, 106);
    oneWayRoad(slipLaneStart, x, 44);
    oneWayRoad(x, slipLaneEnd, 81);
    oneWayRoad(slipLaneEnd, node(0, -150), 69);
    twoWayRoad(x, node(0, 85), 85);
    twoWayRoad(x, node(85, 0), 85);
    int slipLane = node(-22, -40.5);
    oneWayRoad(slipLaneStart, slipLane, 46);
    oneWayRoad(slipLane, slipLaneEnd, 46);
    signalisedJunctionAt(x);
    if (withCrossing) {
      link(slipLane, node(-30, -35), 10, true, footway);
      link(slipLane, node(-14, -46), 10, true, footway);
      signalisedCrossingAt(slipLane);
    }
    return new int[] {x, slipLaneStart, slipLane, slipLaneEnd};
  }

  /**
   * The crossing on the slip lane joins junction X: its node becomes a member, the slip lane segment into it an
   * approach, and the slip lane's own ends stay outside
   */
  @Test
  public void crossingHalfwayAlongASlipLaneJoinsTheJunction() {
    var slipLaneNodes = junctionWithSlipLane(true);
    int slipLaneStart = slipLaneNodes[1];
    int slipLane = slipLaneNodes[2];
    int slipLaneEnd = slipLaneNodes[3];

    var result = consolidate();

    var junction = onlySpatialIntersection();
    assertEquals(1, result.getIntersections(CROSSING_JOINED_FROM_SLIP_LANE).size());
    assertTrue(junction.hasMemberNode(nodes.get(slipLane)));
    assertFalse(junction.hasMemberNode(nodes.get(slipLaneStart)));
    assertFalse(junction.hasMemberNode(nodes.get(slipLaneEnd)));
    assertTrue(junction.getApproachSegments().contains(segment(slipLaneStart, slipLane)));
    assertFalse(junction.getInternalSegments().contains(segment(slipLane, slipLaneEnd)));
    assertTrue(junction.getTypes().contains(IntersectionType.CROSSING));
  }

  /**
   * A give-way slip lane, without a crossing on it, stays outside the junction: its nodes do not become members, so the
   * junction stays on its one node without internal segments, and the slip lane's segments are not approaches
   */
  @Test
  public void giveWaySlipLaneStaysOutside() {
    var slipLaneNodes = junctionWithSlipLane(false);
    int slipLaneStart = slipLaneNodes[1];
    int slipLane = slipLaneNodes[2];
    int slipLaneEnd = slipLaneNodes[3];

    consolidate();

    var junction = onlyNonSpatialIntersection();
    assertEquals(1, junction.getMemberNodes().size());
    for (var slipLaneSegment : List.of(segment(slipLaneStart, slipLane), segment(slipLane, slipLaneEnd))) {
      assertFalse(junction.getApproachSegments().contains(slipLaneSegment));
    }
  }

  /**
   * The crossing K, 13 m along the east arm, would join the junction J, see {@link #crossingsNextToAJunctionJoinIt()}.
   * But J and K are listed to keep in separate intersections, and no other junction is within reach, so the crossing
   * stays as it is: both intersections are left unchanged, and no crossing is listed as joined. Only J and K are
   * signalised
   * <pre>
   *            N
   *            |
   *           K2
   *            |
   *   W ------ J ----- K ----- E      J-K 13 m; J and K listed to keep in separate intersections
   *            |       |
   *            S       F2             the rest of the network as drawn for the shared scenario
   * </pre>
   */
  @Test
  public void crossingListedToKeepApartFromItsJunctionStaysAsItIs() {
    var junctionAndCrossings = junctionWithCrossingsOnEastAndNorthArms(13);
    int junctionNode = junctionAndCrossings[0];
    int crossingNode = junctionAndCrossings[1];
    nodes.get(junctionNode).setExternalId("J");
    nodes.get(crossingNode).setExternalId("K");
    var junction = signalisedJunctionAt(junctionNode);
    var crossing = signalisedCrossingAt(crossingNode);

    var result = consolidateWith(settings -> settings.addNodeIdsToKeepInSeparateIntersections(
        List.of("J", "K"), IdMapperType.EXTERNAL_ID));

    assertEquals(2, intersections.size());
    assertSame(junction, intersections.getByMemberNode(nodes.get(junctionNode)));
    assertSame(crossing, intersections.getByMemberNode(nodes.get(crossingNode)));
    assertTrue(result.getIntersections(CROSSING_JOINED).isEmpty());
  }
}
