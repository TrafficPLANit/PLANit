package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests which crossings join a junction when it is consolidated: those next to it along the road, and those on a slip
 * lane leaving it
 *
 * @author markr
 */
public class CrossingsTest extends IntersectionConsolidationTestBase {

  /**
   * A junction with a crossing on its east arm, at the given distance along the road. A footway crosses the road at
   * the crossing
   * <pre>
   *            N       F1
   *            |       |
   *   W ------ J ----- K ----- E      all roads two-way, J-K as given, K-E 72 m, footway 10 m each side
   *            |       |
   *            S       F2
   * </pre>
   *
   * @param meters from the junction to the crossing
   * @return indices of the junction node and the crossing node
   */
  private int[] junctionWithCrossingOnEastArm(double meters) {
    int junctionNode = node(0, 0);
    twoWayRoad(junctionNode, node(-85, 0), 85);
    twoWayRoad(junctionNode, node(0, 85), 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int crossingNode = node(meters, 0);
    twoWayRoad(junctionNode, crossingNode, meters);
    twoWayRoad(crossingNode, node(meters + 72, 0), 72);
    link(crossingNode, node(meters, 10), 10, true, footway);
    link(crossingNode, node(meters, -10), 10, true, footway);
    return new int[] {junctionNode, crossingNode};
  }

  /**
   * A crossing 13 m along the road from the junction joins it. The junction, on one node, gains the crossing's node, so
   * a new spatial intersection replaces it: both nodes are members, the kind crossing is added, the road between them
   * becomes internal and the crossing's other approaches stay approaches
   */
  @Test
  public void crossingNextToAJunctionJoinsIt() {
    var junctionAndCrossing = junctionWithCrossingOnEastArm(13);
    int junctionNode = junctionAndCrossing[0];
    int crossingNode = junctionAndCrossing[1];
    var junction = signalisedJunctionAt(junctionNode);
    var crossing = signalisedCrossingAt(crossingNode);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertNotSame(junction, consolidated);
    assertTrue(result.getReplacedIntersections().stream().anyMatch(
        pair -> pair.first() == junction && pair.second() == consolidated));
    assertEquals(List.of(crossing), result.getCrossingsJoined());
    assertTrue(consolidated.hasMemberNode(nodes.get(junctionNode)));
    assertTrue(consolidated.hasMemberNode(nodes.get(crossingNode)));
    assertEquals(Set.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), consolidated.getTypes());
    assertEquals(Set.of(segment(junctionNode, crossingNode), segment(crossingNode, junctionNode)),
        Set.copyOf(consolidated.getInternalSegments()));
    int east = crossingNode + 1;
    int footwayNorth = crossingNode + 2;
    int footwaySouth = crossingNode + 3;
    assertTrue(consolidated.getApproachSegments().containsAll(List.of(segment(east, crossingNode),
        segment(footwayNorth, crossingNode), segment(footwaySouth, crossingNode))));
  }

  /**
   * A crossing 40 m along the road from the nearest junction stays as it is
   */
  @Test
  public void crossingOutOfReachStaysAsItIs() {
    var junctionAndCrossing = junctionWithCrossingOnEastArm(40);
    signalisedJunctionAt(junctionAndCrossing[0]);
    signalisedCrossingAt(junctionAndCrossing[1]);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getCrossingsJoined().isEmpty());
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
    assertEquals(1, result.getCrossingsJoinedFromSlipLane().size());
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
}
