package org.goplanit.test.network;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;
import org.goplanit.utils.network.layer.physical.LinkUtils;
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
public class LinkUtilsTest extends LayerTestBase {

  private Mode pedestrian;

  private MacroscopicLinkSegmentType carOnly;
  private MacroscopicLinkSegmentType busOnly;
  private MacroscopicLinkSegmentType walkOnly;
  private MacroscopicLinkSegmentType tramOnly;
  private MacroscopicLinkSegmentType trainOnly;
  private Mode tram;

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    tram = network.getModes().getFactory().registerNew(PredefinedModeType.TRAM);
    var train = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, bus, pedestrian, tram, train));

    carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));
    busOnly = layer.getLinkSegmentTypes().getFactory().registerNew("busOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(busOnly, 50, List.of(bus));
    walkOnly = layer.getLinkSegmentTypes().getFactory().registerNew("walkOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(walkOnly, 5, List.of(pedestrian));
    tramOnly = layer.getLinkSegmentTypes().getFactory().registerNew("tramOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(tramOnly, 50, List.of(tram));
    trainOnly = layer.getLinkSegmentTypes().getFactory().registerNew("trainOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(trainOnly, 80, List.of(train));
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

    var permitsMotorisedRoadMode = LinkSegmentUtils.permitsMotorisedRoadMode();
    assertTrue(LinkUtils.hasSegmentMatching(carOneWayBack, permitsMotorisedRoadMode));
    assertFalse(LinkUtils.hasSegmentMatching(walkBothWays, permitsMotorisedRoadMode));
    assertTrue(LinkUtils.hasSegmentMatching(carOneWay, permitsMotorisedRoadMode));
    assertFalse(LinkUtils.hasSegmentMatching(carOneWay, segment -> segment.isModeAllowed(pedestrian)));
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

    assertEquals(2, nodes.get(1).getNumberOfEdges(LinkUtils.permitsMotorisedRoadModeOnAnySegment()));
  }

  /**
   * A link a tram may use, in at least one direction, is selected when the condition asks for a tram or light rail
   * mode. A link only for cars or walking is not
   */
  @Test
  public void permitsModeMatchingOnAnySegmentSelectsLinksAMatchingModeMayUseInEitherDirection() {
    /*
     * 0 --walkOnly--> 1 <--carOnly--> 2
     *   <--tramOnly-- |
     *              tramOnly
     *                 v
     *                 3
     */
    createNodes(4);
    link(0, 1, walkOnly, tramOnly);
    link(1, 2, carOnly, carOnly);
    link(1, 3, tramOnly, null);

    assertTrue(tram.isTramOrLightRailMode());
    assertFalse(pedestrian.isTramOrLightRailMode());
    assertEquals(2, nodes.get(1).getNumberOfEdges(
        LinkUtils.permitsModeMatchingOnAnySegment(Mode::isTramOrLightRailMode)));
  }

  /**
   * A tram track meets a road where a link a car may use and a link only a tram may use share a node. A link that
   * carries a car one way and a tram the other is a road, not a track
   */
  @Test
  public void tramOrLightRailTrackMeetsRoadWhereARoadLinkAndATrackLinkShareANode() {
    /*
     * 0 --carOnly--> 1 --tramOnly--> 2        1: a road and a track meet
     *
     * 3 --carOnly--> 4 --carOnly--> 5         4: the link to 5 carries a tram back, on the road
     *                  <--tramOnly--
     */
    createNodes(6);
    link(0, 1, carOnly, null);
    link(1, 2, tramOnly, null);
    link(3, 4, carOnly, null);
    link(4, 5, carOnly, tramOnly);

    assertTrue(LinkUtils.isWhereTramOrLightRailTrackMeetsRoad(nodes.get(1)));
    assertFalse(LinkUtils.isWhereTramOrLightRailTrackMeetsRoad(nodes.get(0)));
    assertFalse(LinkUtils.isWhereTramOrLightRailTrackMeetsRoad(nodes.get(2)));
    assertFalse(LinkUtils.isWhereTramOrLightRailTrackMeetsRoad(nodes.get(4)));
  }

  /**
   * A mode uses a node when one of its links allows it, in either direction. So no motorised road or rail mode uses a
   * node whose links only allow walking, and a link that allows a car, a tram or a train in one direction only uses its
   * nodes all the same
   */
  @Test
  public void usedByAModeWhenOneOfItsLinksAllowsItInEitherDirection() {
    /*
     * 0 --walkOnly--> 1 <--walkOnly--> 2 --walkOnly--> 3        0 and 1: only walking
     *                                    <--carOnly---          2: a car back from 3
     *
     * 4 --walkOnly--> 5 --tramOnly--> 6                         4: only walking; 5: a tram on to 6
     *
     * 7 --walkOnly--> 8 --trainOnly--> 9                        8: a train on to 9
     */
    createNodes(10);
    link(0, 1, walkOnly, null);
    link(1, 2, walkOnly, walkOnly);
    link(2, 3, walkOnly, carOnly);
    link(4, 5, walkOnly, null);
    link(5, 6, tramOnly, null);
    link(7, 8, walkOnly, null);
    link(8, 9, trainOnly, null);

    assertTrue(tram.isMotorisedRoadOrRailMode());
    assertFalse(pedestrian.isMotorisedRoadOrRailMode());
    assertTrue(LinkUtils.isUsedByModeMatching(nodes.get(3), Mode::isMotorisedRoadMode));
    assertFalse(LinkUtils.isUsedByModeMatching(nodes.get(1), Mode::isMotorisedRoadMode));
    assertTrue(LinkUtils.isUsedByModeMatching(nodes.get(6), Mode::isTramOrLightRailMode));
    assertTrue(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(0)));
    assertTrue(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(1)));
    assertFalse(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(2)));
    assertTrue(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(4)));
    assertFalse(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(5)));
    assertFalse(LinkUtils.isUsedByNoMotorisedRoadOrRailMode(nodes.get(8)));
  }

  /**
   * Two links belong to the same road when they share their external id, as two pieces of one road of the source do,
   * or a name that is not blank. Links without either, or with blank names, never do
   */
  @Test
  public void sameRoadBySharedExternalIdOrName() {
    /*
     * 0 --- 1 --- 2 --- 3 --- 4 --- 5      0-1 and 1-2: external id 10; 2-3 and 3-4: named High Street;
     *                                      4-5: no external id, blank name
     */
    createNodes(6);
    var first = link(0, 1, carOnly, carOnly);
    var second = link(1, 2, carOnly, carOnly);
    var third = link(2, 3, carOnly, carOnly);
    var fourth = link(3, 4, carOnly, carOnly);
    var fifth = link(4, 5, carOnly, carOnly);
    first.setExternalId("10");
    second.setExternalId("10");
    third.setName("High Street");
    fourth.setName("High Street");
    fifth.setName(" ");

    assertTrue(LinkUtils.isSameExternalIdOrName(first, second));
    assertTrue(LinkUtils.isSameExternalIdOrName(third, fourth));
    assertFalse(LinkUtils.isSameExternalIdOrName(second, third));
    assertFalse(LinkUtils.isSameExternalIdOrName(fifth, fifth));
  }
}
