package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.utils.zoning.modifier.ZoningModifier;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAccessLinkSegmentEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAccessZoneEntryEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidAllowedModeEvent;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.zoning.modifier.event.RecreatedZoningEntitiesManagedIdsEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModificationEvent;
import org.goplanit.utils.zoning.modifier.event.ZoningModifierListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The zoning modifier's changes to connectoids: each makes its change and fires one event naming what was lost, and
 * nothing is fired when nothing changes
 *
 * <pre>
 *   0 -----&gt; 1 &lt;----- 2      one-way segments 0-&gt;1 and 2-&gt;1 into node 1, the access node of the
 *                             stop's connectoid; its vehicle stop entry is reached over both and allows bus and car
 * </pre>
 */
public class ZoningModifierLossEventsTest extends LayerTestBase {

  /** zoning holding the stop */
  private Zoning zoning;

  /** the zoning's modifier */
  private ZoningModifier modifier;

  /** allowed by the entry */
  private Mode bus;

  /** allowed by the entry */
  private Mode car;

  /** the stop */
  private TransferZone stop;

  /** the stop's connectoid at node 1 */
  private TransferConnectoid connectoid;

  /** the connectoid's vehicle stop entry */
  private DirectedConnectoidAccessZoneEntry entry;

  /** segment 0-&gt;1 */
  private MacroscopicLinkSegment fromWest;

  /** segment 2-&gt;1 */
  private MacroscopicLinkSegment fromEast;

  /** records the loss events of the zoning */
  private ZoningLossEventRecorder recorder;

  /**
   * One layer with the two one-way segments into node 1, and a zoning with a bus stop whose connectoid is reached over
   * both for bus and car
   */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus, car));
    MacroscopicLinkSegmentType road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(bus, car));
    createNodesOnLine(3);
    link(0, 1, road, null);
    link(2, 1, road, null);
    fromWest = segment(0, 1);
    fromEast = segment(2, 1);

    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    modifier = zoning.getZoningModifier();
    stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, fromWest, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
    entry.addExplicitAllowedMode(bus);
    entry.addExplicitAllowedMode(car);
    recorder = new ZoningLossEventRecorder(zoning);
  }

  /** Removing the connectoid takes it from the zoning whole, its entry staying on it, with one event naming it */
  @Test
  public void removingConnectoidFiresOneEventKeepingItsEntries() {
    assertTrue(modifier.removeConnectoid(connectoid));

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertTrue(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP));
    assertEquals(1, recorder.getHeard().size());
    assertSame(connectoid, ((RemoveConnectoidEvent) recorder.getHeard().get(0)).getRemovedConnectoid());
  }

  /** Removing a connectoid the zoning does not hold changes nothing and fires nothing */
  @Test
  public void removingConnectoidNotHeldFiresNothing() {
    zoning.getTransferConnectoids().remove(connectoid);

    assertFalse(modifier.removeConnectoid(connectoid));
    assertTrue(recorder.getHeard().isEmpty());
  }

  /** Removing the entry leaves the connectoid in the zoning without entries, with one event naming both */
  @Test
  public void removingEntryFiresOneEventConnectoidStays() {
    assertSame(entry, modifier.removeAccessZoneEntry(connectoid, stop, ZoneConnectoidType.PT_VEHICLE_STOP));

    assertFalse(connectoid.hasAccessZoneEntries());
    assertEquals(1, zoning.getTransferConnectoids().size());
    assertEquals(1, recorder.getHeard().size());
    var event = (RemoveConnectoidAccessZoneEntryEvent) recorder.getHeard().get(0);
    assertSame(connectoid, event.getConnectoid());
    assertSame(entry, event.getRemovedAccessZoneEntry());
  }

  /** Removing one access link segment leaves the other on the entry, with one event naming connectoid, entry and
   * segment */
  @Test
  public void removingAccessLinkSegmentFiresOneEventEntryStays() {
    assertTrue(modifier.removeAccessLinkSegment(connectoid, entry, fromEast));

    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertEquals(1, recorder.getHeard().size());
    var event = (RemoveConnectoidAccessLinkSegmentEvent) recorder.getHeard().get(0);
    assertSame(connectoid, event.getConnectoid());
    assertSame(entry, event.getAccessZoneEntry());
    assertSame(fromEast, event.getRemovedAccessLinkSegment());
  }

  /** Removing bus leaves car allowed, with one event naming connectoid, entry and mode */
  @Test
  public void removingAllowedModeFiresOneEvent() {
    assertTrue(modifier.removeExplicitAllowedMode(connectoid, entry, bus));

    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(1, recorder.getHeard().size());
    var event = (RemoveConnectoidAllowedModeEvent) recorder.getHeard().get(0);
    assertSame(connectoid, event.getConnectoid());
    assertSame(entry, event.getAccessZoneEntry());
    assertSame(bus, event.getRemovedMode());
  }

  /** The last explicitly allowed mode is not removed, and nothing is fired for it */
  @Test
  public void lastAllowedModeIsNotRemovedAndFiresNothing() {
    modifier.removeExplicitAllowedMode(connectoid, entry, bus);

    assertFalse(modifier.removeExplicitAllowedMode(connectoid, entry, car));
    assertEquals(Set.of(car), Set.copyOf(entry.getExplicitlyAllowedModes()));
    assertEquals(List.of(RemoveConnectoidAllowedModeEvent.EVENT_TYPE), recorder.getHeardTypes());
  }

  /**
   * A listener registered on the zoning modifier by type for the recreation of the zoning's entity ids hears it when
   * the connectoid ids are recreated, once for the OD and once for the transfer connectoids ("Zoning entity ids
   * recreated")
   */
  @Test
  public void zoningEntityIdsRecreated() {
    var heard = new ArrayList<ZoningModificationEvent>();
    modifier.addListener(new ZoningModifierListener() {
      @Override
      public void onZoningModifierEvent(ZoningModificationEvent event) {
        heard.add(event);
      }

      @Override
      public EventType[] getKnownSupportedEventTypes() {
        return new EventType[] {RecreatedZoningEntitiesManagedIdsEvent.EVENT_TYPE};
      }
    }, RecreatedZoningEntitiesManagedIdsEvent.EVENT_TYPE);

    modifier.recreateConnectoidIds();

    assertEquals(2, heard.size());
  }
}
