package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.CROSSING_JOINED;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.JUNCTION_PULLED_IN_WITHIN_RADIUS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests joining the intersections the settings list to join into one. Once the crossings have joined, the
 * intersections holding the nodes of a list are joined into one, and the steps from the pull-ins to the crossings run
 * again for it. The signalised junctions J1 and J2 lie 60 m apart along the road, beyond the joining reach and beyond
 * the pull-in margin of a junction on one node, so consolidation alone keeps them apart. Each node's external id is its
 * name
 * <pre>
 *            N1                 N2
 *            |                  |               all roads two-way
 *            |                  |               N1-J1, N2-J2, W-J1 and J2-E 85 m
 *   W ------ J1 ----- M ------- J2 ------ E     J1-M and M-J2 30 m; M is in no intersection
 * </pre>
 *
 * @author markr
 */
public class NodesListedToJoinIntoOneIntersectionTest extends IntersectionConsolidationTestBase {

  /** indices of the nodes of {@link #twoJunctionsSixtyMetresApart()} */
  private static final int J1 = 0, M = 1, J2 = 2, W = 3, E = 4, N1 = 5, N2 = 6;

  /** the external id of each node of {@link #twoJunctionsSixtyMetresApart()}, by index */
  private static final String[] NAMES = {"J1", "M", "J2", "W", "E", "N1", "N2"};

  /**
   * The two signalised junctions 60 m apart along the road, each node's external id its name, every segment entering a
   * junction one of its approaches
   */
  private void twoJunctionsSixtyMetresApart() {
    node(0, 0);
    node(30, 0);
    node(60, 0);
    node(-85, 0);
    node(145, 0);
    node(0, 85);
    node(60, 85);
    twoWayRoad(J1, M, 30);
    twoWayRoad(M, J2, 30);
    twoWayRoad(W, J1, 85);
    twoWayRoad(J2, E, 85);
    twoWayRoad(N1, J1, 85);
    twoWayRoad(N2, J2, 85);
    for (int index = 0; index < NAMES.length; ++index) {
      nodes.get(index).setExternalId(NAMES[index]);
    }
    signalisedJunctionAt(J1);
    signalisedJunctionAt(J2);
  }

  /**
   * Add the signalised junction J3 40 m north of M to {@link #twoJunctionsSixtyMetresApart()}, with roads west and
   * east of it, its external id J3
   *
   * @return index of J3
   */
  private int junctionNorthOfM() {
    int j3 = node(30, 40);
    twoWayRoad(M, j3, 40);
    twoWayRoad(j3, node(-55, 40), 85);
    twoWayRoad(j3, node(115, 40), 85);
    nodes.get(j3).setExternalId("J3");
    signalisedJunctionAt(j3);
    return j3;
  }

  /**
   * J1 and J2, listed to join, become one spatial intersection. The steps after the grouping run again for it: M lies
   * between them, the links to M are reached only through the intersection, so M joins as a member node. The four
   * segments between J1, M and J2 become internal, the roads from W, N1, N2 and E stay its approaches, and the
   * movements are stored: from W to E the movement passes J1, M and J2, 60 m. J1 and J2 are not connected within the
   * joining reach, and are joined all the same
   * <pre>
   *            N1                 N2
   *            |                  |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join
   * </pre>
   */
  @Test
  public void junctionsListedToJoinBecomeOneIntersectionWithTheNodeBetweenThem() {
    twoJunctionsSixtyMetresApart();

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("J1", "J2"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertEquals(Set.of(nodes.get(J1), nodes.get(M), nodes.get(J2)), Set.copyOf(joined.getMemberNodes()));
    assertEquals(Set.of(segment(J1, M), segment(M, J2), segment(J2, M), segment(M, J1)),
        Set.copyOf(joined.getInternalSegments()));
    assertEquals(Set.of(segment(W, J1), segment(N1, J1), segment(E, J2), segment(N2, J2)),
        Set.copyOf(joined.getApproachSegments()));
    assertEquals(2, result.getReplacedIntersections().size());
    assertEquals(0.060, movementBetween(joined, segment(W, J1), segment(J2, E)).getLengthKm(), 1e-9);
  }

  /**
   * J3 lies 40 m north of M, 50 m from J1 and from J2, beyond the pull-in margin of either on its own, so
   * consolidation alone keeps all three apart. Joined, J1 and J2 form an intersection whose centre is M and whose
   * radius is 30 m. The pull-ins run again for it, and J3 lies within its radius plus the margin, so it is pulled in,
   * with M on its connection
   * <pre>
   *     NW ------- J3 ------- NE         J3-NW and J3-NE 85 m
   *                |
   *                |                     M-J3 40 m
   *            N1  |                N2
   *            |   |                |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join
   * </pre>
   */
  @Test
  public void intersectionJoinedAsListedPullsInAJunctionWithinItsLargerRadius() {
    twoJunctionsSixtyMetresApart();
    int j3 = junctionNorthOfM();

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("J1", "J2"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(j3)));
    assertTrue(joined.hasMemberNode(nodes.get(M)));
    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
  }

  /**
   * As in {@link #intersectionJoinedAsListedPullsInAJunctionWithinItsLargerRadius()}, but J3 and J1 are listed to keep
   * in separate intersections, so J3 is not pulled into the intersection holding J1 and J2
   * <pre>
   *     NW ------- J3 ------- NE         J3 and J1 listed to keep apart
   *                |
   *            N1  |                N2
   *            |   |                |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join
   * </pre>
   */
  @Test
  public void intersectionJoinedAsListedDoesNotPullInAJunctionListedToKeepApart() {
    twoJunctionsSixtyMetresApart();
    int j3 = junctionNorthOfM();

    var result = consolidateWith(settings -> {
      settings.addNodeIdsToJoinIntoOneIntersection(List.of("J1", "J2"), IdMapperType.EXTERNAL_ID);
      settings.addNodeIdsToKeepInSeparateIntersections(List.of("J3", "J1"), IdMapperType.EXTERNAL_ID);
    });

    assertEquals(2, intersections.size());
    assertFalse(intersections.getByMemberNode(nodes.get(J1)).hasMemberNode(nodes.get(j3)));
    assertTrue(intersections.getByMemberNode(nodes.get(J1)).hasMemberNode(nodes.get(J2)));
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /**
   * The divided roads crossing becomes one intersection, and the signalised junction J, 85 m along the eastbound
   * carriageway from D, stays apart. Listing B and J joins those two intersections as a whole: B is a node of the whole
   * crossing, so A, C and D join as part of it, not by being pulled in
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <--- B <----- 8
   *              |      ^              B and J listed to join
   *              v      |
   *    10 -----> C ---> D -----> J      J is node 11 of the crossing, with roads north and south of it, 85 m
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void anyNodeOfAnIntersectionJoinsTheWholeIntersection() {
    dividedRoadsCrossing();
    int j = 11;
    twoWayRoad(j, node(100, 85), 85);
    twoWayRoad(j, node(100, -85), 85);
    List.of(A, B, C, D, j).forEach(this::signalisedJunctionAt);
    nodes.get(B).setExternalId("B");
    nodes.get(j).setExternalId("J");

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("B", "J"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertTrue(List.of(A, B, C, D, j).stream().allMatch(index -> joined.hasMemberNode(nodes.get(index))));
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /**
   * The crossing X lies 20 m south of J1, so the crossing rules join it to J1. Listed to join, J1 and J2 are joined,
   * and X stays a member node of the intersection
   * <pre>
   *            N1                 N2
   *            |                  |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join
   *            |
   *            X                                  J1-X 20 m, X a signalised crossing, X-S 85 m
   *            |
   *            S
   * </pre>
   */
  @Test
  public void crossingAlreadyJoinedStaysWithTheIntersectionJoinedAsListed() {
    twoJunctionsSixtyMetresApart();
    int x = node(0, -20);
    twoWayRoad(J1, x, 20);
    twoWayRoad(x, node(0, -105), 85);
    signalisedCrossingAt(x);

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("J1", "J2"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(x)));
    assertEquals(1, result.getIntersections(CROSSING_JOINED).size());
  }

  /**
   * The crossing X lies 5 m beyond the joining reach along the road from the signalised junction J, so the crossing
   * rules leave it as it is. Listed to join, J and X are joined all the same, although they are not connected within
   * the joining reach either
   * <pre>
   *            N
   *            |
   *   W ------ J -------- X ------ E      J-X 45 m, X a signalised crossing; other arms 85 m; all roads two-way
   *            |                          J and X listed to join
   *            S
   * </pre>
   */
  @Test
  public void crossingListedToJoinBeyondTheJoiningReachIsJoined() {
    double meters = IntersectionConsolidatorSettings.DEFAULT_MAXIMUM_JOINING_REACH_METERS + 5;
    int j = node(0, 0);
    int x = node(meters, 0);
    twoWayRoad(j, x, meters);
    twoWayRoad(x, node(meters + 85, 0), 85);
    twoWayRoad(node(-85, 0), j, 85);
    twoWayRoad(node(0, 85), j, 85);
    twoWayRoad(node(0, -85), j, 85);
    signalisedJunctionAt(j);
    signalisedCrossingAt(x);
    nodes.get(j).setExternalId("J");
    nodes.get(x).setExternalId("X");

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("J", "X"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(j)) && joined.hasMemberNode(nodes.get(x)));
    assertTrue(result.getIntersections(CROSSING_JOINED).isEmpty());
  }

  /**
   * P and Q are 32 m apart and each one's only approach with a signal comes from the other, so consolidation keeps
   * them apart, see {@code IntersectionClusterTest#groupThatWouldLoseEveryApproachWithASignalStaysApart}. Listed to
   * join, they are joined all the same
   * <pre>
   *              N
   *              |
   *   E1 ------> P             P-Q 32 m two-way, with P's approach Q->P and Q's approach P->Q, each with a signal;
   *              |             P and Q listed to join
   *              Q ------> E2
   *              |
   *              S
   * </pre>
   */
  @Test
  public void intersectionThatLosesEveryApproachWithASignalIsJoinedAsListed() {
    int p = node(0, 32);
    int q = node(0, 0);
    var between = twoWayRoad(p, q, 32);
    twoWayRoad(p, node(0, 117), 85);
    oneWayRoad(node(85, 32), p, 85);
    twoWayRoad(q, node(0, -85), 85);
    oneWayRoad(q, node(85, 0), 85);
    signalisedJunctionApproachedOnlyFrom(p, q);
    signalisedJunctionApproachedOnlyFrom(q, p);
    nodes.get(p).setExternalId("P");
    nodes.get(q).setExternalId("Q");
    var stopLines = Set.of(between.getLinkSegmentAb(), between.getLinkSegmentBa());

    consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("P", "Q"), IdMapperType.EXTERNAL_ID), stopLines::contains);

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(p)) && joined.hasMemberNode(nodes.get(q)));
  }

  /**
   * P and Q are 20 m apart, and Q does not earn its place in a group with P, so consolidation keeps them apart, see
   * {@code IntersectionClusterTest#memberThatDoesNotEarnItsPlaceKeepsTheJunctionsApart}. Listed to join, they are
   * joined all the same
   * <pre>
   *                N   NE
   *                ^  /
   *                | v
   *   W <--- P <-> Q        P-Q 20 m two-way; arms 85 m, one-way as drawn; P and Q listed to join
   * </pre>
   */
  @Test
  public void intersectionWithAMemberThatDoesNotEarnItsPlaceIsJoinedAsListed() {
    int p = node(0, 0);
    int q = node(20, 0);
    twoWayRoad(p, q, 20);
    oneWayRoad(p, node(-85, 0), 85);
    oneWayRoad(q, node(20, 85), 85);
    oneWayRoad(node(80.1, 60.1), q, 85);
    signalisedJunctionAt(p);
    signalisedJunctionAt(q);
    nodes.get(p).setExternalId("P");
    nodes.get(q).setExternalId("Q");

    consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("P", "Q"), IdMapperType.EXTERNAL_ID));

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(p)) && joined.hasMemberNode(nodes.get(q)));
  }

  /**
   * The list names J1, M and 404. M is in no intersection, so it has no effect, and no node has the id 404, which is
   * listed as not found. That leaves J1 alone, already in one intersection, so nothing is joined
   * <pre>
   *            N1                 N2
   *            |                  |
   *   W ------ J1 ----- M ------- J2 ------ E     J1, M and 404 listed to join
   * </pre>
   */
  @Test
  public void nodeInNoIntersectionHasNoEffect() {
    twoJunctionsSixtyMetresApart();

    var result = consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("J1", "M", "404"), IdMapperType.EXTERNAL_ID));

    assertEquals(2, intersections.size());
    assertTrue(intersections.stream().allMatch(NonSpatialIntersection.class::isInstance));
    assertEquals(List.of("404"), result.getListedNodeIdsNotFound());
  }

  /**
   * J1 and J2 are listed to join, but also to keep in separate intersections, so they are not joined: both stay as
   * they are
   * <pre>
   *            N1                 N2
   *            |                  |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join and to keep apart
   * </pre>
   */
  @Test
  public void joinRefusedWhenItWouldPutNodesListedToKeepApartIntoOne() {
    twoJunctionsSixtyMetresApart();

    consolidateWith(settings -> {
      settings.addNodeIdsToJoinIntoOneIntersection(List.of("J1", "J2"), IdMapperType.EXTERNAL_ID);
      settings.addNodeIdsToKeepInSeparateIntersections(List.of("J1", "J2"), IdMapperType.EXTERNAL_ID);
    });

    assertEquals(2, intersections.size());
    assertTrue(intersections.stream().allMatch(NonSpatialIntersection.class::isInstance));
  }

  /**
   * The nodes to join may be listed by any kind of id. Listed by their PLANit ids, J1 and J2 join as when listed by
   * their external ids, see {@link #junctionsListedToJoinBecomeOneIntersectionWithTheNodeBetweenThem()}
   * <pre>
   *            N1                 N2
   *            |                  |
   *   W ------ J1 ----- M ------- J2 ------ E     J1 and J2 listed to join by their ids
   * </pre>
   */
  @Test
  public void nodesListedByTheirIdsJoin() {
    twoJunctionsSixtyMetresApart();
    var ids = List.of(String.valueOf(nodes.get(J1).getId()), String.valueOf(nodes.get(J2).getId()));

    consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(ids, IdMapperType.ID));

    var joined = onlySpatialIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(J1)) && joined.hasMemberNode(nodes.get(J2)));
  }
}
