package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An access zone entry keeps its access link segments right when the link segment ids are renumbered: removing and
 * adding still find the segments meant, and they come out in id order
 *
 * <pre>
 *   0 -----&gt; 1 &lt;----- 2 &lt;----- 3      segments 0-&gt;1 (id 0), 3-&gt;2 (id 1) and 2-&gt;1 (id 2); the stop's connectoid is at
 *                                      node 1, its entry reached over 0-&gt;1 and 2-&gt;1. Removing 3-&gt;2 and renumbering
 *                                      leaves 0-&gt;1 with id 0 and 2-&gt;1 with id 1
 * </pre>
 */
public class DirectedConnectoidAccessZoneEntryTest extends LayerTestBase {

  /** the road type of every segment */
  private MacroscopicLinkSegmentType road;

  /** segment 0-&gt;1, id 0 throughout */
  private MacroscopicLinkSegment fromWest;

  /** segment 2-&gt;1, id 2 until renumbered, then 1 */
  private MacroscopicLinkSegment fromEast;

  /** the stop's vehicle stop entry, reached over 0-&gt;1 and 2-&gt;1 */
  private DirectedConnectoidAccessZoneEntry entry;

  /** Remove the segment 3-&gt;2 and renumber the link segment ids, so 2-&gt;1 takes id 1 */
  private void renumberLinkSegmentIdsLeavingNoGap() {
    layer.getLayerModifier().removeEdgeSegment(segment(3, 2));
    layer.getLayerModifier().recreateManagedIdEntities();
  }

  /**
   * One layer with the three one-way segments, and a zoning with a bus stop whose connectoid at node 1 is reached over
   * 0-&gt;1 and 2-&gt;1
   */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus));
    road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(bus));
    createNodesOnLine(4);
    link(0, 1, road, null);
    link(3, 2, road, null);
    link(2, 1, road, null);
    fromWest = segment(0, 1);
    fromEast = segment(2, 1);
    assertEquals(2, fromEast.getId());

    var zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    TransferZone stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, fromWest, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry = connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addAccessLinkSegment(fromEast);
  }

  /** After renumbering, removing 2-&gt;1, now id 1, takes it out of the entry, 0-&gt;1 staying */
  @Test
  public void removingAfterRenumberingRemovesTheSegment() {
    renumberLinkSegmentIdsLeavingNoGap();
    assertEquals(1, fromEast.getId());

    assertTrue(entry.removeAccessLinkSegment(fromEast));
    assertFalse(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromWest));
  }

  /** After renumbering, adding a new segment 3-&gt;1, which takes the id 2 that 2-&gt;1 had before, keeps all three */
  @Test
  public void addingSegmentWithAnotherSegmentsOldIdKeepsBoth() {
    renumberLinkSegmentIdsLeavingNoGap();
    link(3, 1, road, null);
    var fromFarEast = segment(3, 1);
    assertEquals(2, fromFarEast.getId());

    entry.addAccessLinkSegment(fromFarEast);

    assertTrue(entry.hasAccessLinkSegment(fromWest));
    assertTrue(entry.hasAccessLinkSegment(fromEast));
    assertTrue(entry.hasAccessLinkSegment(fromFarEast));
    assertEquals(3, entry.getAccessLinkSegments().size());
  }

  /** The access link segments come out in the order of their ids, also after renumbering */
  @Test
  public void segmentsComeOutInIdOrder() {
    renumberLinkSegmentIdsLeavingNoGap();
    link(3, 1, road, null);
    entry.addAccessLinkSegment(segment(3, 1));

    var ids = entry.getAccessLinkSegments().stream().map(EdgeSegment::getId).collect(Collectors.toList());
    assertEquals(ids.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()), ids);
  }
}
