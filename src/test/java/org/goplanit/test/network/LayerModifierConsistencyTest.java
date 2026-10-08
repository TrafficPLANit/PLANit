package org.goplanit.test.network;

import org.goplanit.test.LayerTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.graph.directed.modifier.DirectedGraphModifierImpl;
import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.graph.directed.modifier.event.RemoveMovementEvent;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.MacroscopicNetworkModifierUtils;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.network.layer.modifier.MacroscopicNetworkLayerModifierImpl;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the macroscopic layer modifier keeps what depends on the network consistent with every change it makes,
 * alongside the listeners registered on it from outside
 *
 * @author markr
 */
public class LayerModifierConsistencyTest extends LayerTestBase {

  private MacroscopicNetwork network;
  private IntersectionsImpl intersections;

  /** an outside listener, registered for chosen event types */
  private static class OutsideListener implements DirectedGraphModifierListener {
    final List<Event> received = new ArrayList<>();
    @Override public void onGraphModificationEvent(GraphModificationEvent event) { received.add(event); }
    @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) { received.add(event); }
    @Override public EventType[] getKnownSupportedEventTypes() { return new EventType[0]; }
  }

  /** an outside listener for intersection events, registered for chosen event types */
  private static class OutsideIntersectionListener implements IntersectionModifierListener {
    final List<IntersectionModificationEvent> received = new ArrayList<>();
    private final EventType[] supported;
    OutsideIntersectionListener(EventType... supported) { this.supported = supported; }
    @Override public void onIntersectionModifierEvent(IntersectionModificationEvent event) { received.add(event); }
    @Override public EventType[] getKnownSupportedEventTypes() { return supported; }
  }

  /** records what the macroscopic layer modifier logs */
  private static class RecordingHandler extends Handler {
    final List<LogRecord> records = new ArrayList<>();
    @Override public void publish(LogRecord record) { records.add(record); }
    @Override public void flush() {}
    @Override public void close() {}
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
    intersections = (IntersectionsImpl) layer.getIntersections();
  }

  /** break the link at the node through the layer modifier */
  private void breakAt(MacroscopicLink link, int node) {
    layer.getLayerModifier().breakAt(List.of(link), nodes.get(node), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /** a signalised junction at the node, not registered */
  private Intersection junctionAt(int node) {
    return intersections.getFactory().create(nodes.get(node), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
  }

  /** a registered junction at node 0 with approaches from nodes 1 and 2, all links two-way */
  private Intersection junctionWithTwoApproaches() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var junction = junctionAt(0);
    junction.addApproachSegment(segment(1, 0));
    junction.addApproachSegment(segment(2, 0));
    return intersections.getFactory().register(junction);
  }

  // MODIFIER

  @Test
  public void layerProvidesTheMacroscopicModifier() {
    assertTrue(layer.getLayerModifier() instanceof MacroscopicNetworkLayerModifierImpl);
  }

  @Test
  public void layerCreatesItsModifierOnce() {
    assertSame(layer.getLayerModifier(), layer.getLayerModifier());
  }

  @Test
  public void copyOfLayerModifiesOnlyTheCopy() {
    createNodes(2);
    twoWay(0, 1);
    var copy = layer.deepClone();

    assertNotSame(layer.getLayerModifier(), copy.getLayerModifier());
    copy.getLayerModifier().removeEdge(copy.getLinks().getFirst());
    assertEquals(0, copy.getLinks().size());
    assertEquals(1, layer.getLinks().size());
  }

  @Test
  public void outsideListenerReceivesItsEvents() {
    createNodes(2);
    var link = twoWay(0, 1);
    var outside = new OutsideListener();
    layer.getLayerModifier().addListener(outside, RemoveEdgeSegmentEvent.EVENT_TYPE);

    layer.getLayerModifier().removeEdge(link);

    assertEquals(2, outside.received.size());
  }

  // INTERSECTIONS

  /** Breaking an approach link at a new node leaves the approach on the piece ending at the member node */
  @Test
  public void approachLinkBrokenKeepsThePieceEndingAtTheMember() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addApproachSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(List.of(segment(1, 2)), junction.getApproachSegments());
    assertSame(junction, intersections.getBySegment(segment(1, 2)));
    assertNull(intersections.getBySegment(segment(0, 1)));
  }

  /** Breaking an internal link at a new node leaves both pieces internal, with the new node a member */
  @Test
  public void internalLinkBrokenLeavesBothPiecesInternal() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(0);
    junction.addMemberNode(nodes.get(2));
    junction.addInternalSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(2, junction.getInternalSegments().size());
    assertTrue(junction.getInternalSegments().containsAll(List.of(segment(0, 1), segment(1, 2))));
    assertTrue(junction.getMemberNodes().contains(nodes.get(1)));
    assertSame(junction, intersections.getByMemberNode(nodes.get(1)));
    assertSame(junction, intersections.getBySegment(segment(0, 1)));
    assertSame(junction, intersections.getBySegment(segment(1, 2)));
  }

  /** A crossing registered where a junction's approach was broken takes the piece ending at the crossing */
  @Test
  public void crossingRegisteredOnBrokenApproachTakesTheOuterPiece() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addApproachSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);
    var crossing = intersections.getFactory().create(
        nodes.get(1), IntersectionControlType.SIGNALISED, IntersectionType.CROSSING);
    crossing.addApproachSegment(segment(0, 1));
    intersections.getFactory().register(crossing);

    assertEquals(List.of(segment(1, 2)), junction.getApproachSegments());
    assertSame(junction, intersections.getBySegment(segment(1, 2)));
    assertSame(crossing, intersections.getBySegment(segment(0, 1)));
  }

  /** Removing a dangling part holding a junction's only approach keeps the junction, without approaches */
  @Test
  public void partOfNetworkRemovedKeepsIntersectionWithoutApproaches() {
    createNodes(5);
    oneWay(0, 1); oneWay(1, 2); oneWay(2, 3); oneWay(3, 0);   // main cycle, kept
    oneWay(4, 0);                                             // can only be left, removed with its link
    var approach = segment(4, 0);
    var junction = junctionAt(0);
    junction.addApproachSegment(approach);
    intersections.getFactory().register(junction);

    layer.getLayerModifier().removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true, false, ls -> true);

    assertNull(layer.getLinkSegments().get(approach.getId()));
    assertTrue(junction.getApproachSegments().isEmpty());
    assertSame(junction, intersections.get(junction.getId()));
    assertNull(intersections.getBySegment(approach));
  }

  /** Removing the intersections without approaches removes only those, and outside intersection listeners hear of
   * each */
  @Test
  public void intersectionsWithoutApproachesRemoved() {
    var kept = junctionWithTwoApproaches();
    createNodes(1);
    twoWay(3, 1);
    var withoutApproaches = intersections.getFactory().register(junctionAt(3));
    var listener = new OutsideIntersectionListener(RemoveIntersectionEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    assertEquals(1, layer.getLayerModifier().removeIncompleteIntersections(false, true));

    assertNull(intersections.get(withoutApproaches.getId()));
    assertSame(kept, intersections.get(kept.getId()));
    assertEquals(List.of(withoutApproaches), listener.received.stream()
        .map(event -> ((RemoveIntersectionEvent) event).getRemovedIntersection()).collect(Collectors.toList()));
  }

  /** Removing one approach segment removes that approach and leaves the rest of the junction as it was */
  @Test
  public void singleSegmentRemovedRemovesOnlyThatApproach() {
    var junction = junctionWithTwoApproaches();
    var removed = segment(1, 0);

    layer.getLayerModifier().removeEdgeSegment(removed);

    assertEquals(List.of(segment(2, 0)), junction.getApproachSegments());
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
    assertNull(intersections.getBySegment(removed));
  }

  /** Removing a link while keeping its segments still takes those segments out of the intersections */
  @Test
  public void linkRemovedKeepingItsSegmentsRemovesThemFromIntersections() {
    var junction = junctionWithTwoApproaches();
    var kept = segment(1, 0);

    layer.getLayerModifier().removeEdge(kept.getParent(), false);

    assertEquals(List.of(segment(2, 0)), junction.getApproachSegments());
    assertNull(intersections.getBySegment(kept));
  }

  /** Removing one of two member nodes leaves the junction with the other, and nothing referring to the removed node */
  @Test
  public void memberNodeRemovedLeavesItsIntersection() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(1, 2);
    var junction = junctionAt(1);
    junction.addMemberNode(nodes.get(2));
    junction.addApproachSegment(segment(0, 1));
    junction.addInternalSegment(segment(1, 2));
    junction.addInternalSegment(segment(2, 1));
    intersections.getFactory().register(junction);
    var internal = segment(1, 2);

    layer.getLayerModifier().removeVertex(nodes.get(2));

    assertEquals(List.of(nodes.get(1)), junction.getMemberNodes());
    assertEquals(List.of(segment(0, 1)), junction.getApproachSegments());
    assertTrue(junction.getInternalSegments().isEmpty());
    assertNull(intersections.getByMemberNode(nodes.get(2)));
    assertNull(intersections.getBySegment(internal));
  }

  /** Removing a junction's only member node removes the junction, and outside intersection listeners hear of it */
  @Test
  public void lastMemberNodeRemovedRemovesTheIntersection() {
    var junction = junctionWithTwoApproaches();
    var removals = new ArrayList<Intersection>();
    layer.getLayerModifier().addListener(new IntersectionModifierListener() {
      @Override public void onIntersectionModifierEvent(IntersectionModificationEvent event) {
        removals.add(((RemoveIntersectionEvent) event).getRemovedIntersection());
      }
      @Override public EventType[] getKnownSupportedEventTypes() {
        return new EventType[] {RemoveIntersectionEvent.EVENT_TYPE};
      }
    });
    var approach = segment(1, 0);

    layer.getLayerModifier().removeVertex(nodes.get(0));

    assertNull(intersections.get(junction.getId()));
    assertNull(intersections.getByMemberNode(nodes.get(0)));
    assertNull(intersections.getBySegment(approach));
    assertEquals(List.of(junction), removals);
  }

  /** Removing a segment directly on the graph, bypassing the layer modifier, leaves intersections as they were */
  @Test
  public void removalDirectlyOnGraphLeavesIntersectionsUnchanged() {
    var junction = junctionWithTwoApproaches();
    var approach = segment(1, 0);
    var graphModifier = new DirectedGraphModifierImpl(new UntypedDirectedGraphImpl<>(IdGroupingToken.collectGlobalToken(),
        layer.getNodes(), layer.getLinks(), layer.getLinkSegments(), layer.getBannedMovements()));

    graphModifier.removeEdgeSegment(approach);

    assertEquals(List.of(approach, segment(2, 0)), junction.getApproachSegments());
    assertSame(junction, intersections.getBySegment(approach));
  }

  /** Intersections are still kept consistent after all outside listeners are removed */
  @Test
  public void intersectionsStillUpdatedAfterRemovingAllNonInternalListeners() {
    var junction = junctionWithTwoApproaches();
    var removed = segment(1, 0);
    var ban = layer.getBannedMovements().getFactory().registerNew(removed, segment(0, 2));

    layer.getLayerModifier().removeAllNonInternalListeners();
    layer.getLayerModifier().removeEdgeSegment(removed);

    assertEquals(List.of(segment(2, 0)), junction.getApproachSegments());
    assertNull(layer.getBannedMovements().get(ban.getId()));
  }

  /** An outside listener for segment removal finds the intersections already updated */
  @Test
  public void outsideListenerSeesIntersectionsAlreadyUpdated() {
    var junction = junctionWithTwoApproaches();
    var removed = segment(1, 0);
    var listedWhenHeard = new ArrayList<Boolean>();
    layer.getLayerModifier().addListener(new OutsideListener() {
      @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
        listedWhenHeard.add(junction.getApproachSegments().contains(removed));
      }
    }, RemoveEdgeSegmentEvent.EVENT_TYPE);

    layer.getLayerModifier().removeEdgeSegment(removed);

    assertEquals(List.of(false), listedWhenHeard);
  }

  // BANNED MOVEMENTS

  /** Removing a segment through the layer modifier removes the bans starting from it */
  @Test
  public void segmentRemovedThroughLayerModifierRemovesItsBans() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedMovements().getFactory().registerNew(segment(1, 0), segment(0, 2));

    layer.getLayerModifier().removeEdgeSegment(segment(1, 0));

    assertNull(layer.getBannedMovements().get(ban.getId()));
  }

  /** An outside listener for ban removal hears of a ban removed with its segment */
  @Test
  public void outsideListenerHearsOfBanRemovedWithItsSegment() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedMovements().getFactory().registerNew(segment(1, 0), segment(0, 2));
    var outside = new OutsideListener();
    layer.getLayerModifier().addListener(outside, RemoveMovementEvent.EVENT_TYPE);

    layer.getLayerModifier().removeEdgeSegment(segment(1, 0));

    assertEquals(1, outside.received.size());
    assertSame(ban, ((RemoveMovementEvent) outside.received.get(0)).getRemovedMovement());
  }

  /** A registered ban is found by both its segments, and by neither once removed */
  @Test
  public void banFoundBySegmentWhileRegistered() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var bans = layer.getBannedMovements();
    var ban = bans.getFactory().registerNew(segment(1, 0), segment(0, 2));

    assertEquals(List.of(ban), bans.getBySegment(segment(1, 0)));
    assertEquals(List.of(ban), bans.getBySegment(segment(0, 2)));
    bans.remove(ban);
    assertTrue(bans.getBySegment(segment(1, 0)).isEmpty());
    assertTrue(bans.getBySegment(segment(0, 2)).isEmpty());
  }

  /** Breaking the link a ban starts from leaves it found by the piece ending at its centre node only */
  @Test
  public void banFoundByPieceEndingAtCentreAfterBreakingLink() {
    createNodesOnLine(4);
    var link = breakableOneWay(0, 2);
    oneWay(2, 3);
    var ban = layer.getBannedMovements().getFactory().registerNew(segment(0, 2), segment(2, 3));

    breakAt(link, 1);

    assertEquals(List.of(ban), layer.getBannedMovements().getBySegment(segment(1, 2)));
    assertTrue(layer.getBannedMovements().getBySegment(segment(0, 1)).isEmpty());
  }

  /** A deep copy of the layer finds its bans by its own segments, and the original by the original's */
  @Test
  public void copiedLayerFindsItsBansByItsOwnSegments() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedMovements().getFactory().registerNew(segment(1, 0), segment(0, 2));

    var copy = layer.deepClone();
    var copiedBan = copy.getBannedMovements().get(ban.getId());

    assertNotSame(ban, copiedBan);
    assertNotSame(segment(1, 0), copiedBan.getSegmentFrom());
    assertEquals(List.of(copiedBan), copy.getBannedMovements().getBySegment(copiedBan.getSegmentFrom()));
    assertEquals(List.of(ban), layer.getBannedMovements().getBySegment(segment(1, 0)));
    assertTrue(copy.getBannedMovements().getBySegment(segment(1, 0)).isEmpty());
  }

  // INTERSECTION REMOVAL, IDS AND LOGGING

  /** Removing an intersection explicitly fires its event once; removing it again changes nothing and fires nothing */
  @Test
  public void explicitRemovalFiresOnceAndRepeatChangesNothing() {
    var junction = junctionWithTwoApproaches();
    var listener = new OutsideIntersectionListener(RemoveIntersectionEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    assertTrue(layer.getLayerModifier().removeIntersection(junction));
    assertFalse(layer.getLayerModifier().removeIntersection(junction));

    assertNull(intersections.get(junction.getId()));
    assertNull(intersections.getByMemberNode(nodes.get(0)));
    assertNull(intersections.getBySegment(segment(1, 0)));
    assertEquals(1, listener.received.size());
  }

  /** Removing several intersections removes those registered, fires an event for each, and counts only those */
  @Test
  public void removingSeveralIntersectionsCountsThoseRegistered() {
    var first = junctionWithTwoApproaches();
    createNodes(1);                                                  // node 3
    var notRegistered = junctionAt(3);
    var listener = new OutsideIntersectionListener(RemoveIntersectionEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    assertEquals(1, layer.getLayerModifier().removeIntersections(List.of(first, notRegistered)));

    assertNull(intersections.get(first.getId()));
    assertEquals(1, listener.received.size());
  }

  /** An intersection emptied by raw edits is removed on request, firing its event and leaving no stale lookup */
  @Test
  public void intersectionEmptiedByRawEditsRemovedOnRequest() {
    var junction = junctionWithTwoApproaches();
    var listener = new OutsideIntersectionListener(RemoveIntersectionEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);
    junction.removeMemberNode(nodes.get(0), true);                   // raw edit of a registered intersection

    assertEquals(1, layer.getLayerModifier().removeIncompleteIntersections(true, false));

    assertNull(intersections.get(junction.getId()));
    assertNull(intersections.getByMemberNode(nodes.get(0)));
    assertEquals(1, listener.received.size());
    assertEquals(0, layer.getLayerModifier().removeIncompleteIntersections(true, false));
  }

  /** Each flag removes only the intersections incomplete in its own way, and together they remove both */
  @Test
  public void incompleteIntersectionsRemovedPerSelectedWay() {
    var emptied = junctionWithTwoApproaches();
    emptied.removeMemberNode(nodes.get(0), false);                   // raw edit, still holds its approaches
    createNodes(1);                                                  // node 3
    twoWay(3, 1);
    var withoutApproaches = intersections.getFactory().register(junctionAt(3));

    assertEquals(0, layer.getLayerModifier().removeIncompleteIntersections(false, false));
    assertEquals(1, layer.getLayerModifier().removeIncompleteIntersections(false, true));
    assertNull(intersections.get(withoutApproaches.getId()));
    assertSame(emptied, intersections.get(emptied.getId()));

    var stillWithoutApproaches = intersections.getFactory().register(junctionAt(3));
    assertEquals(2, layer.getLayerModifier().removeIncompleteIntersections(true, true));
    assertNull(intersections.get(emptied.getId()));
    assertNull(intersections.get(stillWithoutApproaches.getId()));
  }

  /** Recreating ids makes intersection ids contiguous, fires its event, and leaves what each refers to in place */
  @Test
  public void recreatingIdsKeepsIntersectionsWorking() {
    var removedFirst = junctionWithTwoApproaches();                  // id 0
    createNodes(2);                                                  // nodes 3 and 4
    twoWay(3, 4);
    var kept = junctionAt(3);                                        // id 1
    kept.addApproachSegment(segment(4, 3));
    intersections.getFactory().register(kept);
    layer.getLayerModifier().removeIntersection(removedFirst);
    var listener = new OutsideIntersectionListener(RecreatedIntersectionsManagedIdsEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().recreateManagedIdEntities();

    assertEquals(0, kept.getId());
    assertEquals(1, listener.received.size());
    assertSame(layer.getIntersections(),
        ((RecreatedIntersectionsManagedIdsEvent) listener.received.get(0)).getIntersections());
    assertSame(kept, intersections.getByMemberNode(nodes.get(3)));
    var approach = segment(4, 3);
    layer.getLayerModifier().removeEdgeSegment(approach);
    assertTrue(kept.getApproachSegments().isEmpty());
  }

  /** An intersection removed with its last member node is logged, unless logging is switched off */
  @Test
  public void intersectionRemovedWithLastNodeIsLoggedUnlessSwitchedOff() {
    var logger = Logger.getLogger(MacroscopicNetworkLayerModifierImpl.class.getCanonicalName());
    var handler = new RecordingHandler();
    logger.addHandler(handler);
    try {
      junctionWithTwoApproaches();
      layer.getLayerModifier().removeVertex(nodes.get(0));
      assertEquals(1, handler.records.size());

      handler.records.clear();
      createNodes(2);                                                // nodes 3 and 4
      twoWay(3, 4);
      var second = junctionAt(3);
      second.addApproachSegment(segment(4, 3));
      intersections.getFactory().register(second);
      layer.getLayerModifier().setLogModifications(false);
      layer.getLayerModifier().removeVertex(nodes.get(3));
      assertTrue(handler.records.isEmpty());
      assertNull(intersections.get(second.getId()));
    } finally {
      logger.removeHandler(handler);
    }
  }

  /** Syncing a network's XML ids to its ids covers its intersections, including those renumbered by a removal */
  @Test
  public void syncingXmlIdsToIdsCoversIntersections() {
    createNodes(3);
    var first = intersections.getFactory().register(junctionAt(0));
    var second = intersections.getFactory().register(junctionAt(1));
    var third = intersections.getFactory().register(junctionAt(2));
    second.setXmlId("b");
    third.setXmlId("c");
    layer.getLayerModifier().removeIntersection(first);

    MacroscopicNetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);

    assertEquals(0, second.getId());
    assertEquals(String.valueOf(second.getId()), second.getXmlId());
    assertEquals(String.valueOf(third.getId()), third.getXmlId());
  }
}
