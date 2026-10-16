package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.Test;

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
   * The loop is 70 m, beyond the reach of 50 m: the segment entering D from it stays an approach
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

  // COUNTING MISSING APPROACHES

  /**
   * An intersection placed at a signal's own position with one approach, from the west. The roads entering from the
   * north and south are counted as missing approaches, not added; a footway entering is not counted
   */
  @Test
  public void intersectionAtASignalsOwnPositionKeepsItsOneApproach() {
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
    junction.addApproachSegment(segment(west, junctionNode));
    intersections.getFactory().register(junction);

    var result = consolidate();

    assertEquals(List.of(segment(west, junctionNode)), junction.getApproachSegments());
    assertEquals(Set.of(segment(north, junctionNode), segment(south, junctionNode)),
        Set.copyOf(result.getMissingApproachSegments()));
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
        segment -> junction.addApproachSegment((MacroscopicLinkSegment) segment));
    junction.setApproachSegmentHasOwnSignal(segment(north, junctionNode), false);
    junction.setApproachSegmentHasOwnSignal(segment(south, junctionNode), false);
    intersections.getFactory().register(junction);

    var result = consolidate();

    assertEquals(List.of(junction), result.getJunctionsWithFewLitArms());
    assertTrue(result.getMissingApproachSegments().isEmpty());
  }
}
