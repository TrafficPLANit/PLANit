package org.goplanit.test.network;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the link segment utilities that select segments by the modes allowed on them, and that find the segments a
 * segment continues on beyond the node it arrives at
 *
 * @author markr
 */
public class MacroscopicLinkSegmentUtilsTest extends LayerTestBase {

  private MacroscopicLinkSegmentType carOnly;
  private MacroscopicLinkSegmentType busOnly;
  private MacroscopicLinkSegmentType walkOnly;

  /**
   * The segment to follow next in a direction: the only one onward, see
   * {@link MacroscopicLinkSegmentUtils#findOnlySegmentOnward}
   *
   * @param inTrafficDirection true to go in the traffic direction, false to go back against it
   * @return the segment to follow next, null when there is none
   */
  private static UnaryOperator<MacroscopicLinkSegment> onlySegmentOnward(boolean inTrafficDirection) {
    return segment -> MacroscopicLinkSegmentUtils.findOnlySegmentOnward(segment, inTrafficDirection, other -> true);
  }

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    var pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, bus, pedestrian));

    carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));
    busOnly = layer.getLinkSegmentTypes().getFactory().registerNew("busOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(busOnly, 50, List.of(bus));
    walkOnly = layer.getLinkSegmentTypes().getFactory().registerNew("walkOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(walkOnly, 5, List.of(pedestrian));
  }

  /**
   * A car or a bus may use a segment, so a motorised road mode is allowed on it. A segment only for walking is not
   * selected
   */
  @Test
  public void permitsMotorisedRoadModeSelectsSegmentsACarOrBusMayUse() {
    /*
     * 0 --carOnly--> 1 --busOnly--> 2 --walkOnly--> 3
     */
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, busOnly, null);
    link(2, 3, walkOnly, null);

    var permitsMotorisedRoadMode = MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode();
    assertTrue(permitsMotorisedRoadMode.test(segment(0, 1)));
    assertTrue(permitsMotorisedRoadMode.test(segment(1, 2)));
    assertFalse(permitsMotorisedRoadMode.test(segment(2, 3)));
  }

  /**
   * The continuations of a segment leave the node it arrives at and meet the condition. Going back along the link
   * arrived on is never one. A segment arriving at a dead end has none
   */
  @Test
  public void findContinuationsLeavesOutTheLinkArrivedOnAndSegmentsFailingTheCondition() {
    /*
     * 0 <--carOnly--> 1 --carOnly--> 2
     *                 |
     *              walkOnly
     *                 v
     *                 3
     *
     * arriving along 0->1, the segment 1->0 goes back along the link arrived on
     */
    createNodes(4);
    link(0, 1, carOnly, carOnly);
    link(1, 2, carOnly, null);
    link(1, 3, walkOnly, null);

    var arriving = segment(0, 1);
    assertEquals(List.of(segment(1, 2)),
        MacroscopicLinkSegmentUtils.findContinuations(arriving, MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode()));
    assertEquals(Set.of(segment(1, 2), segment(1, 3)),
        Set.copyOf(MacroscopicLinkSegmentUtils.findContinuations(arriving, segment -> true)));
    assertTrue(MacroscopicLinkSegmentUtils.findContinuations(segment(1, 3), segment -> true).isEmpty());
  }

  /**
   * The predecessors of a segment enter the node it leaves and meet the condition. Coming from the link it leaves along
   * is never one. A segment leaving a node nothing enters has none
   */
  @Test
  public void findPredecessorsLeavesOutTheLinkLeftAlongAndSegmentsFailingTheCondition() {
    /*
     * 0 --carOnly--> 1 <--carOnly--> 2
     *                ^
     *             walkOnly
     *                |
     *                3
     *
     * leaving along 1->2, the segment 2->1 comes from the link left along
     */
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, carOnly, carOnly);
    link(3, 1, walkOnly, null);

    var leaving = segment(1, 2);
    assertEquals(List.of(segment(0, 1)),
        MacroscopicLinkSegmentUtils.findPredecessors(leaving, MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode()));
    assertEquals(Set.of(segment(0, 1), segment(3, 1)),
        Set.copyOf(MacroscopicLinkSegmentUtils.findPredecessors(leaving, segment -> true)));
    assertTrue(MacroscopicLinkSegmentUtils.findPredecessors(segment(0, 1), segment -> true).isEmpty());
  }

  /**
   * The only segment onward in a direction meets the condition. Where several go on, or none, there is no such segment
   */
  @Test
  public void findOnlySegmentOnwardGivesNoneWhereSeveralOrNoneGoOn() {
    /*
     * 0 --carOnly--> 1 --carOnly--> 2
     *                |
     *             walkOnly
     *                v
     *                3
     */
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, carOnly, null);
    link(1, 3, walkOnly, null);

    assertEquals(segment(1, 2), MacroscopicLinkSegmentUtils.findOnlySegmentOnward(
        segment(0, 1), true, MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode()));
    assertNull(MacroscopicLinkSegmentUtils.findOnlySegmentOnward(segment(0, 1), true, segment -> true));
    assertNull(MacroscopicLinkSegmentUtils.findOnlySegmentOnward(segment(1, 2), true, segment -> true));
    assertEquals(segment(0, 1),
        MacroscopicLinkSegmentUtils.findOnlySegmentOnward(segment(1, 2), false, segment -> true));
  }

  /**
   * Following segments one after the other, the distance at each node reached adds the length of each segment
   * followed. It stops with the first answer given, and gives none when the node with the answer lies beyond the
   * distance or nothing is left to follow. Against the traffic it reaches the upstream node of each segment instead
   */
  @Test
  public void followSegmentsUntilAnsweredStopsWithTheFirstAnswerWithinTheDistance() {
    /*
     * 0 --carOnly--> 1 --carOnly--> 2 --carOnly--> 3
     *
     * each link 1000 m long, starting half way along 0->1, so 500 m before node 1
     */
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, carOnly, null);
    link(2, 3, carOnly, null);

    var metersAtNodeReached = new LinkedHashMap<Node, Double>();
    assertNull(MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(segment(0, 1), true, 500, 10000,
        (segment, meters) -> {
          metersAtNodeReached.put(segment.getDownstreamNode(), meters);
          return null;
        }, onlySegmentOnward(true)));
    assertEquals(Map.of(nodes.get(1), 500.0, nodes.get(2), 1500.0, nodes.get(3), 2500.0), metersAtNodeReached);
    assertEquals(List.of(nodes.get(1), nodes.get(2), nodes.get(3)), List.copyOf(metersAtNodeReached.keySet()));

    BiFunction<MacroscopicLinkSegment, Double, MacroscopicLinkSegment> answerArrivingAtNode2 =
        (segment, meters) -> segment.getDownstreamNode() == nodes.get(2) ? segment : null;
    assertEquals(segment(1, 2), MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(
        segment(0, 1), true, 500, 1500, answerArrivingAtNode2, onlySegmentOnward(true)));
    assertNull(MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(
        segment(0, 1), true, 500, 1400, answerArrivingAtNode2, onlySegmentOnward(true)));

    assertEquals(segment(0, 1), MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(
        segment(2, 3), false, 1000, 10000,
        (segment, meters) -> segment.getUpstreamNode() == nodes.get(0) ? segment : null, onlySegmentOnward(false)));
  }

  /**
   * Following segments round a loop, it stops when it gets back to a node already reached, without an answer
   */
  @Test
  public void followSegmentsUntilAnsweredStopsWhenItGetsBackToANodeAlreadyReached() {
    /*
     * 0 --carOnly--> 1
     * ^              |
     * carOnly     carOnly
     * |              v
     * 3 <--carOnly-- 2
     */
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, carOnly, null);
    link(2, 3, carOnly, null);
    link(3, 0, carOnly, null);

    var nodesReached = new ArrayList<Node>();
    assertNull(MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(segment(0, 1), true, 1000, 100000,
        (segment, meters) -> {
          nodesReached.add(segment.getDownstreamNode());
          return null;
        }, onlySegmentOnward(true)));
    assertEquals(List.of(nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(0)), nodesReached);
  }

}
