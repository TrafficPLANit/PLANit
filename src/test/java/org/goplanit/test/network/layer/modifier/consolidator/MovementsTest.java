package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the movements consolidation stores for each spatial intersection: one for each approach and exit its internal
 * segments connect for a motorised road or rail mode, or for any mode when active modes are included, banned or not,
 * with the length of the shortest connection
 *
 * @author markr
 */
public class MovementsTest extends IntersectionConsolidationTestBase {

  /**
   * The divided roads crossing, see {@link #dividedRoadsCrossing()}, its approach from the north a street pedestrians
   * may use too, a footway leaving the corner A, and a signalised junction at each of its four nodes, with only the
   * roads approaches
   * <pre>
   *       4
   *       |  street, car and pedestrian
   *   F   |
   *     \ |
   *       A ---- B       F-A footway, 14 m
   *       |      |
   *       C ---- D
   * </pre>
   *
   * @return the footway segment leaving A
   */
  private MacroscopicLinkSegment dividedRoadsCrossingWithStreetFromNorthAndFootwayFromCorner() {
    dividedRoadsCrossing();
    var street = layer.getLinkSegmentTypes().getFactory().registerNew("street", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(street, 50, List.of(car, pedestrian));
    segment(4, A).setLinkSegmentType(street);
    int footwayEnd = node(-10, 25);
    link(A, footwayEnd, 14, true, footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionWithRoadApproachesAt);
    return segment(A, footwayEnd);
  }

  /**
   * A car entering the divided roads crossing from the north, at A, turns left to the east over the internal segments
   * A-C and C-D, 30 m together. Every approach reaches the four exits, so 16 movements are stored, each listed in the
   * result, none with a direct ban. The approach from the north and the exit to the west meet at member node A, the
   * corner of the crossing: the intersection still refers to a movement between them, of length 0
   */
  @Test
  public void movementsThroughDividedRoadsCrossingAreStored() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertEquals(16, consolidated.getMovements().size());
    assertEquals(16, layer.getSpatialMovements().size());
    assertEquals(16, result.getStoredSpatialMovements().size());
    var leftTurn = movementBetween(consolidated, segment(4, A), segment(D, 11));
    assertEquals(0.030, leftTurn.getLengthKm(), 1e-9);
    assertTrue(consolidated.getMovements().stream().noneMatch(SpatialMovement::hasDirectBan));

    var corner = movementBetween(onlySpatialIntersection(), segment(4, A), segment(A, 9));
    assertEquals(0, corner.getLengthKm(), 1e-9);
  }

  /**
   * The turn from A-C onto C-D is banned for every mode, and it is the only connection from the northern approach to
   * the eastern exit. That movement is stored all the same, without a direct ban, so all 16 movements are there. The
   * banned turn stays at its node, and the movement is banned implicitly for cars, the only mode on the roads
   */
  @Test
  public void movementBlockedByABannedTurnInsideTheJunctionIsStillStored() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D));

    consolidate();

    var consolidated = onlySpatialIntersection();
    assertEquals(16, consolidated.getMovements().size());
    var blocked = movementBetween(consolidated, segment(4, A), segment(D, 11));
    assertFalse(blocked.hasDirectBan());
    assertSame(ban, layer.getBannedTurns().get(ban.getId()));
    assertEquals(Set.of(car), bannedModes(consolidated, blocked));
  }

  /**
   * The roads allow cars and buses, and the turn from A-C onto C-D is banned for buses only. Searching the internal
   * segments from the northern approach while respecting banned turns, cars reach the eastern exit and buses do not,
   * so that movement is banned implicitly for buses only
   */
  @Test
  public void banForBusesOnlyStopsOnlyTheSearchForBuses() {
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer.registerSupportedMode(bus);
    var carAndBus = layer.getLinkSegmentTypes().getFactory().registerNew("carAndBus", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carAndBus, 50, List.of(car, bus));
    dividedRoadsCrossing();
    layer.getLinkSegments().forEach(segment -> segment.setLinkSegmentType(carAndBus));
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D), List.of(bus));
    var north = segment(4, A);
    var east = segment(D, 11);

    consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(MacroscopicNetworkLayerUtils.findExitsReachedThroughSpatialIntersection(
        layer, consolidated, north, car, true).containsKey(east));
    assertFalse(MacroscopicNetworkLayerUtils.findExitsReachedThroughSpatialIntersection(
        layer, consolidated, north, bus, true).containsKey(east));
    assertEquals(Set.of(bus), bannedModes(consolidated, movementBetween(consolidated, north, east)));
  }

  /**
   * An intersection with one member node stores no movements: every turn there is allowed unless banned
   */
  @Test
  public void singleNodeJunctionStoresNoMovements() {
    int junctionNode = node(0, 0);
    twoWayRoad(junctionNode, node(-85, 0), 85);
    twoWayRoad(junctionNode, node(85, 0), 85);
    twoWayRoad(junctionNode, node(0, 85), 85);
    var junction = signalisedJunctionAt(junctionNode);

    var result = consolidate();

    assertSame(junction, onlyNonSpatialIntersection());
    assertTrue(layer.getSpatialMovements().isEmpty());
    assertTrue(result.getStoredSpatialMovements().isEmpty());
  }

  /**
   * By default movements are for motorised road and rail modes only. The approach from the north is a street
   * pedestrians may use too, and a footway leaves the corner A, but no car can get from that approach onto the
   * footway, so no movement is stored there: the 16 movements of the crossing stay
   */
  @Test
  public void movementNoMotorisedModeCanMakeIsNotStored() {
    var footwayFromCorner = dividedRoadsCrossingWithStreetFromNorthAndFootwayFromCorner();

    consolidate();

    var consolidated = onlySpatialIntersection();
    assertEquals(16, consolidated.getMovements().size());
    assertTrue(consolidated.getMovements().stream()
        .noneMatch(movement -> movement.getLinkSegmentTo() == footwayFromCorner));
  }

  /**
   * With active modes included in movements, the walk from the street in the north onto the footway leaving the corner
   * A is a movement too, besides the 16 of the crossing
   */
  @Test
  public void movementOnlyActiveModeCanMakeIsStoredWhenIncluded() {
    var footwayFromCorner = dividedRoadsCrossingWithStreetFromNorthAndFootwayFromCorner();

    consolidateWith(settings -> settings.setIncludeActiveModesInMovements(true));

    var consolidated = onlySpatialIntersection();
    assertEquals(17, consolidated.getMovements().size());
    assertNotNull(movementBetween(consolidated, segment(4, A), footwayFromCorner));
  }
}
