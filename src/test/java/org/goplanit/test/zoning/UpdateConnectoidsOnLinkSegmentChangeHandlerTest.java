package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.ModifiedLinkSegmentTypes;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.handler.UpdateConnectoidsOnLinkSegmentChangeHandler;
import org.goplanit.zoning.modifier.event.handler.UpdateConnectoidsOnLinkSegmentChangeResult.ConnectoidAccessLoss;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping connectoids in step with the access link segments they use, when a link segment is removed or loses modes
 * when its type changes
 *
 * <pre>
 *   0 -----&gt; 1 &lt;----- 2      one-way segments 0-&gt;1 and 2-&gt;1, both entering node 1, the access node of
 *                             the stop's connectoid; both allow bus, car and pedestrian
 * </pre>
 */
public class UpdateConnectoidsOnLinkSegmentChangeHandlerTest extends LayerTestBase {

  private Zoning zoning;

  private Mode bus;

  private Mode car;

  private Mode pedestrian;

  private MacroscopicLinkSegment fromWest;

  private MacroscopicLinkSegment fromEast;

  private TransferZone stop;

  private UpdateConnectoidsOnLinkSegmentChangeHandler handler;

  /** a connectoid of the stop at node 1, with a vehicle stop entry reached over 0-&gt;1 for the given modes, none for
   * every mode */
  private TransferConnectoid vehicleStopConnectoid(Mode... modes) {
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, fromWest, ZoneConnectoidType.PT_VEHICLE_STOP);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    for (var mode : modes) {
      entry.addExplicitAllowedMode(mode);
    }
    return connectoid;
  }

  /** start keeping the connectoids in step, once they are all created */
  private void keepConnectoidsInStep() {
    handler = new UpdateConnectoidsOnLinkSegmentChangeHandler(zoning);
    layer.getLayerModifier().addListener((DirectedGraphModifierListener) handler);
    layer.getLayerModifier().addListener((LinkSegmentTypeModifierListener) handler);
  }

  /** withdraw a mode's access from a link segment, as restricting a mode to its connected subnetworks does */
  private void withdraw(MacroscopicLinkSegment segment, Mode mode) {
    MacroscopicNetworkLayerUtils.withdrawModeAccess(layer, new ModifiedLinkSegmentTypes(), segment, mode);
  }

  /** the kinds of loss the handler stated, in order */
  private List<ConnectoidAccessLoss.Kind> lossKinds() {
    return handler.getResult().getAccessLosses().stream().map(ConnectoidAccessLoss::getKind)
        .collect(Collectors.toList());
  }

  /**
   * One layer with the two one-way segments into node 1, of a type allowing bus, car and pedestrian, and a zoning with
   * one bus stop
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
    createNodesOnLine(3);
    link(0, 1, road, null);
    link(2, 1, road, null);
    fromWest = segment(0, 1);
    fromEast = segment(2, 1);

    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
  }

  /** bus withdrawn from the only access link segment of a bus entry takes the entry, the pedestrian entry stays */
  @Test
  public void entryOfWithdrawnModeOnlyAccessLinkSegmentIsRemoved() {
    var connectoid = vehicleStopConnectoid(bus);
    connectoid.createUndirectedAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS_EGRESS)
        .addExplicitAllowedMode(pedestrian);
    keepConnectoidsInStep();

    withdraw(fromWest, bus);

    assertEquals(1, zoning.getTransferConnectoids().size());
    assertFalse(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP));
    assertTrue(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS_EGRESS));
    assertEquals(List.of(ConnectoidAccessLoss.Kind.ACCESS_ZONE_ENTRY), lossKinds());
  }

  /** a connectoid left without any entry is removed */
  @Test
  public void connectoidLeftWithoutEntryIsRemoved() {
    vehicleStopConnectoid(bus);
    keepConnectoidsInStep();

    withdraw(fromWest, bus);

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(ConnectoidAccessLoss.Kind.CONNECTOID), lossKinds());
  }

  /** bus withdrawn from one of two access link segments takes that segment, bus staying on the other */
  @Test
  public void accessLinkSegmentWithoutItsModesIsRemovedOthersStay() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    keepConnectoidsInStep();

    withdraw(fromWest, bus);

    assertFalse(entry.hasAccessLinkSegment(fromWest));
    assertTrue(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.isModeAllowed(bus));
    assertEquals(List.of(ConnectoidAccessLoss.Kind.ACCESS_LINK_SEGMENT), lossKinds());
  }

  /** bus withdrawn where the entry also allows car, which the segment still allows, takes bus from the entry only */
  @Test
  public void withdrawnModeIsRemovedFromEntryOtherModesStay() {
    var connectoid = vehicleStopConnectoid(bus, car);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    keepConnectoidsInStep();

    withdraw(fromWest, bus);

    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(List.of(ConnectoidAccessLoss.Kind.ALLOWED_MODE), lossKinds());
  }

  /** an entry for bus and car reached over 0-&gt;1 and 2-&gt;1, where 2-&gt;1 allows no bus: 0-&gt;1 losing both bus and
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
    keepConnectoidsInStep();

    layer.getLayerModifier().changeLinkSegmentType(fromWest, footway);

    assertFalse(entry.hasAccessLinkSegment(fromWest));
    assertTrue(entry.hasAccessLinkSegment(fromEast));
    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(
        List.of(ConnectoidAccessLoss.Kind.ACCESS_LINK_SEGMENT, ConnectoidAccessLoss.Kind.ALLOWED_MODE), lossKinds());
  }

  /** a removed access link segment is taken out of the entry, which goes with its last one */
  @Test
  public void removedAccessLinkSegmentIsTakenOut() {
    var connectoid = vehicleStopConnectoid(bus);
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    keepConnectoidsInStep();

    layer.getLayerModifier().removeEdgeSegment(fromEast);
    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));

    layer.getLayerModifier().removeEdgeSegment(fromWest);
    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(
        List.of(ConnectoidAccessLoss.Kind.ACCESS_LINK_SEGMENT, ConnectoidAccessLoss.Kind.CONNECTOID), lossKinds());
  }

  /** an entry allowing every mode is left as it is when its access link segment loses bus, since what it allows
   * follows from the link segment */
  @Test
  public void entryAllowingEveryModeIsLeftAsItIs() {
    var connectoid = vehicleStopConnectoid();
    var entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    keepConnectoidsInStep();

    withdraw(fromWest, bus);

    assertTrue(entry.getExplicitlyAllowedModes().isEmpty());
    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertTrue(lossKinds().isEmpty());
  }
}
