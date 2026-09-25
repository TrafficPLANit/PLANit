package org.goplanit.test.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.graph.directed.modifier.DirectedGraphModifierImpl;
import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RemoveMovementEvent;
import org.goplanit.graph.modifier.event.BreakEdgeEvent;
import org.goplanit.graph.modifier.event.RemoveEdgeEvent;
import org.goplanit.graph.modifier.event.RemoveGraphEntityEvent;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the graph modifier fires the events of the changes it makes to the listeners registered for them, and
 * leaves the layer consistent when a change cannot be made.
 *
 * @author markr
 */
public class GraphModifierEventsTest {

  private MacroscopicNetworkLayer layer;
  private List<Node> nodes;

  /** records the events it receives */
  private static class RecordingListener implements DirectedGraphModifierListener {
    final List<Object> received = new ArrayList<>();
    private final EventType[] supported;
    /** @param supported event types it registers for when added without naming any */
    RecordingListener(EventType... supported) { this.supported = supported; }
    @Override public void onGraphModificationEvent(GraphModificationEvent event) { received.add(event); }
    @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) { received.add(event); }
    @Override public EventType[] getKnownSupportedEventTypes() { return supported; }
  }

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
    nodes = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  private void createNodes(int count) {
    for (int i = 0; i < count; ++i) {
      nodes.add(layer.getNodes().getFactory().registerNew());
    }
  }

  /** connect a to b with a segment in the a-&gt;b direction only */
  private MacroscopicLink oneWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    return link;
  }

  /** connect a to b with a segment in each direction */
  private MacroscopicLink twoWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    layer.getLinkSegments().getFactory().registerNew(link, false, true);
    return link;
  }

  /** node i positioned at (i, 0), so links between them can be given straight geometries */
  private void createNodesOnLine(int count) {
    createNodes(count);
    for (int i = 0; i < count; ++i) {
      nodes.get(i).setPosition(PlanitJtsUtils.createPoint(i, 0));
    }
  }

  /** a one-way link a->b whose geometry passes through every node on the line between them */
  private MacroscopicLink breakableOneWay(int a, int b) {
    var link = oneWay(a, b);
    var coordinates = new Coordinate[b - a + 1];
    for (int i = a; i <= b; ++i) {
      coordinates[i - a] = new Coordinate(i, 0);
    }
    link.setGeometry(PlanitJtsUtils.createLineString(coordinates));
    return link;
  }

  /** a two-way link from node 0 whose end node B is taken away by a raw edit */
  private MacroscopicLink linkWithoutNodeB() {
    var link = twoWay(0, 1);
    link.setVertexB(null);
    return link;
  }

  /** a graph modifier on the layer's own containers, so listeners internal to it can be registered */
  private DirectedGraphModifierImpl graphModifierOnLayer() {
    return new DirectedGraphModifierImpl(new UntypedDirectedGraphImpl<>(IdGroupingToken.collectGlobalToken(),
        layer.getNodes(), layer.getLinks(), layer.getLinkSegments(), layer.getBannedMovements()));
  }

  /** listener for the break link events, registered for all the types it supports */
  private RecordingListener registerBreakLinkListener() {
    var listener = new RecordingListener(BreakEdgeEvent.EVENT_TYPE, BreakEdgeSegmentEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);
    return listener;
  }

  private void assertUnchangedAfterFailedBreakLink(
      Map<Long, ?> result, MacroscopicLink link, RecordingListener listener) {
    assertTrue(result.isEmpty());
    assertEquals(1, layer.getLinks().size());
    assertEquals(2, layer.getLinkSegments().size());
    assertTrue(link.hasEdgeSegmentAb() && link.hasEdgeSegmentBa());
    assertEquals(0, listener.received.size());
  }

  /**
   * A listener registered for banned movement removal only receives the event when a banned movement is removed, here
   * as part of removing a dangling part of the network
   */
  @Test
  public void removedBannedMovementFiresEventForItsOwnListener() {
    createNodes(7);
    oneWay(0, 1); oneWay(1, 2); oneWay(2, 3); oneWay(3, 0);         // main part, kept as the largest
    var in = oneWay(4, 5); var out = oneWay(5, 6);                  // dangling part, removed
    layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), out.getLinkSegmentAb());

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, RemoveMovementEvent.EVENT_TYPE);
    layer.getLayerModifier().removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true, false);

    assertEquals(0, layer.getBannedMovements().size());
    assertEquals(1, listener.received.size());
    assertEquals(RemoveMovementEvent.EVENT_TYPE, ((RemoveMovementEvent) listener.received.get(0)).getType());
  }

  /** Removing a link removes both its segments from the layer, firing a removal event for each */
  @Test
  public void removedLinkRemovesItsSegments() {
    createNodes(2);
    var link = twoWay(0, 1);

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, RemoveEdgeSegmentEvent.EVENT_TYPE);
    layer.getLayerModifier().removeEdge(link);

    assertEquals(0, layer.getLinks().size());
    assertEquals(0, layer.getLinkSegments().size());
    assertEquals(2, listener.received.size());
  }

  /** Removing a link while keeping its segments leaves them registered and attached, for the caller to remove */
  @Test
  public void removedLinkKeepsItsSegmentsWhenAsked() {
    createNodes(2);
    var link = twoWay(0, 1);

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, RemoveEdgeSegmentEvent.EVENT_TYPE);
    layer.getLayerModifier().removeEdge(link, false);

    assertEquals(0, layer.getLinks().size());
    assertEquals(2, layer.getLinkSegments().size());
    assertTrue(link.hasEdgeSegmentAb());
    assertTrue(link.hasEdgeSegmentBa());
    assertEquals(0, listener.received.size());
  }

  /** Removing a link without segments removes it without complaint */
  @Test
  public void removedLinkWithoutSegmentsIsRemoved() {
    createNodes(2);
    var link = layer.getLinks().getFactory().registerNew(nodes.get(0), nodes.get(1), 1, true);

    graphModifierOnLayer().removeEdge(link);

    assertNull(layer.getLinks().get(link.getId()));
  }

  /** Removing a segment directly on the graph modifier removes only the bans referring to it, firing an event for each */
  @Test
  public void removedSegmentRemovesItsBans() {
    createNodes(4);
    var in = oneWay(0, 1);
    var out = oneWay(1, 2);
    var other = oneWay(3, 1);
    var removedBan = layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), out.getLinkSegmentAb());
    var keptBan = layer.getBannedMovements().getFactory().registerNew(other.getLinkSegmentAb(), out.getLinkSegmentAb());
    var modifier = graphModifierOnLayer();
    var listener = new RecordingListener(RemoveEdgeSegmentEvent.EVENT_TYPE, RemoveMovementEvent.EVENT_TYPE);
    modifier.addListener(listener);

    modifier.removeEdgeSegment(in.getLinkSegmentAb());

    assertNull(layer.getBannedMovements().get(removedBan.getId()));
    assertSame(keptBan, layer.getBannedMovements().get(keptBan.getId()));
    assertEquals(2, listener.received.size());
    assertTrue(listener.received.get(0) instanceof RemoveEdgeSegmentEvent);
    assertTrue(listener.received.get(1) instanceof RemoveMovementEvent);
  }

  /** Removing a segment that is no longer registered changes nothing and fires no event */
  @Test
  public void repeatedSegmentRemovalFiresOnce() {
    createNodes(2);
    var link = twoWay(0, 1);
    var segment = link.getLinkSegmentAb();

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, RemoveEdgeSegmentEvent.EVENT_TYPE);
    layer.getLayerModifier().removeEdgeSegment(segment);
    layer.getLayerModifier().removeEdgeSegment(segment);

    assertEquals(1, layer.getLinkSegments().size());
    assertTrue(link.hasEdgeSegmentBa());
    assertEquals(1, listener.received.size());
  }

  /** Removing a link and then one of its segments fires one removal event per segment */
  @Test
  public void segmentRemovedAfterItsLinkFiresOnce() {
    createNodes(2);
    var link = twoWay(0, 1);
    var segment = link.getLinkSegmentAb();

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, RemoveEdgeSegmentEvent.EVENT_TYPE);
    layer.getLayerModifier().removeEdge(link);
    layer.getLayerModifier().removeEdgeSegment(segment);

    assertEquals(0, layer.getLinkSegments().size());
    assertEquals(2, listener.received.size());
  }

  /**
   * Breaking a link without end node B at a node leaves the layer unchanged, throws nothing and fires no break link
   * events
   */
  @Test
  public void failedBreakLinkChangesNothing() {
    createNodes(3);
    var link = linkWithoutNodeB();
    var listener = registerBreakLinkListener();

    var result = layer.getLayerModifier().breakAt(
        List.of(link), nodes.get(2), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);

    assertUnchangedAfterFailedBreakLink(result, link, listener);
  }

  /** As {@link #failedBreakLinkChangesNothing()}, for breaking a link given an index of banned movements */
  @Test
  public void failedBreakLinkWithIndexedBansChangesNothing() {
    createNodes(3);
    var link = linkWithoutNodeB();
    var listener = registerBreakLinkListener();

    var result = layer.getLayerModifier().breakAt(
        List.of(link), nodes.get(2), new HashMap<Node, List<BannedMovement>>(),
        PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);

    assertUnchangedAfterFailedBreakLink(result, link, listener);
  }

  /**
   * Breaking a link at a node, given an index of bans that still holds a removed ban, completes and leaves that ban
   * untouched
   */
  @Test
  public void breakLinkWithStaleBanIndexSkipsRemovedBan() {
    createNodes(4);
    nodes.get(0).setPosition(PlanitJtsUtils.createPoint(0, 0));
    nodes.get(1).setPosition(PlanitJtsUtils.createPoint(2, 0));
    nodes.get(2).setPosition(PlanitJtsUtils.createPoint(1, 0));   // node to break at
    nodes.get(3).setPosition(PlanitJtsUtils.createPoint(2, 1));
    var toBreak = oneWay(0, 1);
    toBreak.setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(2, 0)));
    var exit = oneWay(1, 3);
    var ban = layer.getBannedMovements().getFactory().registerNew(toBreak.getLinkSegmentAb(), exit.getLinkSegmentAb());
    Map<Node, List<BannedMovement>> bansByCentreNode = new HashMap<>(Map.of(nodes.get(1), List.of(ban)));

    /* removed the way the graph modifier removes a ban, after the index was built */
    layer.getBannedMovements().remove(ban);
    ban.setSegmentFrom(null);
    ban.setSegmentTo(null);

    var result = layer.getLayerModifier().breakAt(
        List.of(toBreak), nodes.get(2), bansByCentreNode, PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);

    assertEquals(1, result.size());
    assertEquals(3, layer.getLinks().size());
    assertEquals(0, layer.getBannedMovements().size());
    assertTrue(!ban.hasSegmentFrom() && !ban.hasSegmentTo());
  }

  /**
   * Removing the part of the network around a node through the directed graph modifier removes its link segments and
   * the bans referring to them, not only its links and nodes
   */
  @Test
  public void removedPartAroundNodeLeavesNoSegmentsOrBans() throws PlanItException {
    createNodes(7);
    oneWay(0, 1); oneWay(1, 2); oneWay(2, 3); oneWay(3, 0);         // main part, kept
    var in = oneWay(4, 5); var out = oneWay(5, 6);                  // part around node 5, removed
    layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), out.getLinkSegmentAb());

    /* a directed graph modifier on the layer's own containers, as the layer wires its own */
    var graph = new UntypedDirectedGraphImpl<>(IdGroupingToken.collectGlobalToken(),
        layer.getNodes(), layer.getLinks(), layer.getLinkSegments(), layer.getBannedMovements());
    new DirectedGraphModifierImpl(graph).removeSubGraphOf(nodes.get(5));

    assertEquals(4, layer.getNodes().size());
    assertEquals(4, layer.getLinks().size());
    assertEquals(4, layer.getLinkSegments().size());
    assertEquals(0, layer.getBannedMovements().size());
  }

  /** Removing a dangling part of the network with several nodes fires one removed part event, not one per node */
  @Test
  public void removedPartFiresOneEvent() {
    createNodes(7);
    oneWay(0, 1); oneWay(1, 2); oneWay(2, 3); oneWay(3, 0);         // main part, kept as the largest
    twoWay(4, 5); twoWay(5, 6);                                     // dangling part of three nodes, removed

    var listener = new RecordingListener(RemoveGraphEntityEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);
    layer.getLayerModifier().removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true, false);

    assertEquals(4, layer.getNodes().size());
    assertEquals(1, listener.received.size());
  }

  /**
   * A listener registered only for the break link event receives it when a link is broken at a node, and nothing once
   * removed for that event
   */
  @Test
  public void listenerForNamedBreakLinkEventReceivesItUntilRemoved() {
    createNodesOnLine(6);
    var first = breakableOneWay(0, 2);
    var second = breakableOneWay(3, 5);

    var listener = new RecordingListener();
    layer.getLayerModifier().addListener(listener, BreakEdgeEvent.EVENT_TYPE);
    layer.getLayerModifier().breakAt(List.of(first), nodes.get(1), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
    assertEquals(1, listener.received.size());

    layer.getLayerModifier().removeListener(listener, BreakEdgeEvent.EVENT_TYPE);
    layer.getLayerModifier().breakAt(List.of(second), nodes.get(4), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
    assertEquals(1, listener.received.size());
  }

  /** A listener removed without naming an event type receives nothing afterwards */
  @Test
  public void removedListenerReceivesNothing() {
    createNodes(2);
    var link = twoWay(0, 1);

    var listener = new RecordingListener(RemoveEdgeEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);
    layer.getLayerModifier().removeListener(listener);
    layer.getLayerModifier().removeEdge(link);

    assertEquals(0, listener.received.size());
  }

  /** A listener internal to the modifier receives each event before the outside listeners do */
  @Test
  public void internalListenerReceivesEventBeforeOutsideListener() {
    createNodes(2);
    var link = oneWay(0, 1);
    var modifier = graphModifierOnLayer();

    var outside = new RecordingListener(RemoveEdgeEvent.EVENT_TYPE);
    var outsideEventsSeenByInternal = new ArrayList<Integer>();
    var internal = new RecordingListener(RemoveEdgeEvent.EVENT_TYPE) {
      @Override public void onGraphModificationEvent(GraphModificationEvent event) {
        outsideEventsSeenByInternal.add(outside.received.size());
      }
    };
    modifier.addListener(outside);                 // registered first
    modifier.addInternalListener(internal);
    modifier.removeEdge(link);

    assertEquals(List.of(0), outsideEventsSeenByInternal);
    assertEquals(1, outside.received.size());
  }

  /** A listener internal to the modifier keeps receiving events, once each, after listeners are removed */
  @Test
  public void internalListenerSurvivesRemovalOfListeners() {
    createNodes(2);
    var link = twoWay(0, 1);
    var modifier = graphModifierOnLayer();

    var internal = new RecordingListener(RemoveEdgeSegmentEvent.EVENT_TYPE, RemoveEdgeEvent.EVENT_TYPE);
    modifier.addInternalListener(internal);
    modifier.removeListener(internal);
    modifier.removeAllNonInternalListeners();
    modifier.removeEdge(link);

    assertEquals(3, internal.received.size());   // both segments, then the link
  }

  /** Recreating ban ids continues the node ids afterwards, rather than restarting them */
  @Test
  public void recreatingBanIdsLeavesNodeIdsAlone() {
    createNodes(3);
    var in = oneWay(0, 1);
    var out = oneWay(1, 2);
    layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), out.getLinkSegmentAb());

    layer.getBannedMovements().recreateIds(true);
    var next = layer.getNodes().getFactory().registerNew();

    assertEquals(3, next.getNodeId());
  }

  /** A ban reports a centre vertex only when its segments meet */
  @Test
  public void banHasCentreVertexOnlyWhereItsSegmentsMeet() {
    createNodes(4);
    var in = oneWay(0, 1);
    var out = oneWay(1, 2);
    var elsewhere = oneWay(2, 3);
    var valid = layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), out.getLinkSegmentAb());
    var invalid = layer.getBannedMovements().getFactory().registerNew(in.getLinkSegmentAb(), elsewhere.getLinkSegmentAb());

    assertSame(nodes.get(1), valid.getCentreVertex());
    assertFalse(invalid.hasCentreVertex());
    assertNull(invalid.getCentreVertex());
  }
}
