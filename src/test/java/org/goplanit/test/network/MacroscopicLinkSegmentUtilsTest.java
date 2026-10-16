package org.goplanit.test.network;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
