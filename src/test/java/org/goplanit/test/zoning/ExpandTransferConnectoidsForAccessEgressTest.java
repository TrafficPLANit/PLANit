package org.goplanit.test.zoning;

import org.goplanit.test.LayerTestBase;
import org.goplanit.converter.zoning.ZoningConverterUtils;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expanding the existing connectoids of a transfer zone with access and egress for travellers: access is travel
 * towards the zone, so a mode arriving at the connectoid's node can use it for access; egress is travel away from the
 * zone, so a mode leaving the node can use it for egress
 */
public class ExpandTransferConnectoidsForAccessEgressTest extends LayerTestBase {

  /** zoning holding the transfer zones and connectoids */
  private Zoning zoning;

  /** the active mode to expand the connectoids with */
  private Mode pedestrian;

  /** segment type only pedestrians may use */
  private MacroscopicLinkSegmentType walkOnly;

  /** segment type only trains may use */
  private MacroscopicLinkSegmentType railOnly;

  /**
   * A layer supporting pedestrians and trains, with a walk only and a rail only segment type, four nodes, and a zoning
   */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    var train = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(pedestrian, train));

    walkOnly = layer.getLinkSegmentTypes().getFactory().registerNew("walkOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(walkOnly, 5, List.of(pedestrian));
    railOnly = layer.getLinkSegmentTypes().getFactory().registerNew("railOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(railOnly, 80, List.of(train));

    createNodes(4);
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
  }

  /** a stop's node only entered on foot: pedestrians can arrive there for access, but cannot leave for egress */
  @Test
  public void segmentEnteringStopNodeGivesAccessOnly() {
    var walkIn = link(0, 1, walkOnly, null).getLinkSegmentAb();
    var stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, walkIn, ZoneConnectoidType.PT_VEHICLE_STOP);

    var added = ZoningConverterUtils.expandTransferConnectoidsWithEligibleUndirectedAccessEgressEntries(
        stop, Set.of(connectoid), Set.of(), Set.of(pedestrian));

    assertTrue(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.ZONE_ACCESS));
    assertFalse(connectoid.hasAccessZoneEntry(stop, ZoneConnectoidType.ZONE_EGRESS));
    assertEquals(Set.of(pedestrian), added.first());
    assertTrue(added.second().isEmpty());
  }

  /** a connectoid no active mode reaches does not keep the next one, on a street, from being expanded */
  @Test
  public void connectoidWithoutEligibleSegmentDoesNotStopTheOthers() {
    var rail = link(2, 3, railOnly, null).getLinkSegmentAb();
    var walk = twoWay(0, 1, walkOnly).getLinkSegmentAb();
    var station = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.STATION, true);
    var onRail = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        station, true, rail, ZoneConnectoidType.PT_VEHICLE_STOP);
    var onStreet = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        station, true, walk, ZoneConnectoidType.PT_VEHICLE_STOP);

    var added = ZoningConverterUtils.expandTransferConnectoidsWithEligibleUndirectedAccessEgressEntries(
        station, new LinkedHashSet<>(List.of(onRail, onStreet)), Set.of(), Set.of(pedestrian));

    assertTrue(onStreet.hasAccessZoneEntry(station, ZoneConnectoidType.ZONE_ACCESS_EGRESS));
    assertEquals(Set.of(pedestrian), added.first());
    assertEquals(Set.of(pedestrian), added.second());
  }

  /** transfer zones are found by the entry types they have and lack over all their connectoids: a station with its stop
   * on one connectoid and its access and egress on another has both */
  @Test
  public void transferZonesAreFoundByEntryTypesOverAllTheirConnectoids() {
    var walk = twoWay(0, 1, walkOnly).getLinkSegmentAb();
    var stopOnly = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var station = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.STATION, true);
    var accessOnly = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var atPole = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stopOnly, true, walk, ZoneConnectoidType.PT_VEHICLE_STOP);
    atPole.createUndirectedAccessZoneEntry(station, ZoneConnectoidType.ZONE_ACCESS_EGRESS);
    atPole.createUndirectedAccessZoneEntry(accessOnly, ZoneConnectoidType.ZONE_ACCESS);
    zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        station, true, walk, ZoneConnectoidType.PT_VEHICLE_STOP);
    var connectoidsByTransferZone = zoning.getTransferConnectoids().createIndexByAccessZone();

    var accessOrEgressWithoutStop = ZoningConverterUtils.findTransferZonesWithAnyButNoneOfEntryTypes(
        connectoidsByTransferZone, ZoneConnectoidType.ACCESS_OR_EGRESS_TYPES, Set.of(ZoneConnectoidType.PT_VEHICLE_STOP));
    var stopWithoutAccessOrEgress = ZoningConverterUtils.findTransferZonesWithAnyButNoneOfEntryTypes(
        connectoidsByTransferZone, Set.of(ZoneConnectoidType.PT_VEHICLE_STOP), ZoneConnectoidType.ACCESS_OR_EGRESS_TYPES);

    assertEquals(Set.of(accessOnly), accessOrEgressWithoutStop);
    assertEquals(Set.of(stopOnly), stopWithoutAccessOrEgress);
  }
}
