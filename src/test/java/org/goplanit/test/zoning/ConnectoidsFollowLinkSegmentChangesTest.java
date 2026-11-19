package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.ModifiedLinkSegmentTypes;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.RemoveConnectoidAccessLinkSegmentEvent;
import org.goplanit.zoning.modifier.event.RemoveConnectoidAccessZoneEntryEvent;
import org.goplanit.zoning.modifier.event.RemoveConnectoidAllowedModeEvent;
import org.goplanit.zoning.modifier.event.RemoveConnectoidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A zoning made on a network keeps its transfer connectoids in step with the access link segments they use, when a link
 * segment is removed or loses modes when its type changes, reporting each loss once, at the level it stopped
 *
 * <pre>
 *   0 -----&gt; 1 &lt;----- 2 &lt;----- 3      one-way segments 0-&gt;1 (id 0), 3-&gt;2 (id 1) and 2-&gt;1 (id 2); 0-&gt;1 and 2-&gt;1
 *                                      enter node 1, the access node of the stop's connectoid; all allow bus, car and
 *                                      pedestrian
 * </pre>
 */
public class ConnectoidsFollowLinkSegmentChangesTest extends LayerTestBase {

  /** zoning made on the network */
  private Zoning zoning;

  /** a mode of the road type */
  private Mode bus;

  /** a mode of the road type */
  private Mode car;

  /** a mode of the road type */
  private Mode pedestrian;

  /** segment 0-&gt;1 into node 1 */
  private MacroscopicLinkSegment fromWest;

  /** segment 2-&gt;1 into node 1 */
  private MacroscopicLinkSegment fromEast;

  /** the stop */
  private TransferZone stop;

  /** records the loss events of the zoning */
  private ZoningLossEventRecorder recorder;

  /**
   * A connectoid of the stop at node 1, with a vehicle stop entry reached over 0-&gt;1 for the given modes, none for
   * every mode
   *
   * @param modes the entry allows explicitly
   * @return the connectoid
   */
  private TransferConnectoid vehicleStopConnectoid(Mode... modes) {
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, fromWest, ZoneConnectoidType.PT_VEHICLE_STOP);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    for (var mode : modes) {
      entry.addExplicitAllowedMode(mode);
    }
    return connectoid;
  }

  /**
   * Withdraw a mode's access from a link segment, as restricting a mode to its connected subnetworks does
   *
   * @param segment to withdraw the mode from
   * @param mode to withdraw
   */
  private void withdraw(MacroscopicLinkSegment segment, Mode mode) {
    MacroscopicNetworkLayerUtils.withdrawModeAccess(layer, new ModifiedLinkSegmentTypes(), segment, mode);
  }

  /**
   * The types of the loss events heard so far
   *
   * @return types in the order heard
   */
  private List<EventType> lossEventTypes() {
    return recorder.getHeardTypes();
  }

  /**
   * Move the ends at node 1 of the links of the given segments, in one call, to a new node at (1, 1)
   *
   * @param segments whose links to move
   * @return the new node
   */
  private Node moveEndsAtNodeOneToANewNode(MacroscopicLinkSegment... segments) {
    createNodes(1);
    var newNode = nodes.get(nodes.size() - 1);
    newNode.setPosition(PlanitJtsUtils.createPoint(1, 1));
    var links = Stream.of(segments).map(MacroscopicLinkSegment::getParent).collect(Collectors.toList());
    layer.getLayerModifier().moveLinkEndsAtNode(links, nodes.get(1), newNode, PlanitJtsCrsUtils.CARTESIANCRS);
    return newNode;
  }

  /**
   * The only transfer connectoid at a node
   *
   * @param node to look at
   * @return the connectoid
   */
  private TransferConnectoid onlyConnectoidAt(Node node) {
    var atNode = zoning.getTransferConnectoids().getByAccessNode(node);
    assertEquals(1, atNode.size());
    return atNode.get(0);
  }

  /**
   * One layer with the three one-way segments, of a type allowing bus, car and pedestrian, and a zoning made on the
   * network with one bus stop
   */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus, car, pedestrian));
    MacroscopicLinkSegmentType road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(bus, car, pedestrian));
    createNodesOnLine(4);
    link(0, 1, road, null);
    link(3, 2, road, null);
    link(2, 1, road, null);
    fromWest = segment(0, 1);
    fromEast = segment(2, 1);

    zoning = new Zoning(token, network);
    stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    recorder = new ZoningLossEventRecorder(zoning);
  }

  /** Bus withdrawn from the only access link segment of a bus entry takes the entry, the pedestrian entry stays */
  @Test
  public void entryOfWithdrawnModeOnlyAccessLinkSegmentIsRemoved() {
    var connectoid = vehicleStopConnectoid(bus);
    connectoid.createUndirectedAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS_EGRESS)
        .addExplicitAllowedMode(pedestrian);

    withdraw(fromWest, bus);

    assertEquals(1, zoning.getTransferConnectoids().size());
    assertFalse(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP));
    assertTrue(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS_EGRESS));
    assertEquals(List.of(RemoveConnectoidAccessZoneEntryEvent.EVENT_TYPE), lossEventTypes());
  }

  /** A connectoid left without any entry is removed, reported as the connectoid removed only */
  @Test
  public void connectoidLeftWithoutEntryIsRemoved() {
    vehicleStopConnectoid(bus);

    withdraw(fromWest, bus);

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(RemoveConnectoidEvent.EVENT_TYPE), lossEventTypes());
  }

  /** Bus withdrawn from one of two access link segments takes that segment, bus staying on the other */
  @Test
  public void accessLinkSegmentWithoutItsModesIsRemovedOthersStay() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);

    withdraw(fromWest, bus);

    assertFalse(entry.hasAccessLinkSegment(fromWest));
    assertTrue(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.isModeAllowed(bus));
    assertEquals(List.of(RemoveConnectoidAccessLinkSegmentEvent.EVENT_TYPE), lossEventTypes());
  }

  /** Bus withdrawn where the entry also allows car, which the segment still allows, takes bus from the entry only */
  @Test
  public void withdrawnModeIsRemovedFromEntryOtherModesStay() {
    var connectoid = vehicleStopConnectoid(bus, car);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);

    withdraw(fromWest, bus);

    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(List.of(RemoveConnectoidAllowedModeEvent.EVENT_TYPE), lossEventTypes());
  }

  /** An entry for bus and car reached over 0-&gt;1 and 2-&gt;1, where 2-&gt;1 allows no bus: 0-&gt;1 losing both bus and
   * car at once is taken out, and bus goes from the entry with it, since 2-&gt;1 does not allow it */
  @Test
  public void modeOnlyTheRemovedAccessLinkSegmentAllowedIsRemovedFromEntry() {
    var connectoid = vehicleStopConnectoid(bus, car);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    withdraw(fromEast, bus);
    MacroscopicLinkSegmentType footway =
        layer.getLinkSegmentTypes().getFactory().registerNew("footway", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(footway, 5, List.of(pedestrian));
    var lossesBefore = lossEventTypes().size();

    layer.getLayerModifier().changeLinkSegmentType(fromWest, footway);

    assertFalse(entry.hasAccessLinkSegment(fromWest));
    assertTrue(entry.hasAccessLinkSegment(fromEast));
    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(List.of(RemoveConnectoidAccessLinkSegmentEvent.EVENT_TYPE, RemoveConnectoidAllowedModeEvent.EVENT_TYPE),
        lossEventTypes().subList(lossesBefore, lossEventTypes().size()));
  }

  /** A removed access link segment is taken out of the entry, and the connectoid goes with its last one */
  @Test
  public void removedAccessLinkSegmentIsTakenOut() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);

    layer.getLayerModifier().removeEdgeSegment(fromEast);
    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));

    layer.getLayerModifier().removeEdgeSegment(fromWest);
    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(RemoveConnectoidAccessLinkSegmentEvent.EVENT_TYPE, RemoveConnectoidEvent.EVENT_TYPE),
        lossEventTypes());
  }

  /** An entry allowing every mode is left as it is when its access link segment loses bus, since what it allows
   * follows from the link segment */
  @Test
  public void entryAllowingEveryModeIsLeftAsItIs() {
    var connectoid = vehicleStopConnectoid();
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);

    withdraw(fromWest, bus);

    assertTrue(entry.getExplicitlyAllowedModes().isEmpty());
    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertTrue(lossEventTypes().isEmpty());
  }

  /** After the link segment ids are renumbered, removing 2-&gt;1 still takes it out of the entry, 0-&gt;1 staying */
  @Test
  public void removalAfterRenumberingTakesTheSegmentOut() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    layer.getLayerModifier().removeEdgeSegment(segment(3, 2));
    layer.getLayerModifier().recreateManagedIdEntities();

    layer.getLayerModifier().removeEdgeSegment(fromEast);

    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertEquals(List.of(RemoveConnectoidAccessLinkSegmentEvent.EVENT_TYPE), lossEventTypes());
  }

  /** A zoning and its shallow copy both follow the network; a removed access link segment of their shared connectoid
   * is taken out once, reported once, and nothing fails */
  @Test
  public void sharedConnectoidIsChangedOnce() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    var copy = zoning.shallowClone();
    var copyRecorder = new ZoningLossEventRecorder(copy);

    layer.getLayerModifier().removeEdgeSegment(fromEast);

    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertEquals(1, lossEventTypes().size() + copyRecorder.getHeardTypes().size());
  }

  /** The links of 0-&gt;1 and 2-&gt;1 moved together from node 1 to a new node take the connectoid reached over both
   * along, with its entries as they were, and nothing is lost */
  @Test
  public void connectoidReachedOverSeveralMovedLinksMovesWithThem() {
    var connectoid = vehicleStopConnectoid(bus);
    var otherStop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    connectoid.createDirectedAccessZoneEntry(otherStop, ZoneConnectoidType.PT_VEHICLE_STOP, fromEast);

    var newNode = moveEndsAtNodeOneToANewNode(fromWest, fromEast);

    assertSame(newNode, connectoid.getReferenceVertex());
    assertEquals(1, zoning.getTransferConnectoids().size());
    assertEquals(List.of(fromWest),
        connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    assertEquals(List.of(fromEast),
        connectoid.getAsDirectedAccessZoneEntry(otherStop, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    assertTrue(lossEventTypes().isEmpty());
  }

  /** Only the link of 0-&gt;1 moved to a new node splits the connectoid also reached over 2-&gt;1: the stop's entry over
   * 0-&gt;1 goes to a new connectoid at the new node, the other stop's entry over 2-&gt;1 stays, and nothing is lost */
  @Test
  public void connectoidAlsoReachedOverALinkLeftAtTheNodeIsSplit() {
    var connectoid = vehicleStopConnectoid(bus);
    connectoid.setExternalId("stop position");
    var otherStop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    connectoid.createDirectedAccessZoneEntry(otherStop, ZoneConnectoidType.PT_VEHICLE_STOP, fromEast);

    var newNode = moveEndsAtNodeOneToANewNode(fromWest);

    assertSame(nodes.get(1), connectoid.getReferenceVertex());
    assertFalse(connectoid.hasAccessZoneEntry(stop));
    assertEquals(List.of(fromEast),
        connectoid.getAsDirectedAccessZoneEntry(otherStop, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    var split = onlyConnectoidAt(newNode);
    assertEquals("stop position", split.getExternalId());
    var movedEntry = split.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    assertEquals(List.of(fromWest), movedEntry.getAccessLinkSegments());
    assertEquals(Set.of(bus), Set.copyOf(movedEntry.getExplicitlyAllowedModes()));
    assertFalse(split.hasAccessZoneEntry(otherStop));
    assertTrue(lossEventTypes().isEmpty());
  }

  /** Only the link of 0-&gt;1 moved to a new node splits the stop's entry reached over 0-&gt;1 and 2-&gt;1 into two:
   * one over 2-&gt;1 at node 1, one over 0-&gt;1 at the new node */
  @Test
  public void entryReachedOverAMovedLinkAndALinkLeftAtTheNodeIsSplitInTwo() {
    var connectoid = vehicleStopConnectoid(bus);
    connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP).addAccessLinkSegment(fromEast);

    var newNode = moveEndsAtNodeOneToANewNode(fromWest);

    assertEquals(List.of(fromEast),
        connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    assertEquals(List.of(fromWest), onlyConnectoidAt(newNode)
        .getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    assertTrue(lossEventTypes().isEmpty());
  }
}
