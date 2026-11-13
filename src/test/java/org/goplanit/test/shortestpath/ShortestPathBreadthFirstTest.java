package org.goplanit.test.shortestpath;

import java.util.List;
import java.util.Set;

import org.goplanit.algorithms.shortest.ShortestPathBreadthFirst;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the shortest search over the part of a graph around where it starts, along edges in either direction and
 * along edge segments with turns that can be refused. Costs are link lengths in km
 *
 * @author markr
 */
public class ShortestPathBreadthFirstTest extends LayerTestBase {

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
  }

  /** one-way link from a to b of the given length */
  private void oneWay(int a, int b, double lengthKm) {
    oneWay(a, b).setLengthKm(lengthKm);
  }

  /** the length of an edge */
  private static double lengthKm(Edge edge) {
    return edge.getLengthKm();
  }

  /** the length of the link of a segment */
  private static double lengthKm(MacroscopicLinkSegment segment) {
    return segment.getParent().getLengthKm();
  }

  // ALONG EDGES

  /**
   * The shortest way from 0 to 1 passes 2, over two links of 1 km, rather than the direct link of 3 km. Link 1-2 is
   * one-way towards 2 and is still used against its direction
   * <pre>
   *   0 -------3------> 1
   *    \1             /
   *     --> 2 <--1---       2-1 one-way from 1 to 2
   * </pre>
   */
  @Test
  public void alongEdgesFindsTheShortestEvenOverMoreLinks() {
    createNodes(3);
    oneWay(0, 1, 3);
    oneWay(0, 2, 1);
    oneWay(1, 2, 1);

    var result = ShortestPathBreadthFirst.<Node>executeAlongEdges(
        nodes.get(0), ShortestPathBreadthFirstTest::lengthKm, node -> true, Double.POSITIVE_INFINITY);

    assertEquals(2, result.getCostToReach(nodes.get(1)), 1e-9);
    assertEquals(List.of(nodes.get(0), nodes.get(2), nodes.get(1)), result.getEntitiesOnConnectionTo(nodes.get(1)));
  }

  /**
   * A vertex beyond the maximum cost is not reached, and a vertex that may not be reached is neither reached nor
   * passed
   * <pre>
   *   0 --1--> 1 --1--> 2 --1--> 3
   * </pre>
   */
  @Test
  public void alongEdgesStopsAtTheMaximumCostAndAtVerticesItMayNotReach() {
    createNodes(4);
    oneWay(0, 1, 1);
    oneWay(1, 2, 1);
    oneWay(2, 3, 1);

    var withinTwo = ShortestPathBreadthFirst.<Node>executeAlongEdges(
        nodes.get(0), ShortestPathBreadthFirstTest::lengthKm, node -> true, 2);
    assertEquals(Set.of(nodes.get(0), nodes.get(1), nodes.get(2)), withinTwo.getReached());
    assertFalse(withinTwo.isReached(nodes.get(3)));
    assertEquals(Double.MAX_VALUE, withinTwo.getCostToReach(nodes.get(3)));

    var notThroughOne = ShortestPathBreadthFirst.<Node>executeAlongEdges(
        nodes.get(0), ShortestPathBreadthFirstTest::lengthKm, node -> node != nodes.get(1), Double.POSITIVE_INFINITY);
    assertEquals(Set.of(nodes.get(0)), notThroughOne.getReached());
  }

  /** An edge with an infinite cost is not followed */
  @Test
  public void alongEdgesSkipsEdgesWithInfiniteCost() {
    createNodes(2);
    oneWay(0, 1, 1);

    var result = ShortestPathBreadthFirst.<Node>executeAlongEdges(
        nodes.get(0), edge -> Double.POSITIVE_INFINITY, node -> true, Double.POSITIVE_INFINITY);

    assertEquals(Set.of(nodes.get(0)), result.getReached());
  }

  /**
   * The shortest connections over the links that may be followed, in either direction: link 0-1 may not be followed,
   * so 1 is reached from 0 over 2, 2 km, though both links on that way are one-way towards 0. With 1.5 km as the
   * furthest, 1 is not reached at all
   * <pre>
   *   0 -------1------> 1      0-1 not followed
   *    ^              /
   *     \--- 2 <-----          1-2 and 2-0 one-way, 1 km each
   * </pre>
   */
  @Test
  public void shortestConnectionsFollowOnlyTheLinksAllowedInEitherDirection() {
    createNodes(3);
    var direct = oneWay(0, 1);
    direct.setLengthKm(1);
    oneWay(1, 2, 1);
    oneWay(2, 0, 1);

    var result = MacroscopicNetworkLayerUtils.findShortestConnections(
        nodes.get(0), edge -> edge != direct, node -> true, 5000);
    var withinOneAndAHalf = MacroscopicNetworkLayerUtils.findShortestConnections(
        nodes.get(0), edge -> edge != direct, node -> true, 1500);

    assertEquals(2000, result.getCostToReach(nodes.get(1)), 1e-6);
    assertTrue(result.getEntitiesOnConnectionTo(nodes.get(1)).contains(nodes.get(2)));
    assertEquals(Set.of(nodes.get(0), nodes.get(2)), withinOneAndAHalf.getReached());
  }

  // ALONG EDGE SEGMENTS

  /**
   * From the end of segment 0-1, both 1-2-4 and 1-3-4 lead to 4, but the turn from 2-4 onto 4-5 is refused. The
   * segment 4-5 is still reached, over 3, at the cost of 1-3 and 3-4. A search that looks at each vertex once would
   * miss it, having reached 4 over 2 first
   * <pre>
   *            2
   *          /   \
   *   0 --> 1     4 --> 5      all one-way to the right, 1 km each
   *          \   /
   *            3
   * </pre>
   */
  @Test
  public void alongEdgeSegmentsFindsAWayAroundARefusedTurn() {
    createNodes(6);
    oneWay(0, 1, 1);
    oneWay(1, 2, 1);
    oneWay(1, 3, 1);
    oneWay(2, 4, 1);
    oneWay(3, 4, 1);
    oneWay(4, 5, 1);
    var refusedFrom = segment(2, 4);
    var refusedOnto = segment(4, 5);

    var result = ShortestPathBreadthFirst.<MacroscopicLinkSegment>executeAlongEdgeSegments(segment(0, 1), true,
        (from, onto) -> !(from == refusedFrom && onto == refusedOnto), segment -> true,
        ShortestPathBreadthFirstTest::lengthKm, Double.POSITIVE_INFINITY);

    assertEquals(0, result.getCostToReach(segment(0, 1)), 1e-9);
    assertEquals(0, result.getCostToReach(segment(1, 2)), 1e-9);
    assertEquals(2, result.getCostToReach(segment(4, 5)), 1e-9);
    assertEquals(List.of(segment(0, 1), segment(1, 3), segment(3, 4), segment(4, 5)),
        result.getEntitiesOnConnectionTo(segment(4, 5)));
  }

  /**
   * A segment reached that may not be continued on ends the search there
   * <pre>
   *   0 --> 1 --> 2 --> 3
   * </pre>
   */
  @Test
  public void alongEdgeSegmentsStopsAtSegmentsItMayNotContinueOn() {
    createNodes(4);
    oneWay(0, 1, 1);
    oneWay(1, 2, 1);
    oneWay(2, 3, 1);
    var last = segment(1, 2);

    var result = ShortestPathBreadthFirst.<MacroscopicLinkSegment>executeAlongEdgeSegments(segment(0, 1), true,
        (from, onto) -> true, segment -> segment != last, ShortestPathBreadthFirstTest::lengthKm,
        Double.POSITIVE_INFINITY);

    assertEquals(Set.of(segment(0, 1), segment(1, 2)), result.getReached());
  }

  // AGAINST EDGE SEGMENTS

  /**
   * Back from the start of segment 4-5, both 2-4 and 3-4 come into 4, but coming from 2-4 onto 4-5 is refused. The
   * segment 0-1 is still reached, back over 3, at the cost of 3-4 and 1-3; 1-2 is not, since it only leads onto 2-4
   * <pre>
   *            2
   *          /   \
   *   0 --> 1     4 --> 5      all one-way to the right, 1 km each
   *          \   /
   *            3
   * </pre>
   */
  @Test
  public void againstEdgeSegmentsFindsAWayAroundARefusedTurn() {
    createNodes(6);
    oneWay(0, 1, 1);
    oneWay(1, 2, 1);
    oneWay(1, 3, 1);
    oneWay(2, 4, 1);
    oneWay(3, 4, 1);
    oneWay(4, 5, 1);
    var refusedFrom = segment(2, 4);
    var refusedOnto = segment(4, 5);

    var result = ShortestPathBreadthFirst.<MacroscopicLinkSegment>executeAlongEdgeSegments(segment(4, 5), false,
        (from, onto) -> !(from == refusedFrom && onto == refusedOnto), segment -> true,
        ShortestPathBreadthFirstTest::lengthKm, Double.POSITIVE_INFINITY);

    assertEquals(0, result.getCostToReach(segment(4, 5)), 1e-9);
    assertEquals(0, result.getCostToReach(segment(3, 4)), 1e-9);
    assertEquals(2, result.getCostToReach(segment(0, 1)), 1e-9);
    assertFalse(result.getReached().contains(segment(2, 4)));
    assertFalse(result.getReached().contains(segment(1, 2)));
    assertEquals(List.of(segment(4, 5), segment(3, 4), segment(1, 3), segment(0, 1)),
        result.getEntitiesOnConnectionTo(segment(0, 1)));
  }

  /**
   * A segment reached that may not be continued on ends the search back there
   * <pre>
   *   0 --> 1 --> 2 --> 3
   * </pre>
   */
  @Test
  public void againstEdgeSegmentsStopsAtSegmentsItMayNotContinueOn() {
    createNodes(4);
    oneWay(0, 1, 1);
    oneWay(1, 2, 1);
    oneWay(2, 3, 1);
    var last = segment(1, 2);

    var result = ShortestPathBreadthFirst.<MacroscopicLinkSegment>executeAlongEdgeSegments(segment(2, 3), false,
        (from, onto) -> true, segment -> segment != last, ShortestPathBreadthFirstTest::lengthKm,
        Double.POSITIVE_INFINITY);

    assertEquals(Set.of(segment(2, 3), segment(1, 2)), result.getReached());
  }
}
