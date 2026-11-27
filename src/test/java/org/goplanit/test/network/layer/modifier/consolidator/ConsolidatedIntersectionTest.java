package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITH_SIGNAL_TAGGED_ON_NODE_AHEAD;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.JUNCTION_WITH_FEW_SIGNALISED_ARMS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests what a consolidated intersection is made of: its internal links, the links reached only through it, and the
 * approaches counted as missing
 *
 * @author markr
 */
public class ConsolidatedIntersectionTest extends IntersectionConsolidationTestBase {

  // THE CONSOLIDATED INTERSECTION

  /**
   * A 120 m link joining A and D is not used for scoring, but both its segments become internal segments, and it is
   * listed as longer than the warning length of 100 m
   */
  @Test
  public void longInternalLinkIsInternalAndListed() {
    dividedRoadsCrossing();
    var longLink = twoWayRoad(A, D, 120);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.getInternalSegments().contains(longLink.getLinkSegmentAb()));
    assertTrue(consolidated.getInternalSegments().contains(longLink.getLinkSegmentBa()));
    assertEquals(List.of(longLink), result.getInternalLinksLongerThanWarningLength());
  }

  /** The approach from B into A, with values, becomes an internal segment of the consolidated intersection, which has
   * no approach, and so no approach values, for it */
  @Test
  public void approachMadeInternalHasNoApproachValues() {
    dividedRoadsCrossing();
    var intersectionAtA = signalisedJunctionAt(A);
    List.of(B, C, D).forEach(this::signalisedJunctionAt);
    var fromB = (IntersectionSignalSegment) intersectionAtA.getApproach(segment(B, A));
    fromB.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);

    consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.getInternalSegments().contains(segment(B, A)));
    assertNull(consolidated.getApproach(segment(B, A)));
  }

  /** The approach from N into A, node 4, stays an approach of the consolidated intersection and keeps its values, with
   * how they got their value */
  @Test
  public void approachStayingAnApproachKeepsItsValues() {
    dividedRoadsCrossing();
    var intersectionAtA = signalisedJunctionAt(A);
    List.of(B, C, D).forEach(this::signalisedJunctionAt);
    int north = 4;
    var fromNorth = (IntersectionSignalSegment) intersectionAtA.getApproach(segment(north, A));
    fromNorth.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    fromNorth.setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY);

    consolidate();

    var consolidated = onlySpatialIntersection();
    var kept = (IntersectionSignalSegment) consolidated.getApproach(segment(north, A));
    assertNotNull(kept);
    assertEquals(0.44, kept.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, kept.getGreenRatioOrigin());
    assertEquals(0.95, kept.getSaturationFlowFactor());
    assertEquals(CapacityFactorOrigin.SET_DIRECTLY, kept.getSaturationFlowFactorOrigin());
  }

  // LINKS REACHED ONLY THROUGH THE INTERSECTION

  /**
   * Add a one-way loop to the divided roads crossing that leaves at B and comes back in at D over L1 and L2, with the
   * given segment lengths. L1 and L2 are no part of the cluster: each is linked to one junction only
   * <pre>
   *     A <--- B ---> L1
   *     |      ^      |
   *     v      |      v
   *     C ---> D <--- L2
   * </pre>
   *
   * @param meters lengths of B-L1, L1-L2 and L2-D
   */
  private void loopFromBToD(double... meters) {
    int l1 = node(22, 12);
    int l2 = node(22, 3);
    oneWayRoad(B, l1, meters[0]);
    oneWayRoad(l1, l2, meters[1]);
    oneWayRoad(l2, D, meters[2]);
  }

  /**
   * The loop of 24 m is only reached by driving through the intersection: its segments become internal, its nodes
   * member nodes, and the case is listed with B feeding it
   */
  @Test
  public void linkLeavingAndEnteringTheIntersectionAgainBecomesInternal() {
    dividedRoadsCrossing();
    loopFromBToD(8, 8, 8);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    int l1 = 12;
    int l2 = 13;

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(l1)));
    assertTrue(consolidated.hasMemberNode(nodes.get(l2)));
    assertTrue(consolidated.getInternalSegments().containsAll(
        List.of(segment(B, l1), segment(l1, l2), segment(l2, D))));
    assertFalse(consolidated.getApproachSegments().contains(segment(l2, D)));
    assertEquals(1, result.getLinksReachedOnlyThroughIntersection().size());
    var reachedOnlyThrough = result.getLinksReachedOnlyThroughIntersection().get(0);
    assertSame(segment(l2, D), reachedOnlyThrough.getEnteringSegment());
    assertEquals(List.of(nodes.get(B)), reachedOnlyThrough.getFeedingNodes());
    assertEquals(24, reachedOnlyThrough.getDistanceMeters(), 1e-6);
    assertFalse(reachedOnlyThrough.isWithSignal());
  }

  /**
   * A vehicle signal stands on the middle of the loop: that segment becomes an internal segment with a signal
   */
  @Test
  public void signalOnLinkReachedOnlyThroughTheIntersection() {
    dividedRoadsCrossing();
    loopFromBToD(8, 8, 8);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var signalled = segment(12, 13);

    var result = consolidate(segment -> segment == signalled);

    assertEquals(List.of(signalled), onlySpatialIntersection().getInternalSegmentsWithSignal());
    assertTrue(result.getLinksReachedOnlyThroughIntersection().get(0).isWithSignal());
  }

  /**
   * The loop is 70 m, beyond the reach of 60 m: the segment entering D from it stays an approach. Twice the
   * intersection's radius of 10.6 m reaches less far
   */
  @Test
  public void fedFromBeyondTheReachStaysAnApproach() {
    dividedRoadsCrossing();
    loopFromBToD(30, 10, 30);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    assertTrue(onlyIntersection().getApproachSegments().contains(segment(13, D)));
    assertTrue(result.getLinksReachedOnlyThroughIntersection().isEmpty());
  }

  /**
   * The same 70 m loop with a radius factor of 7: seven times the intersection's radius of 10.6 m reaches 74.2 m,
   * further than the reach of 60 m, so a large intersection, whose roads between its junctions are longer, looks
   * further back. The loop becomes internal
   */
  @Test
  public void fedFromWithinTheRadiusTimesTheFactorBecomesInternal() {
    dividedRoadsCrossing();
    loopFromBToD(30, 10, 30);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidateWith(
        settings -> settings.setMaximumDistanceReachedOnlyThroughIntersectionRadiusFactor(7));

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.getInternalSegments().contains(segment(13, D)));
    assertFalse(consolidated.getApproachSegments().contains(segment(13, D)));
    assertEquals(1, result.getLinksReachedOnlyThroughIntersection().size());
    assertEquals(70, result.getLinksReachedOnlyThroughIntersection().get(0).getDistanceMeters(), 1e-6);
  }

  /**
   * A two-way road ending 20 m away: traffic on it only arrives by turning back, so it starts there and its segment
   * into the junction stays an approach
   */
  @Test
  public void deadEndRoadStaysAnApproach() {
    int junctionNode = node(0, 0);
    twoWayRoad(junctionNode, node(-85, 0), 85);
    twoWayRoad(junctionNode, node(85, 0), 85);
    int deadEnd = node(0, 20);
    twoWayRoad(junctionNode, deadEnd, 20);
    signalisedJunctionAt(junctionNode);

    var result = consolidate();

    assertTrue(onlyIntersection().getApproachSegments().contains(segment(deadEnd, junctionNode)));
    assertTrue(result.getLinksReachedOnlyThroughIntersection().isEmpty());
  }

  // APPROACHES WITHOUT A SIGNAL

  /**
   * A signalised junction placed at a signal's own position, with one approach, from the west. The roads entering from
   * the north and south become approaches without a signal of their own, listed as not on a slip lane, so the junction
   * has few lit arms. A footway entering does not become an approach
   */
  @Test
  public void roadsEnteringASignalisedJunctionBecomeApproachesWithoutASignal() {
    int junctionNode = node(0, 0);
    int west = node(-85, 0);
    int north = node(0, 85);
    int south = node(0, -85);
    twoWayRoad(west, junctionNode, 85);
    twoWayRoad(north, junctionNode, 85);
    twoWayRoad(south, junctionNode, 85);
    link(node(10, 10), junctionNode, 14, true, footway);
    var junction = intersections.getFactory().createNonSpatial(
        nodes.get(junctionNode), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    junction.addNewApproachSegment(segment(west, junctionNode));
    intersections.getFactory().register(junction);

    var result = consolidate();

    assertEquals(Set.of(segment(west, junctionNode), segment(north, junctionNode), segment(south, junctionNode)),
        Set.copyOf(junction.getApproachSegments()));
    assertTrue(junction.hasOwnSignal(segment(west, junctionNode)));
    assertFalse(junction.hasOwnSignal(segment(north, junctionNode)));
    assertFalse(junction.hasOwnSignal(segment(south, junctionNode)));
    assertEquals(Set.of(segment(north, junctionNode), segment(south, junctionNode)),
        Set.copyOf(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE)));
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE).isEmpty());
    assertEquals(List.of(junction), result.getIntersections(JUNCTION_WITH_FEW_SIGNALISED_ARMS));
  }

  /**
   * The junction's signal is tagged on its node, so every road into it is an approach with a signal, and no signal
   * stands on a road to say where cars stop. A crossing K on a side street joins it through T, where the side street
   * meets the main road 10 m before the junction. T becomes a member node, and the main road into T takes over the
   * signal the main road had into the junction: it is the same road, and the junction now starts at T. It is listed as
   * such, not as an approach without a signal
   * <pre>
   *              X
   *              |
   *              K                        crossing on the side street, X-K 85 m, K-T 20 m
   *              |
   *   M <------> T <------> J <------> E  main road M-T 75 m and T-J 10 m, both named Main Road; J-E 85 m
   *                         |
   *                         S             J-S 85 m; all two-way
   * </pre>
   */
  @Test
  public void mainRoadIntoAJoinedNodeTakesOverTheSignalTaggedOnTheJunctionNode() {
    int junctionNode = node(0, 0);
    int t = node(-10, 0);
    int m = node(-85, 0);
    twoWayRoad(m, t, 75).setName("Main Road");
    twoWayRoad(t, junctionNode, 10).setName("Main Road");
    twoWayRoad(junctionNode, node(85, 0), 85);
    twoWayRoad(junctionNode, node(0, -85), 85);
    int k = node(-10, 20);
    twoWayRoad(t, k, 20);
    twoWayRoad(k, node(-10, 105), 85);
    signalisedJunctionAt(junctionNode);
    signalisedCrossingAt(k);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(t)));
    assertTrue(consolidated.hasOwnSignal(segment(m, t)));
    assertEquals(List.of(segment(m, t)), result.getApproaches(ADDED_WITH_SIGNAL_TAGGED_ON_NODE_AHEAD));
    assertFalse(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).contains(segment(m, t)));
  }

  /**
   * The road from the west passes a signalised junction U and reaches the junction J at X, 20 m before it, where a
   * slip lane leaves it over S. Only the road from X onto J is an approach. The slip lane becomes an approach without a
   * signal of its own, listed as on a slip lane, since it leaves a road leading into the same junction. U lies within
   * the further reach allowed to salvage an approach without a signal, but a slip lane is expected to have no signal,
   * so U is not pulled in
   * <pre>
   *     W ---> U ---> X -------> J
   *                    \        ^
   *                     -> S --/         slip lane X-S-J, one way; U-X 22 m, so U and J are no cluster
   * </pre>
   */
  @Test
  public void slipLaneEnteringASignalisedJunctionIsListedAsOnASlipLane() {
    int junctionNode = node(0, 0);
    int west = node(-85, 0);
    int u = node(-42, 0);
    int x = node(-20, 0);
    int s = node(-10, -8);
    oneWayRoad(west, u, 43);
    oneWayRoad(u, x, 22);
    oneWayRoad(x, junctionNode, 20);
    oneWayRoad(x, s, 13);
    oneWayRoad(s, junctionNode, 13);
    var junction = intersections.getFactory().createNonSpatial(
        nodes.get(junctionNode), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    junction.addNewApproachSegment(segment(x, junctionNode));
    intersections.getFactory().register(junction);
    signalisedJunctionAt(u);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL).isEmpty());
    assertTrue(junction.getApproachSegments().contains(segment(s, junctionNode)));
    assertFalse(junction.hasOwnSignal(segment(s, junctionNode)));
    assertEquals(List.of(segment(s, junctionNode)), result.getApproaches(ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE));
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }

  /**
   * As in {@link #slipLaneEnteringASignalisedJunctionIsListedAsOnASlipLane}, but X-S-J is two-way for cars. A slip lane
   * is one-way for motorised modes, so this is a side road: S-J becomes an approach without a signal of its own, listed
   * as not on a slip lane
   */
  @Test
  public void twoWayRoadIsNotASlipLane() {
    int junctionNode = node(0, 0);
    int west = node(-85, 0);
    int x = node(-20, 0);
    int s = node(-10, -8);
    oneWayRoad(west, x, 65);
    oneWayRoad(x, junctionNode, 20);
    twoWayRoad(x, s, 13);
    twoWayRoad(s, junctionNode, 13);
    var junction = intersections.getFactory().createNonSpatial(
        nodes.get(junctionNode), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    junction.addNewApproachSegment(segment(x, junctionNode));
    intersections.getFactory().register(junction);

    var result = consolidate();

    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE).isEmpty());
    assertEquals(List.of(segment(s, junctionNode)), result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE));
  }

  /**
   * A signalised junction with three road approaches, two of them without a signal of their own, has few lit arms
   */
  @Test
  public void junctionWithFewLitArmsIsListed() {
    int junctionNode = node(0, 0);
    int west = node(-85, 0);
    int north = node(0, 85);
    int south = node(0, -85);
    twoWayRoad(west, junctionNode, 85);
    twoWayRoad(north, junctionNode, 85);
    twoWayRoad(south, junctionNode, 85);
    var junction = intersections.getFactory().createNonSpatial(
        nodes.get(junctionNode), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    nodes.get(junctionNode).streamEntrySegments().forEach(
        segment -> junction.addNewApproachSegment((MacroscopicLinkSegment) segment));
    junction.swapApproachToNewSignalOrUnsignalised(segment(north, junctionNode), false);
    junction.swapApproachToNewSignalOrUnsignalised(segment(south, junctionNode), false);
    intersections.getFactory().register(junction);

    var result = consolidate();

    assertEquals(List.of(junction), result.getIntersections(JUNCTION_WITH_FEW_SIGNALISED_ARMS));
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }
}
