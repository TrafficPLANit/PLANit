package org.goplanit.test.zoning;

import org.goplanit.converter.zoning.ZoningConverterCommonData;
import org.goplanit.converter.zoning.ZoningConverterModifierUtils;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Breaking links at a node on the zoning converter data keeps what refers to them right: a transfer connectoid on
 * a broken link segment, and the spatial index of links. A node at a location on a link is the end node there, or a
 * new node with the link broken at it
 *
 * <pre>
 *   0 ----------------&gt; 2      broken at 1:   0 ------&gt; 1 ------&gt; 2
 *                      stop                                       stop
 * </pre>
 *
 * Nodes 0, 1 and 2 at (0, 0), (1, 0) and (2, 0), the link from 0 to 2 running through where node 1 lies; the stop's
 * connectoid is reached over the segment from 0 to 2, so at node 2
 */
public class BreakLinkAtNodeTest extends LayerTestBase {

  /** the mode of the stop */
  private Mode bus;

  /** the link to break */
  private MacroscopicLink link;

  /** zoning with the stop */
  private Zoning zoning;

  /** converter data over the network and zoning, its connectoids and links indexed */
  private ZoningConverterCommonData data;

  /** the stop's connectoid */
  private TransferConnectoid connectoid;

  /** One layer for bus, the link, a stop with a bus connectoid at node 2, and converter data indexing both */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    network.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus));
    createNodesOnLine(3);
    link = breakableOneWay(0, 2);
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    TransferZone stop = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        stop, true, segment(0, 2), ZoneConnectoidType.PT_VEHICLE_STOP);
    connectoid.getAsDirectedAccessZoneEntry(stop, ZoneConnectoidType.PT_VEHICLE_STOP).addExplicitAllowedMode(bus);
    data = new ZoningConverterCommonData(network, zoning);
    data.getConnectoidData().indexExistingTransferConnectoids();
    data.recreateSpatiallyIndexedLinks();
  }

  /** The broken link comes back as two pieces, from node 0 to node 1 and from node 1 to node 2 */
  @Test
  public void testBrokenLinkGivesTwoPieces() {
    long linkId = link.getId();
    var brokenLinks = ZoningConverterModifierUtils.breakLinksAtNode(data, nodes.get(1), layer, List.of(link));
    assertEquals(Set.of(linkId), brokenLinks.keySet());
    var pieces = brokenLinks.get(linkId);
    assertEquals(Set.of(Set.of(nodes.get(0), nodes.get(1)), Set.of(nodes.get(1), nodes.get(2))),
        Set.of(Set.of(pieces.first().getNodeA(), pieces.first().getNodeB()),
            Set.of(pieces.second().getNodeA(), pieces.second().getNodeB())));
  }

  /** The connectoid, reached over the segment ending at node 2, is reached over the piece from node 1 to node 2 */
  @Test
  public void testConnectoidOnBrokenSegmentKeepsItsNode() {
    ZoningConverterModifierUtils.breakLinksAtNode(data, nodes.get(1), layer, List.of(link));
    var accessSegment = (LinkSegment) connectoid.getExplicitAccessLinkSegmentsStream().findFirst().orElseThrow();
    assertEquals(nodes.get(1), accessSegment.getUpstreamVertex());
    assertEquals(nodes.get(2), accessSegment.getDownstreamVertex());
  }

  /** The spatial index finds the two pieces where the link was, and nothing else */
  @Test
  public void testSpatialIndexHoldsThePieces() {
    var pieces =
        ZoningConverterModifierUtils.breakLinksAtNode(data, nodes.get(1), layer, List.of(link)).get(link.getId());
    var found = new HashSet<>(data.findLinksSpatially(layer, new Envelope(-1, 3, -1, 1)));
    assertEquals(Set.of(pieces.first(), pieces.second()), found);
    assertTrue(found.stream().noneMatch(l -> l.getNodeA() == nodes.get(0) && l.getNodeB() == nodes.get(2)));
  }

  /** A location inside the link, at (0.5, 0) between its shape points, gives a new node there with the link broken
   * at it: one node and one link more, the new node joining the two pieces */
  @Test
  public void testLocationInsideLinkGivesNewNodeWithLinkBroken() {
    int nodesBefore = layer.getNodes().size();
    int linksBefore = layer.getLinks().size();
    var nodeAndBroken = ZoningConverterModifierUtils.extractNodeByLinkGeometryLocation(
        data, PlanitJtsUtils.createPoint(0.5, 0), link, layer);
    assertTrue(nodeAndBroken.second());
    assertTrue(nodeAndBroken.first().isPositionEqual2D(new Coordinate(0.5, 0), Precision.EPSILON_6));
    assertEquals(nodesBefore + 1, layer.getNodes().size());
    assertEquals(linksBefore + 1, layer.getLinks().size());
    assertEquals(2, nodeAndBroken.first().getLinks().size());
  }

  /** A location at the link's end node 2 gives that node, nothing broken */
  @Test
  public void testLocationAtEndNodeGivesThatNode() {
    int linksBefore = layer.getLinks().size();
    var nodeAndBroken = ZoningConverterModifierUtils.extractNodeByLinkGeometryLocation(
        data, PlanitJtsUtils.createPoint(2, 0), link, layer);
    assertFalse(nodeAndBroken.second());
    assertEquals(nodes.get(2), nodeAndBroken.first());
    assertEquals(linksBefore, layer.getLinks().size());
  }
}
