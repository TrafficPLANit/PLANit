package org.goplanit.test.network.layer.macroscopic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests what is worked out for a movement of a spatial intersection from the network inside it: the internal segments
 * it passes, and the modes it is banned for. Link lengths are given in metres
 * <pre>
 *    0 ---> 1 ---------> 2 ---> 3      1, 2 and 4 are member nodes; 0-1 is the approach, 2-3 and 1-5 are exits
 *           | \          ^
 *           v   \        |             internal: 1-2 10 m, 1-4 20 m, 4-2 20 m
 *           5     -> 4 --
 * </pre>
 * The movement east goes from 0-1 to 2-3, 10 m over 1-2; the movement at the corner goes from 0-1 to 1-5, at member
 * node 1. Every segment allows cars and buses
 *
 * @author markr
 */
public class SpatialMovementUtilsTest extends LayerTestBase {

  private MacroscopicNetwork network;
  private Mode car;
  private Mode bus;
  private MacroscopicLinkSegmentType carAndBus;
  private SpatialIntersection junction;
  private SpatialMovement east;
  private SpatialMovement corner;

  /** records what the utilities log */
  private static class RecordingHandler extends Handler {
    final List<LogRecord> records = new ArrayList<>();
    @Override public void publish(LogRecord record) { records.add(record); }
    @Override public void flush() {}
    @Override public void close() {}
  }

  /**
   * Join node a to node b one way, with a segment allowing cars and buses
   *
   * @param a index of node A
   * @param b index of node B
   * @param meters length of the link
   */
  private void oneWayRoad(int a, int b, double meters) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), meters / 1000, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true).setLinkSegmentType(carAndBus);
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, bus));
    carAndBus = layer.getLinkSegmentTypes().getFactory().registerNew("carAndBus", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carAndBus, 50, List.of(car, bus));

    createNodes(6);
    oneWayRoad(0, 1, 85);
    oneWayRoad(1, 2, 10);
    oneWayRoad(1, 4, 20);
    oneWayRoad(4, 2, 20);
    oneWayRoad(2, 3, 85);
    oneWayRoad(1, 5, 85);

    junction = spatialJunctionOver(1, 2, 4);
    junction.addNewApproachSegment(segment(0, 1));
    junction.addNewInternalSegment(segment(1, 2));
    junction.addNewInternalSegment(segment(1, 4));
    junction.addNewInternalSegment(segment(4, 2));
    east = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(2, 3), 0.010);
    corner = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(1, 5), 0);
    junction.addMovement(east);
    junction.addMovement(corner);
    layer.getIntersections().getFactory().register(junction);
  }

  // WITHOUT CHANGES TO THE NETWORK

  /**
   * On the network as built: the movement east passes the internal segment of its shortest connection, 1-2, not the
   * longer way over 4; the movement at the corner enters and leaves at member node 1, so it passes no internal
   * segment. The connection of the movement east is 10 m: a warning is logged when it is expected to be at most 5 m,
   * none when at most 15 m, and the internal segments passed are found either way. With nothing banned, neither cars
   * nor buses are banned from the movement east
   */
  @Test
  public void internalSegmentsPassedAndBansWithoutChangesToTheNetwork() {
    assertEquals(List.of(segment(1, 2)), SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, east));
    assertTrue(SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, corner).isEmpty());

    var logger = Logger.getLogger(SpatialMovementUtils.class.getCanonicalName());
    var handler = new RecordingHandler();
    logger.addHandler(handler);
    try {
      assertEquals(List.of(segment(1, 2)), SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, east, 5));
      assertEquals(1, handler.records.stream().filter(record -> record.getLevel() == Level.WARNING).count());

      handler.records.clear();
      assertEquals(List.of(segment(1, 2)), SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, east, 15));
      assertTrue(handler.records.isEmpty());
    } finally {
      logger.removeHandler(handler);
    }

    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, car));
    assertFalse(SpatialMovementUtils.isBanned(layer, junction, east, bus, true, true));
    assertFalse(SpatialMovementUtils.isBannedForEveryMode(layer, junction, east, true, true));
    assertTrue(SpatialMovementUtils.findBannedModes(layer, junction, east, true, true).isEmpty());
  }

  // INTERNAL SEGMENTS PASSED

  /**
   * The turn from the approach onto 1-2 is banned for every mode. The movement east still passes 1-2: its length is
   * measured with banned turns ignored, and so are the internal segments it passes
   */
  @Test
  public void internalSegmentsPassedIgnoreBannedTurns() {
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2));

    assertEquals(List.of(segment(1, 2)), SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, east));
  }

  /** No mode may use the internal segments leaving member node 1 any more, so the movement east is not connected */
  @Test
  public void movementNoLongerConnectedPassesNothing() {
    var closed = layer.getLinkSegmentTypes().getFactory().registerNew("closed", 1800, 180);
    segment(1, 2).setLinkSegmentType(closed);
    segment(1, 4).setLinkSegmentType(closed);

    assertNull(SpatialMovementUtils.findInternalSegmentsPassed(layer, junction, east));
  }

  // BANS

  /**
   * The turn onto 1-2 is banned for buses, but buses can still go over 4, so the movement east is not banned for them
   */
  @Test
  public void modeBannedOnOneConnectionIsNotImplicitlyBanned() {
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2), List.of(bus));

    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, bus));
  }

  /**
   * The turns onto 1-2 and onto 1-4 are banned for buses, so the movement east is banned implicitly for buses, while
   * it has no direct ban. It is not banned for cars
   */
  @Test
  public void modeBannedOnEveryConnectionIsImplicitlyBanned() {
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2), List.of(bus));
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 4), List.of(bus));

    assertTrue(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, bus));
    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, car));
    assertFalse(east.hasDirectBan());
    assertTrue(SpatialMovementUtils.isBanned(layer, junction, east, bus, false, true));
    assertFalse(SpatialMovementUtils.isBanned(layer, junction, east, bus, true, false));
    assertEquals(Set.of(bus), SpatialMovementUtils.findBannedModes(layer, junction, east, true, true));
  }

  /**
   * The internal segments leaving member node 1 are for cars only, so the movement east is banned implicitly for buses
   */
  @Test
  public void internalSegmentsClosedToAModeBanItImplicitly() {
    var carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));
    segment(1, 2).setLinkSegmentType(carOnly);
    segment(1, 4).setLinkSegmentType(carOnly);

    assertTrue(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, bus));
    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, car));
  }

  /**
   * Link 1-2 is two-way inside the junction, and the turns from the approach onto the exit 1-5 and onto 1-4 are banned
   * for buses. So a bus could make the movement at the corner only by driving along 1-2 and straight back, which no
   * driver does inside a junction: the movement is banned implicitly for buses. Cars may still make it
   */
  @Test
  public void goingBackAlongAnInnerLinkDoesNotGetRoundABan() {
    var back = layer.getLinkSegments().getFactory().registerNew(segment(1, 2).getParent(), false, true);
    back.setLinkSegmentType(carAndBus);
    junction.addNewInternalSegment(back);
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 5), List.of(bus));
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 4), List.of(bus));

    assertTrue(SpatialMovementUtils.isImplicitlyBanned(layer, junction, corner, bus));
    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, corner, car));
  }

  /** A direct ban for cars bans the movement east for cars, but not implicitly; it is not banned for buses */
  @Test
  public void directBanBansTheModesItNames() {
    east.banDirectly(List.of(car));

    assertTrue(SpatialMovementUtils.isBanned(layer, junction, east, car, true, false));
    assertFalse(SpatialMovementUtils.isBanned(layer, junction, east, car, false, true));
    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, car));
    assertEquals(Set.of(car), SpatialMovementUtils.findBannedModes(layer, junction, east, true, true));
  }

  /** The exit 2-3 is for cars only: buses have no access to the movement east, so it is not banned for them */
  @Test
  public void modeWithoutAccessToTheExitIsNotBanned() {
    var carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));
    segment(2, 3).setLinkSegmentType(carOnly);

    assertFalse(SpatialMovementUtils.isImplicitlyBanned(layer, junction, east, bus));
    assertFalse(SpatialMovementUtils.isBanned(layer, junction, east, bus, true, true));
    assertTrue(SpatialMovementUtils.findBannedModes(layer, junction, east, true, true).isEmpty());
  }

  /**
   * The turns onto 1-2 and onto 1-4 are banned for every mode, so the movement east is banned for every mode,
   * implicitly; it has no direct ban
   */
  @Test
  public void movementBannedOnEveryConnectionForEveryModeIsBannedForEveryMode() {
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2));
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 4));

    assertTrue(SpatialMovementUtils.isBannedForEveryMode(layer, junction, east, true, true));
    assertFalse(SpatialMovementUtils.isBannedForEveryMode(layer, junction, east, true, false));
    assertEquals(Set.of(car, bus), SpatialMovementUtils.findBannedModes(layer, junction, east, true, true));
  }
}
