package org.goplanit.test.network;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the link utilities that check the segments of a link in both directions
 *
 * @author markr
 */
public class MacroscopicLinkUtilsTest extends LayerTestBase {

  private Mode pedestrian;

  private MacroscopicLinkSegmentType carOnly;
  private MacroscopicLinkSegmentType busOnly;
  private MacroscopicLinkSegmentType walkOnly;

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);

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
   * A link has a segment matching when the segment in either direction matches. A link with a segment in one direction
   * only is checked on that segment
   */
  @Test
  public void hasSegmentMatchingChecksBothDirections() {
    /*
     * 0 --walkOnly--> 1 <--walkOnly--> 2 --carOnly--> 3
     *   <--carOnly---
     */
    createNodes(4);
    var carOneWayBack = link(0, 1, walkOnly, carOnly);
    var walkBothWays = link(1, 2, walkOnly, walkOnly);
    var carOneWay = link(2, 3, carOnly, null);

    var permitsMotorisedRoadMode = MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode();
    assertTrue(MacroscopicLinkUtils.hasSegmentMatching(carOneWayBack, permitsMotorisedRoadMode));
    assertFalse(MacroscopicLinkUtils.hasSegmentMatching(walkBothWays, permitsMotorisedRoadMode));
    assertTrue(MacroscopicLinkUtils.hasSegmentMatching(carOneWay, permitsMotorisedRoadMode));
    assertFalse(MacroscopicLinkUtils.hasSegmentMatching(carOneWay, segment -> segment.isModeAllowed(pedestrian)));
  }

  /**
   * A link a car or a bus may use, in at least one direction, is selected. A link only for walking is not
   */
  @Test
  public void permitsMotorisedRoadModeOnAnySegmentSelectsLinksACarOrBusMayUseInEitherDirection() {
    /*
     * 0 --walkOnly--> 1 <--walkOnly--> 2
     *   <--carOnly--- |
     *              busOnly
     *                 v
     *                 3
     */
    createNodes(4);
    link(0, 1, walkOnly, carOnly);
    link(1, 2, walkOnly, walkOnly);
    link(1, 3, busOnly, null);

    assertEquals(2, nodes.get(1).getNumberOfEdges(MacroscopicLinkUtils.permitsMotorisedRoadModeOnAnySegment()));
  }
}
