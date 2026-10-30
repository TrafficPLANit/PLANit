package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Removing the transfer connectoids no service uses: at a node no service calls at, the entries serving public
 * transport vehicles are unused, entries on the same connectoid giving travellers access to or egress from another
 * zone are not
 */
public class RemoveUnusedTransferConnectoidsTest extends LayerTestBase {

  /** zoning holding the transfer zones and connectoids */
  private Zoning zoning;

  /** service network whose only layer has no services */
  private ServiceNetwork serviceNetwork;

  /** the segment the connectoids give access to */
  private MacroscopicLinkSegment segment;

  /**
   * One layer with a one-way link A-&gt;B, a zoning, and a service network layer on top of it without services
   */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    layer = network.getTransportLayers().getFactory().registerNew();
    createNodes(2);
    oneWay(0, 1);
    segment = segment(0, 1);

    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    serviceNetwork = new ServiceNetwork(token, network);
    serviceNetwork.getTransportLayers().getFactory().registerNew(layer);
  }

  /** a stop no service calls at goes with all its entries, and its connectoid, serving nothing else, with it */
  @Test
  public void connectoidOfUnusedStopOnlyIsRemoved() {
    var busStop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        busStop, true, segment, ZoneConnectoidType.PT_VEHICLE_STOP);
    connectoid.createUndirectedAccessZoneEntry(busStop, ZoneConnectoidType.ZONE_ACCESS_EGRESS);

    zoning.getZoningModifier().removeUnusedTransferConnectoids(serviceNetwork.getTransportLayers(), false);

    assertTrue(zoning.getTransferConnectoids().isEmpty());
  }

  /** a stop no service calls at shares its connectoid with the egress of a station, only the stop's entry goes */
  @Test
  public void egressOfOtherZoneOnConnectoidOfUnusedStopIsKept() {
    var busStop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var station = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.STATION, true);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        busStop, true, segment, ZoneConnectoidType.PT_VEHICLE_STOP);
    connectoid.createUndirectedAccessZoneEntry(busStop, ZoneConnectoidType.ZONE_ACCESS_EGRESS);
    connectoid.createUndirectedAccessZoneEntry(station, ZoneConnectoidType.ZONE_EGRESS);

    zoning.getZoningModifier().removeUnusedTransferConnectoids(serviceNetwork.getTransportLayers(), false);

    assertEquals(1, zoning.getTransferConnectoids().size());
    assertFalse(connectoid.hasAccessZoneEntry(busStop));
    assertTrue(connectoid.hasAccessZoneEntry(station, ZoneConnectoidType.ZONE_EGRESS));
    assertEquals(1, connectoid.getNumberOfAccessZoneEntries());
  }
}
