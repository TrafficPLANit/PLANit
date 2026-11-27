package org.goplanit.test.network;

import org.goplanit.test.LayerTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.graph.directed.modifier.DirectedGraphModifierImpl;
import org.goplanit.utils.graph.modifier.event.MoveEdgeEndsAtVertexEvent;
import org.goplanit.utils.graph.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.utils.graph.modifier.event.RemoveBannedTurnEvent;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.MacroscopicNetworkModifierUtils;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RemoveSpatialMovementEvent;
import org.goplanit.network.layer.modifier.MacroscopicNetworkLayerModifierImpl;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RemoveTollRoadEvent;
import org.goplanit.utils.network.layer.modifier.event.ChangeLinkSegmentTypeEvent;
import org.goplanit.utils.network.layer.modifier.event.RemoveLinkSegmentTypeEvent;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.directed.Turn;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.test.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierEventProducer;
import org.goplanit.utils.graph.modifier.event.GraphModifierEventProducer;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventProducer;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventProducer;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RecreatedTollRoadsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierEventProducer;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the macroscopic layer modifier keeps what depends on the network consistent with every change it makes,
 * alongside the listeners registered on it from outside
 *
 * @author markr
 */
public class LayerModifierConsistencyTest extends LayerTestBase {

  /** an outside listener, registered for chosen event types */
  private static class OutsideListener implements DirectedGraphModifierListener {
    final List<Event> received = new ArrayList<>();
    @Override public void onGraphModificationEvent(GraphModificationEvent event) { received.add(event); }
    @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
      received.add(event);
    }
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

  /** an outside listener for toll road events, registered for chosen event types */
  private static class OutsideTollRoadListener implements TollRoadModifierListener {
    final List<TollRoadModificationEvent> received = new ArrayList<>();
    private final EventType[] supported;
    OutsideTollRoadListener(EventType... supported) { this.supported = supported; }
    @Override public void onTollRoadModifierEvent(TollRoadModificationEvent event) { received.add(event); }
    @Override public EventType[] getKnownSupportedEventTypes() { return supported; }
  }

  private MacroscopicNetwork network;
  private IntersectionsImpl intersections;

  // SPATIAL MOVEMENTS

  /** the modes registered by {@link #junctionWithMovement()} and some tests */
  private Mode car;
  private Mode bus;

  /** the exit link of {@link #junctionWithMovement()} */
  private MacroscopicLink exitLink;

  /** break the link at the node through the layer modifier */
  private void breakAt(MacroscopicLink link, int node) {
    layer.getLayerModifier().breakAt(List.of(link), nodes.get(node), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /** a signalised non-spatial junction at the node, not registered */
  private NonSpatialIntersection junctionAt(int node) {
    return intersections.getFactory().createNonSpatial(
        nodes.get(node), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
  }

  /** a registered junction at node 0 with approaches from nodes 1 and 2, all links two-way */
  private Intersection junctionWithTwoApproaches() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var junction = junctionAt(0);
    junction.addNewApproachSegment(segment(1, 0));
    junction.addNewApproachSegment(segment(2, 0));
    return intersections.getFactory().register(junction);
  }

  /**
   * A spatial junction of member nodes 1 and 2, joined by the internal segment 1-2, with the approach 0-1, the exit 2-4
   * (whose geometry passes node 3) and a movement from that approach to that exit, without a direct ban. Every segment
   * allows cars and buses
   * <pre>
   *   0 ---> 1 ===> 2 ---------> 4      nodes on a line; 3 lies on the exit, not linked yet
   * </pre>
   *
   * @return the movement
   */
  private SpatialMovement junctionWithMovement() {
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer.registerSupportedModes(List.of(car, bus));
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(road, 50, List.of(car, bus));
    createNodesOnLine(5);
    breakableOneWay(0, 1);
    breakableOneWay(1, 2);
    exitLink = breakableOneWay(2, 4);
    layer.getLinkSegments().forEach(segment -> segment.setLinkSegmentType(road));
    var junction = spatialJunctionOver(1, 2);
    junction.addNewApproachSegment(segment(0, 1));
    junction.addNewInternalSegment(segment(1, 2));
    var movement = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(2, 4), 0.001);
    junction.addMovement(movement);
    intersections.getFactory().register(junction);
    return movement;
  }

  /**
   * The modes a movement of the junction of {@link #junctionWithMovement()} is banned for, directly or implicitly,
   * worked out from the layer
   *
   * @param movement to work out the banned modes of
   * @return the modes
   */
  private Set<Mode> bannedModes(SpatialMovement movement) {
    return SpatialMovementUtils.findBannedModes(
        layer, (SpatialIntersection) intersections.getFirst(), movement, true, true);
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
    intersections = (IntersectionsImpl) layer.getIntersections();
  }

  // MODIFIER

  /** The layer provides the macroscopic modifier, and creates it once */
  @Test
  public void layerProvidesTheMacroscopicModifierCreatedOnce() {
    assertTrue(layer.getLayerModifier() instanceof MacroscopicNetworkLayerModifierImpl);
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

  /** Breaking an approach link at a new node leaves the approach on the link segment ending at the member node */
  @Test
  public void approachLinkBrokenKeepsTheLinkSegmentEndingAtTheMember() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addNewApproachSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(List.of(segment(1, 2)), junction.getApproachSegments());
    assertSame(junction, intersections.getBySegment(segment(1, 2)));
    assertNull(intersections.getBySegment(segment(0, 1)));
  }

  /** Breaking an approach link at a new node keeps whether the approach has a signal of its own on the link segment
   * ending at the member node */
  @Test
  public void approachLinkBrokenKeepsItsOwnSignal() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addNewApproachSegment(segment(0, 2));
    junction.swapApproachToNewSignalOrUnsignalised(segment(0, 2), false);
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(List.of(segment(1, 2)), junction.getApproachSegments());
    assertFalse(junction.hasOwnSignal(segment(1, 2)));
  }

  /** Breaking an approach link at a new node upstream gives the approach's values, with how they got their value, to
   * the new link segment ending at the member node */
  @Test
  public void approachLinkBrokenKeepsItsValues() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addNewApproachSegment(segment(0, 2));
    var approach = (IntersectionSignalSegment) junction.getApproach(segment(0, 2));
    approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    approach.setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY);
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(List.of(segment(1, 2)), junction.getApproachSegments());
    var afterBreak = (IntersectionSignalSegment) junction.getApproach(segment(1, 2));
    assertEquals(0.44, afterBreak.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, afterBreak.getGreenRatioOrigin());
    assertEquals(0.95, afterBreak.getSaturationFlowFactor());
    assertNull(junction.getApproach(segment(0, 1)));
  }

  /** Breaking an internal link with a signal at a new node keeps its values on the link segment that keeps the
   * signal */
  @Test
  public void internalLinkBrokenKeepsItsValuesWithTheSignal() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = spatialJunctionOver(0, 2);
    junction.addNewInternalSegmentWithSignal(segment(0, 2));
    ((IntersectionSignalSegment) junction.getIntersectionInternalSegment(segment(0, 2)))
        .setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(1, junction.getInternalSegmentsWithSignal().size());
    var withSignal = junction.getInternalSegmentsWithSignal().get(0);
    var internalWithSignal = (IntersectionSignalSegment) junction.getIntersectionInternalSegment(withSignal);
    assertEquals(0.6, internalWithSignal.getGreenRatio());
  }

  /** Breaking an internal link at a new node leaves both link segments internal, with the new node a member */
  @Test
  public void internalLinkBrokenLeavesBothLinkSegmentsInternal() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = spatialJunctionOver(0, 2);
    junction.addNewInternalSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);

    assertEquals(2, junction.getInternalSegments().size());
    assertTrue(junction.getInternalSegments().containsAll(List.of(segment(0, 1), segment(1, 2))));
    assertTrue(junction.getMemberNodes().contains(nodes.get(1)));
    assertSame(junction, intersections.getByMemberNode(nodes.get(1)));
    assertSame(junction, intersections.getBySegment(segment(0, 1)));
    assertSame(junction, intersections.getBySegment(segment(1, 2)));
  }

  /** A crossing registered where a junction's approach was broken takes the link segment ending at the crossing */
  @Test
  public void crossingRegisteredOnBrokenApproachTakesTheOuterLinkSegment() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    var junction = junctionAt(2);
    junction.addNewApproachSegment(segment(0, 2));
    intersections.getFactory().register(junction);

    breakAt(link, 1);
    var crossing = intersections.getFactory().createNonSpatial(
        nodes.get(1), IntersectionControlType.SIGNALISED, IntersectionType.CROSSING);
    crossing.addNewApproachSegment(segment(0, 1));
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
    junction.addNewApproachSegment(approach);
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

  /**
   * Removing a link while keeping its segments leaves them to the caller: the intersection keeps a kept segment until
   * the segment is removed itself, which takes it out
   */
  @Test
  public void linkRemovedKeepingItsSegmentsLeavesThemUntilTheyAreRemoved() {
    var junction = junctionWithTwoApproaches();
    var kept = segment(1, 0);

    layer.getLayerModifier().removeEdge(kept.getParent(), false);
    assertSame(junction, intersections.getBySegment(kept));

    layer.getLayerModifier().removeEdgeSegment(kept);
    assertEquals(List.of(segment(2, 0)), junction.getApproachSegments());
    assertNull(intersections.getBySegment(kept));
  }

  /** Removing one of three member nodes leaves the junction with the other two, and nothing referring to the removed
   * node */
  @Test
  public void memberNodeRemovedLeavesItsIntersection() {
    createNodes(4);
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);
    var junction = spatialJunctionOver(1, 2, 3);
    junction.addNewApproachSegment(segment(0, 1));
    List.of(segment(1, 2), segment(2, 1), segment(2, 3), segment(3, 2)).forEach(junction::addNewInternalSegment);
    intersections.getFactory().register(junction);
    var internal = segment(2, 3);

    layer.getLayerModifier().removeVertex(nodes.get(3));

    assertSame(junction, intersections.getByMemberNode(nodes.get(1)));
    assertEquals(List.of(nodes.get(1), nodes.get(2)), junction.getMemberNodes());
    assertEquals(List.of(segment(0, 1)), junction.getApproachSegments());
    assertEquals(List.of(segment(1, 2), segment(2, 1)), junction.getInternalSegments());
    assertNull(intersections.getByMemberNode(nodes.get(3)));
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
    var graphModifier = new DirectedGraphModifierImpl(new UntypedDirectedGraphImpl<>(
        IdGroupingToken.collectGlobalToken(),
        layer.getNodes(), layer.getLinks(), layer.getLinkSegments(), layer.getBannedTurns()));

    graphModifier.removeEdgeSegment(approach);

    assertEquals(List.of(approach, segment(2, 0)), junction.getApproachSegments());
    assertSame(junction, intersections.getBySegment(approach));
  }

  /** Intersections are still kept consistent after all outside listeners are removed */
  @Test
  public void intersectionsStillUpdatedAfterRemovingAllNonInternalListeners() {
    var junction = junctionWithTwoApproaches();
    var removed = segment(1, 0);
    var ban = layer.getBannedTurns().getFactory().registerNew(removed, segment(0, 2));

    layer.getLayerModifier().removeAllNonInternalListeners();
    layer.getLayerModifier().removeEdgeSegment(removed);

    assertEquals(List.of(segment(2, 0)), junction.getApproachSegments());
    assertNull(layer.getBannedTurns().get(ban.getId()));
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

  // BANNED TURNS

  /** Removing a segment through the layer modifier removes the bans starting from it */
  @Test
  public void segmentRemovedThroughLayerModifierRemovesItsBans() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));

    layer.getLayerModifier().removeEdgeSegment(segment(1, 0));

    assertNull(layer.getBannedTurns().get(ban.getId()));
  }

  /** An outside listener for ban removal hears of a ban removed with its segment */
  @Test
  public void outsideListenerHearsOfBanRemovedWithItsSegment() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));
    var outside = new OutsideListener();
    layer.getLayerModifier().addListener(outside, RemoveBannedTurnEvent.EVENT_TYPE);

    layer.getLayerModifier().removeEdgeSegment(segment(1, 0));

    assertEquals(1, outside.received.size());
    assertSame(ban, ((RemoveBannedTurnEvent) outside.received.get(0)).getRemovedBannedTurn());
  }

  /** A registered ban is found by both its segments, and by neither once removed */
  @Test
  public void banFoundBySegmentWhileRegistered() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var bans = layer.getBannedTurns();
    var ban = bans.getFactory().registerNew(segment(1, 0), segment(0, 2));

    assertEquals(List.of(ban), bans.getBySegment(segment(1, 0)));
    assertEquals(List.of(ban), bans.getBySegment(segment(0, 2)));
    bans.remove(ban);
    assertTrue(bans.getBySegment(segment(1, 0)).isEmpty());
    assertTrue(bans.getBySegment(segment(0, 2)).isEmpty());
  }

  /** Breaking the link a ban starts from leaves it found by the link segment ending at its centre node only */
  @Test
  public void banFoundByLinkSegmentEndingAtCentreAfterBreakingLink() {
    createNodesOnLine(4);
    var link = breakableOneWay(0, 2);
    oneWay(2, 3);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(0, 2), segment(2, 3));

    breakAt(link, 1);

    assertEquals(List.of(ban), layer.getBannedTurns().getBySegment(segment(1, 2)));
    assertTrue(layer.getBannedTurns().getBySegment(segment(0, 1)).isEmpty());
  }

  /**
   * A U-turn ban between two parallel one-way links, such as the two carriageways of a divided road, keeps the other
   * carriageway's segment when the link it starts from is broken: only a segment of the broken link moves
   * <pre>
   *   0 ----> 1 ----> 2    link broken at node 1
   *   0 <------------ 2    parallel link, the U-turn ban at node 2 runs from 1-2 onto it
   * </pre>
   */
  @Test
  public void uTurnBanOntoParallelLinkKeepsItsSegmentAfterBreakingLink() {
    createNodesOnLine(3);
    var link = breakableOneWay(0, 2);
    oneWay(2, 0);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(0, 2), segment(2, 0));

    breakAt(link, 1);

    assertSame(segment(1, 2), ban.getSegmentFrom());
    assertSame(segment(2, 0), ban.getSegmentTo());
    assertEquals(List.of(ban), layer.getBannedTurns().getBySegment(segment(2, 0)));
  }

  /** A deep copy of the layer finds its bans by its own segments, and the original by the original's */
  @Test
  public void copiedLayerFindsItsBansByItsOwnSegments() {
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));

    var copy = layer.deepClone();
    var copiedBan = copy.getBannedTurns().get(ban.getId());

    assertNotSame(ban, copiedBan);
    assertNotSame(segment(1, 0), copiedBan.getSegmentFrom());
    assertEquals(List.of(copiedBan), copy.getBannedTurns().getBySegment(copiedBan.getSegmentFrom()));
    assertEquals(List.of(ban), layer.getBannedTurns().getBySegment(segment(1, 0)));
    assertTrue(copy.getBannedTurns().getBySegment(segment(1, 0)).isEmpty());
  }

  /** A deep copy of the layer keeps the modes each banned turn bans, and the copy registers banned turns with modes
   * too */
  @Test
  public void copiedLayerKeepsTheModesOfItsBannedTurns() {
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    createNodes(3);
    twoWay(0, 1);
    twoWay(0, 2);
    var busOnly = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2), List.of(bus));
    var everyMode = layer.getBannedTurns().getFactory().registerNew(segment(2, 0), segment(0, 1));

    var copy = layer.deepClone();

    var copiedBusOnly = (MacroscopicBannedTurn) copy.getBannedTurns().get(busOnly.getId());
    assertNotSame(busOnly, copiedBusOnly);
    assertEquals(Set.of(bus), copiedBusOnly.getExplicitlyBannedModes());
    var copiedEveryMode = (MacroscopicBannedTurn) copy.getBannedTurns().get(everyMode.getId());
    assertFalse(copiedEveryMode.hasExplicitlyBannedModes());
    var addedToCopy = copy.getBannedTurns().getFactory().registerNew(
        copiedEveryMode.getLinkSegmentFrom(), copiedBusOnly.getLinkSegmentTo(), List.of(bus));
    assertEquals(Set.of(bus), addedToCopy.getExplicitlyBannedModes());
    assertTrue(layer.getBannedTurns().getBySegment(segment(2, 0)).stream().noneMatch(ban -> ban == addedToCopy));
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
    kept.addNewApproachSegment(segment(4, 3));
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
    junctionWithTwoApproaches();
    assertEquals(1, LogRecorder.countRecords(MacroscopicNetworkLayerModifierImpl.class, Level.ALL,
        () -> layer.getLayerModifier().removeVertex(nodes.get(0))));

    createNodes(2);                                                // nodes 3 and 4
    twoWay(3, 4);
    var second = junctionAt(3);
    second.addNewApproachSegment(segment(4, 3));
    intersections.getFactory().register(second);
    layer.getLayerModifier().setLogModifications(false);
    assertEquals(0, LogRecorder.countRecords(MacroscopicNetworkLayerModifierImpl.class, Level.ALL,
        () -> layer.getLayerModifier().removeVertex(nodes.get(3))));
    assertNull(intersections.get(second.getId()));
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

  // TOLL ROADS

  /**
   * Breaking a link of a toll road at a node leaves both new link segments on it, and only those of the tolled
   * direction ("Break a link of a toll road")
   */
  @Test
  public void brokenLinkOfATollRoadLeavesBothNewLinkSegmentsOnIt() {
    createNodesOnLine(3);
    var link = breakableTwoWay(0, 2);
    var tollRoad = layer.getTollRoads().getFactory().registerNew(
        List.of(segment(0, 2)), PricedModeSelection.unspecified());

    breakAt(link, 1);

    assertEquals(Set.of(segment(0, 1), segment(1, 2)), Set.copyOf(tollRoad.getLinkSegments()));
    assertEquals(2, tollRoad.getLinkSegments().size());
    assertSame(tollRoad, layer.getTollRoads().getByLinkSegment(segment(0, 1)));
    assertSame(tollRoad, layer.getTollRoads().getByLinkSegment(segment(1, 2)));
    assertNull(layer.getTollRoads().getByLinkSegment(segment(2, 1)));
    assertNull(layer.getTollRoads().getByLinkSegment(segment(1, 0)));
  }

  /**
   * Removing one of two link segments of a toll road keeps the toll road with the other; removing its only link
   * segment, on its own or with its link, removes the toll road, which an outside listener hears of once each ("Remove
   * the only link segment of a toll road")
   */
  @Test
  public void tollRoadLeftWithoutLinkSegmentsIsRemoved() {
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    var lastLink = oneWay(2, 3);
    var tollRoads = layer.getTollRoads();
    var first = tollRoads.getFactory().registerNew(List.of(segment(0, 1)), PricedModeSelection.unspecified());
    var second = tollRoads.getFactory().registerNew(
        List.of(segment(1, 2), segment(2, 3)), PricedModeSelection.unspecified());
    var listener = new OutsideTollRoadListener(RemoveTollRoadEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeEdgeSegment(segment(1, 2));
    assertSame(second, tollRoads.get(second.getId()));
    assertEquals(List.of(segment(2, 3)), second.getLinkSegments());
    assertTrue(listener.received.isEmpty());

    layer.getLayerModifier().removeEdgeSegment(segment(0, 1));
    assertNull(tollRoads.get(first.getId()));
    layer.getLayerModifier().removeEdge(lastLink);
    assertTrue(tollRoads.isEmpty());
    assertEquals(List.of(first, second), listener.received.stream()
        .map(event -> ((RemoveTollRoadEvent) event).getRemovedTollRoad()).collect(Collectors.toList()));
  }

  /**
   * Syncing a network's XML ids to its ids covers its toll roads, including one renumbered by a removal, which keeps
   * its name and external id
   */
  @Test
  public void syncingXmlIdsToIdsCoversTollRoads() {
    createNodes(3);
    oneWay(0, 1);
    oneWay(1, 2);
    var tollRoads = layer.getTollRoads();
    var first = tollRoads.getFactory().registerNew(List.of(segment(0, 1)), PricedModeSelection.unspecified());
    var second = tollRoads.getFactory().registerNew(List.of(segment(1, 2)), PricedModeSelection.unspecified());
    second.setXmlId("m4");
    second.setName("M4");
    second.setExternalId("way 7");
    tollRoads.remove(first);

    MacroscopicNetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);

    assertEquals(0, second.getId());
    assertEquals("0", second.getXmlId());
    assertEquals("M4", second.getName());
    assertEquals("way 7", second.getExternalId());
  }

  /** The toll roads still follow the layer once every listener registered from outside is removed */
  @Test
  public void tollRoadsStillUpdatedAfterRemovingAllNonInternalListeners() {
    createNodes(2);
    oneWay(0, 1);
    var tollRoad = layer.getTollRoads().getFactory().registerNew(
        List.of(segment(0, 1)), PricedModeSelection.unspecified());
    var listener = new OutsideTollRoadListener(RemoveTollRoadEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);
    layer.getLayerModifier().removeAllNonInternalListeners();

    layer.getLayerModifier().removeEdgeSegment(segment(0, 1));

    assertNull(layer.getTollRoads().get(tollRoad.getId()));
    assertTrue(listener.received.isEmpty());
  }

  // MOVING A LINK END

  /**
   * Moving the end of a footway at member node 1 to a centre node 3: the footway ends at 3, its geometry runs on in a
   * straight line to 3's position, its length follows, and the ban from it onto the road at 1 is removed
   * <pre>
   *               3       (1, 1)
   *   0 ---> 1 ---> 2     footway 0-1, road 1-2, nodes on a line, 1 m apart
   * </pre>
   */
  @Test
  public void movingAFootwayEndToACentreNode() {
    createNodesOnLine(3);
    createNodes(1);
    nodes.get(3).setPosition(PlanitJtsUtils.createPoint(1, 1));
    var footway = breakableOneWay(0, 1);
    breakableOneWay(1, 2);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2));

    var removedBans = layer.getLayerModifier().moveLinkEndsAtNode(
        List.of(footway), nodes.get(1), nodes.get(3), PlanitJtsCrsUtils.CARTESIANCRS);

    assertSame(nodes.get(3), footway.getNodeB());
    assertFalse(nodes.get(1).getEdges().contains(footway));
    assertTrue(nodes.get(3).getEdges().contains(footway));
    assertSame(nodes.get(3), footway.getLinkSegmentAb().getDownstreamVertex());
    var coordinates = footway.getGeometry().getCoordinates();
    assertTrue(coordinates[coordinates.length - 1].equals2D(new Coordinate(1, 1)));
    assertTrue(coordinates[coordinates.length - 2].equals2D(new Coordinate(1, 0)));
    assertEquals(0.002, footway.getLengthKm(), 1e-9);
    assertEquals(List.of(ban), removedBans);
    assertTrue(layer.getBannedTurns().isEmpty());
  }

  /**
   * Moving the ends at node 1 of two footways to a centre node 3 in one call: both end at 3, and the move is reported
   * once, naming both
   * <pre>
   *               3       (1, 1)
   *   0 ---> 1 &lt;--- 2     footways 0-1 and 2-1, nodes on a line, 1 m apart
   * </pre>
   */
  @Test
  public void movingTheEndsOfTwoLinksInOneCall() {
    createNodesOnLine(3);
    createNodes(1);
    nodes.get(3).setPosition(PlanitJtsUtils.createPoint(1, 1));
    var west = oneWay(0, 1);
    var east = oneWay(2, 1);
    var outside = new OutsideListener();
    layer.getLayerModifier().addListener(outside, MoveEdgeEndsAtVertexEvent.EVENT_TYPE);

    layer.getLayerModifier().moveLinkEndsAtNode(
        List.of(west, east), nodes.get(1), nodes.get(3), PlanitJtsCrsUtils.CARTESIANCRS);

    assertSame(nodes.get(3), west.getNodeB());
    assertSame(nodes.get(3), east.getNodeB());
    assertTrue(nodes.get(1).getEdges().isEmpty());
    assertEquals(1, outside.received.size());
    var move = (MoveEdgeEndsAtVertexEvent) outside.received.get(0);
    assertEquals(List.of(west, east), move.getEdges());
    assertSame(nodes.get(1), move.getPreviousEndVertex());
    assertSame(nodes.get(3), move.getNewEndVertex());
  }

  /** A movement created without a direct ban reports none, also when a banned turn lies on its connection */
  @Test
  public void movementWithoutDirectBanReportsNoneWhateverTurnsAreBannedOnItsConnection() {
    var movement = junctionWithMovement();
    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2));

    assertFalse(movement.hasDirectBan());
    assertTrue(movement.getExplicitlyDirectlyBannedModes().isEmpty());
    assertFalse(movement.isModeDirectlyBanned(car));
  }

  /** A movement entering at one member node and leaving at another is not a turn, and its length is available. A deep
   * copy of the layer holds its own movements, referred to by its own intersections and referring to its own segments,
   * with the same length and direct ban */
  @Test
  public void movementIsNotATurnAndIsCopiedWithTheLayer() {
    var movement = junctionWithMovement();

    assertFalse(movement instanceof Turn);
    assertEquals(0.001, movement.getLengthKm());

    movement.banDirectly(List.of(car));

    var copy = layer.deepClone();

    assertEquals(1, copy.getSpatialMovements().size());
    var copiedMovement = copy.getSpatialMovements().getFirst();
    assertNotSame(movement, copiedMovement);
    assertSame(copiedMovement, ((SpatialIntersection) copy.getIntersections().getFirst()).getMovements().get(0));
    var copiedFrom = copy.getLinkSegments().get(movement.getLinkSegmentFrom().getId());
    assertSame(copiedFrom, copiedMovement.getLinkSegmentFrom());
    assertSame(copy.getLinkSegments().get(movement.getLinkSegmentTo().getId()), copiedMovement.getLinkSegmentTo());
    assertNotSame(movement.getLinkSegmentFrom(), copiedMovement.getLinkSegmentFrom());
    assertEquals(List.of(copiedMovement), copy.getSpatialMovements().getBySegment(copiedFrom));
    assertEquals(0.001, copiedMovement.getLengthKm());
    assertEquals(Set.of(car), copiedMovement.getExplicitlyDirectlyBannedModes());
  }

  /**
   * Recreating ids on a layer with three banned turns and two movements numbers the banned turns 0 to 2 and the
   * movements 0 and 1, each on their own, and syncing XML ids covers the movements
   * <pre>
   *   0 <--> 1 <--> 2 <--> 3      two independent movements with a direct ban, 0-1 to 2-3 and 3-2 to 1-0
   * </pre>
   */
  @Test
  public void recreatingIdsNumbersBannedTurnsAndMovementsEachFromZero() {
    createNodes(4);
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);
    var bans = layer.getBannedTurns().getFactory();
    var removedBan = bans.registerNew(segment(0, 1), segment(1, 0));
    bans.registerNew(segment(0, 1), segment(1, 2));
    bans.registerNew(segment(2, 1), segment(1, 0));
    bans.registerNew(segment(1, 2), segment(2, 3));
    var movements = layer.getSpatialMovements().getFactory();
    var removedMovement = movements.registerNew(segment(0, 1), segment(2, 3), 0.002);
    var eastbound = movements.registerNew(segment(0, 1), segment(2, 3), 0.002);
    var westbound = movements.registerNew(segment(3, 2), segment(1, 0), 0.002);
    eastbound.banDirectly(List.of());
    westbound.banDirectly(List.of());
    layer.getBannedTurns().remove(removedBan);                       // leaves gaps in the ids of both
    layer.getLayerModifier().removeSpatialMovement(removedMovement);

    MacroscopicNetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(network);

    assertEquals(List.of(0L, 1L, 2L), layer.getBannedTurns().stream().map(ban -> ban.getId()).sorted()
        .collect(Collectors.toList()));
    assertEquals(List.of(0L, 1L), layer.getSpatialMovements().stream().map(SpatialMovement::getId).sorted()
        .collect(Collectors.toList()));
    assertEquals(String.valueOf(eastbound.getId()), eastbound.getXmlId());
    assertEquals(String.valueOf(westbound.getId()), westbound.getXmlId());
  }

  /** Removing an internal segment leaves the movement as it was, its direct ban included */
  @Test
  public void internalSegmentRemovedLeavesMovementUnchanged() {
    var movement = junctionWithMovement();
    movement.banDirectly(List.of(bus));
    var from = movement.getLinkSegmentFrom();
    var to = movement.getLinkSegmentTo();

    layer.getLayerModifier().removeEdgeSegment(segment(1, 2));

    assertSame(movement, layer.getSpatialMovements().get(movement.getId()));
    assertSame(from, movement.getLinkSegmentFrom());
    assertSame(to, movement.getLinkSegmentTo());
    assertEquals(0.001, movement.getLengthKm());
    assertEquals(Set.of(bus), movement.getExplicitlyDirectlyBannedModes());
  }

  /** Adding a banned turn at a member node leaves the movement as it was */
  @Test
  public void bannedTurnAddedAtMemberNodeLeavesMovementUnchanged() {
    var movement = junctionWithMovement();
    var from = movement.getLinkSegmentFrom();
    var to = movement.getLinkSegmentTo();

    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2), List.of(bus));

    assertSame(movement, layer.getSpatialMovements().get(movement.getId()));
    assertEquals(List.of(movement), ((SpatialIntersection) intersections.getFirst()).getMovements());
    assertSame(from, movement.getLinkSegmentFrom());
    assertSame(to, movement.getLinkSegmentTo());
    assertEquals(0.001, movement.getLengthKm());
    assertFalse(movement.hasDirectBan());
  }

  /** Breaking the exit link at a new node, e.g. to place a stop, points the movement to the link segment starting
   * at the member node */
  @Test
  public void exitBrokenPointsMovementToLinkSegmentStartingAtMember() {
    var movement = junctionWithMovement();

    breakAt(exitLink, 3);

    assertSame(segment(2, 3), movement.getLinkSegmentTo());
    assertEquals(List.of(movement), layer.getSpatialMovements().getBySegment(segment(2, 3)));
    assertTrue(layer.getSpatialMovements().getBySegment(segment(3, 4)).isEmpty());
  }

  /** Removing an approach removes every movement from it, each with a removal event */
  @Test
  public void approachRemovedRemovesItsMovements() {
    var movement = junctionWithMovement();
    var junction = (SpatialIntersection) intersections.getFirst();
    createNodes(1);                                                  // node 5, a second exit at member node 1
    oneWay(1, 5);
    var corner = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(1, 5), 0);
    intersections.update(junction, intersection -> ((SpatialIntersection) intersection).addMovement(corner));
    var listener = new OutsideIntersectionListener(RemoveSpatialMovementEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeEdgeSegment(segment(0, 1));

    assertTrue(layer.getSpatialMovements().isEmpty());
    assertTrue(junction.getMovements().isEmpty());
    assertEquals(2, listener.received.size());
    assertEquals(Set.of(movement, corner), listener.received.stream()
        .map(event -> ((RemoveSpatialMovementEvent) event).getRemovedSpatialMovement()).collect(Collectors.toSet()));
  }

  /** Removing an intersection removes its movements, each with a removal event */
  @Test
  public void intersectionRemovedRemovesItsMovements() {
    var movement = junctionWithMovement();
    var listener = new OutsideIntersectionListener(RemoveSpatialMovementEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeIntersection(intersections.getFirst());

    assertTrue(layer.getSpatialMovements().isEmpty());
    assertEquals(1, listener.received.size());
    assertSame(movement, ((RemoveSpatialMovementEvent) listener.received.get(0)).getRemovedSpatialMovement());
  }

  /**
   * A spatial junction over member nodes 1 and 2 loses node 2. The movement to the exit at 2 goes with that exit, and
   * the junction, left with node 1, is replaced by a non-spatial intersection on it. The corner movement at 1 with a
   * direct ban for cars becomes a banned turn for cars, the corner movement without a direct ban leaves no trace, every
   * movement removal fires an event, and the banned turn node 1 already had stays
   * <pre>
   *            5
   *            ^
   *            |
   *   0 -----> 1 ===> 2 -----> 3      1-2 internal; movements from 0-1 to 2-3, to 1-5 (direct ban for cars) and
   *            |                      to 1-6 (a banned turn for every mode)
   *            v
   *            6
   * </pre>
   */
  @Test
  public void spatialIntersectionLeftWithOneMemberNodeBecomesNonSpatial() {
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    createNodes(7);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    oneWay(1, 5);
    oneWay(1, 6);
    var approach = segment(0, 1);
    var junction = spatialJunctionOver(1, 2);
    junction.addNewApproachSegment(approach);
    junction.addNewInternalSegment(segment(1, 2));
    var movements = layer.getSpatialMovements().getFactory();
    var through = movements.registerNew(approach, segment(2, 3), 0.001);
    var bannedCorner = movements.registerNew(approach, segment(1, 5), 0);
    bannedCorner.banDirectly(List.of(car));
    var corner = movements.registerNew(approach, segment(1, 6), 0);
    List.of(through, bannedCorner, corner).forEach(junction::addMovement);
    intersections.getFactory().register(junction);
    var existingBan = layer.getBannedTurns().getFactory().registerNew(approach, segment(1, 6));
    var listener = new OutsideIntersectionListener(
        RemoveSpatialMovementEvent.EVENT_TYPE, RemoveIntersectionEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeVertex(nodes.get(2));

    assertEquals(1, intersections.size());
    var replacement = intersections.getByMemberNode(nodes.get(1));
    assertTrue(replacement instanceof NonSpatialIntersection);
    assertEquals(List.of(approach), replacement.getApproachSegments());
    assertTrue(layer.getSpatialMovements().isEmpty());

    var bansFromApproach = layer.getBannedTurns().getBySegment(approach);
    assertEquals(2, bansFromApproach.size());
    assertTrue(bansFromApproach.stream().anyMatch(ban -> ban == existingBan));
    var carBan = (MacroscopicBannedTurn) bansFromApproach.stream()
        .filter(ban -> ban.getSegmentTo() == segment(1, 5)).findFirst().orElseThrow();
    assertEquals(Set.of(car), carBan.getExplicitlyBannedModes());

    var removedMovements = listener.received.stream().filter(event -> event instanceof RemoveSpatialMovementEvent)
        .map(event -> ((RemoveSpatialMovementEvent) event).getRemovedSpatialMovement()).collect(Collectors.toList());
    assertEquals(3, removedMovements.size());
    assertEquals(Set.of(through, bannedCorner, corner), Set.copyOf(removedMovements));
    assertTrue(listener.received.stream().anyMatch(event -> event instanceof RemoveIntersectionEvent &&
        ((RemoveIntersectionEvent) event).getRemovedIntersection() == junction));
  }

  /**
   * The non-spatial intersection replacing a spatial one left with one member node keeps the values of its approach,
   * with how they got their value
   * <pre>
   *   0 -----> 1 ===> 2 -----> 3      1-2 internal
   * </pre>
   */
  @Test
  public void spatialIntersectionLeftWithOneMemberNodeKeepsApproachValues() {
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    var approach = segment(0, 1);
    var junction = spatialJunctionOver(1, 2);
    junction.addNewApproachSegment(approach);
    junction.addNewInternalSegment(segment(1, 2));
    ((IntersectionSignalSegment) junction.getApproach(approach)).setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    junction.getApproach(approach).setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY);
    intersections.getFactory().register(junction);

    layer.getLayerModifier().removeVertex(nodes.get(2));

    var replacement = intersections.getByMemberNode(nodes.get(1));
    assertTrue(replacement instanceof NonSpatialIntersection);
    var handedOn = (IntersectionSignalSegment) replacement.getApproach(approach);
    assertEquals(0.44, handedOn.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, handedOn.getGreenRatioOrigin());
    assertEquals(0.95, handedOn.getSaturationFlowFactor());
    assertEquals(CapacityFactorOrigin.SET_DIRECTLY, handedOn.getSaturationFlowFactorOrigin());
  }

  // INDEPENDENT MOVEMENTS

  /**
   * A ban from segment 0-1 to segment 2-3 passes nodes 1 and 2, which belong to no intersection: it is an independent
   * movement with a direct ban, held by the layer's movements, and no intersection refers to it
   * <pre>
   *   0 ---> 1 ---> 2 ---> 3
   * </pre>
   */
  @Test
  public void banAcrossTwoNodesIsAnIndependentMovement() {
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);

    var independent = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(2, 3), 0.002);
    independent.banDirectly(List.of());

    assertTrue(independent.hasDirectBan());
    assertTrue(independent.isModeDirectlyBanned(car));
    assertEquals(List.of(independent), layer.getSpatialMovements().getBySegment(segment(0, 1)));
    assertEquals(List.of(independent), layer.getSpatialMovements().getBySegment(segment(2, 3)));
    assertTrue(intersections.isEmpty());
  }

  /** Removing the to segment of an independent movement removes the movement, with a removal event */
  @Test
  public void independentMovementRemovedWithItsToSegment() {
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    var independent = layer.getSpatialMovements().getFactory().registerNew(segment(0, 1), segment(2, 3), 0.002);
    independent.banDirectly(List.of());
    var listener = new OutsideIntersectionListener(RemoveSpatialMovementEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeEdgeSegment(segment(2, 3));

    assertTrue(layer.getSpatialMovements().isEmpty());
    assertEquals(1, listener.received.size());
    assertSame(independent, ((RemoveSpatialMovementEvent) listener.received.get(0)).getRemovedSpatialMovement());
  }

  // MODES A MOVEMENT IS BANNED FOR

  /** Cars and buses use the approach and the exit, and the only connection between them passes a turn at member node 1
   * banned for buses: the movement is banned implicitly for buses only, and the movement itself is unchanged */
  @Test
  public void turnBannedForBusesAtMemberNodeBansTheMovementImplicitlyForBuses() {
    var movement = junctionWithMovement();
    assertTrue(bannedModes(movement).isEmpty());

    layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2), List.of(bus));

    assertEquals(Set.of(bus), bannedModes(movement));
    assertFalse(movement.hasDirectBan());
    assertEquals(0.001, movement.getLengthKm());
  }

  /** Cars and buses use the movement, and a direct ban for cars bans it for cars only */
  @Test
  public void directBanForCarsBansTheMovementForCarsOnly() {
    var movement = junctionWithMovement();

    movement.banDirectly(List.of(car));

    assertTrue(movement.isModeDirectlyBanned(car));
    assertFalse(movement.isModeDirectlyBanned(bus));
    assertEquals(Set.of(car), bannedModes(movement));
  }

  /** The modes a movement is banned for are worked out each time they are asked for, so they follow a banned turn
   * being removed and an internal segment losing access for a mode, while the movement stays as it is */
  @Test
  public void bannedModesOfAMovementFollowChangesToTheNetwork() {
    var movement = junctionWithMovement();
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(0, 1), segment(1, 2), List.of(bus));
    assertEquals(Set.of(bus), bannedModes(movement));

    layer.getBannedTurns().remove(ban);
    assertTrue(bannedModes(movement).isEmpty());

    var busOnly = layer.getLinkSegmentTypes().getFactory().registerNew("busOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(busOnly, 50, List.of(bus));
    segment(1, 2).setLinkSegmentType(busOnly);
    assertEquals(Set.of(car), bannedModes(movement));
    assertSame(movement, layer.getSpatialMovements().get(movement.getId()));
    assertEquals(0.001, movement.getLengthKm());
  }

  /** A listener of removed link segments can still ask the removed segment for both its end nodes, the segment being
   * taken off its link only after the listeners have run */
  @Test
  public void removedSegmentKnowsItsEndNodesWhileListenersRun() {
    createNodes(2);
    var link = oneWay(0, 1);
    var removed = segment(0, 1);
    var endNodesSeen = new ArrayList<Object>();
    layer.getLayerModifier().addListener(new DirectedGraphModifierListener() {
      @Override public void onGraphModificationEvent(GraphModificationEvent event) { }
      @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
        var segment = ((RemoveEdgeSegmentEvent) event).getRemovedEdgeSegment();
        endNodesSeen.add(segment.getUpstreamVertex());
        endNodesSeen.add(segment.getDownstreamVertex());
      }
    }, RemoveEdgeSegmentEvent.EVENT_TYPE);

    layer.getLayerModifier().removeEdgeSegment(removed);

    assertEquals(List.of(nodes.get(0), nodes.get(1)), endNodesSeen);
    assertFalse(link.hasEdgeSegmentAb());
    assertFalse(removed.hasParent());
  }

  /** An internal graph listener on the layer modifier keeps hearing after the other listeners are removed, and hears
   * nothing once removed itself */
  @Test
  public void internalGraphListenerSurvivesClearingAndStopsOnceRemoved() {
    createNodes(3);
    oneWay(0, 1);
    oneWay(1, 2);
    var heard = new ArrayList<DirectedGraphModificationEvent>();
    DirectedGraphModifierListener internal = new DirectedGraphModifierListener() {
      @Override public void onGraphModificationEvent(GraphModificationEvent event) { }
      @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) { heard.add(event); }
      @Override public EventType[] getKnownSupportedEventTypes() {
        return new EventType[] { RemoveEdgeSegmentEvent.EVENT_TYPE };
      }
    };
    var modifier = layer.getLayerModifier();
    modifier.addInternalListener(internal);
    modifier.removeAllNonInternalListeners();

    modifier.removeEdgeSegment(segment(0, 1));
    assertEquals(1, heard.size());

    modifier.removeInternalListener(internal);
    modifier.removeEdgeSegment(segment(1, 2));
    assertEquals(1, heard.size());
  }

  /** An internal link segment type listener on the layer modifier keeps hearing after the other listeners are removed,
   * and hears nothing once removed itself */
  @Test
  public void internalLinkSegmentTypeListenerSurvivesClearingAndStopsOnceRemoved() {
    createNodes(2);
    oneWay(0, 1);
    var road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    var lane = layer.getLinkSegmentTypes().getFactory().registerNew("lane", 900, 90);
    var heard = new ArrayList<LinkSegmentTypeModificationEvent>();
    LinkSegmentTypeModifierListener internal = new LinkSegmentTypeModifierListener() {
      @Override public void onLinkSegmentTypeModifierEvent(LinkSegmentTypeModificationEvent event) { heard.add(event); }
      @Override public EventType[] getKnownSupportedEventTypes() {
        return new EventType[] { ChangeLinkSegmentTypeEvent.EVENT_TYPE };
      }
    };
    var modifier = layer.getLayerModifier();
    modifier.addInternalListener(internal);
    modifier.removeAllNonInternalListeners();

    modifier.changeLinkSegmentType(segment(0, 1), road);
    assertEquals(1, heard.size());

    modifier.removeInternalListener(internal);
    modifier.changeLinkSegmentType(segment(0, 1), lane);
    assertEquals(1, heard.size());
  }

  /**
   * Removing infrastructure without mode access removes a link segment type no mode may use once its link segments are
   * gone, and an outside listener hears of it after the removal of the link segment that carried it ("Link segment type
   * removed with infrastructure without mode access")
   */
  @Test
  public void linkSegmentTypeRemovedWithInfrastructureWithoutModeAccess() {
    createNodes(2);
    oneWay(0, 1);
    var closed = layer.getLinkSegmentTypes().getFactory().registerNew("closed", 1800, 180);
    segment(0, 1).setLinkSegmentType(closed);
    var heard = new ArrayList<Event>();
    var modifier = layer.getLayerModifier();
    modifier.addListener(new DirectedGraphModifierListener() {
      @Override public void onGraphModificationEvent(GraphModificationEvent event) { }
      @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) { heard.add(event); }
      @Override public EventType[] getKnownSupportedEventTypes() { return new EventType[0]; }
    }, RemoveEdgeSegmentEvent.EVENT_TYPE);
    modifier.addListener(new LinkSegmentTypeModifierListener() {
      @Override public void onLinkSegmentTypeModifierEvent(LinkSegmentTypeModificationEvent event) { heard.add(event); }
      @Override public EventType[] getKnownSupportedEventTypes() { return new EventType[0]; }
    }, RemoveLinkSegmentTypeEvent.EVENT_TYPE);

    modifier.removeInfrastructureWithoutModeAccess();

    assertNull(layer.getLinkSegmentTypes().get(closed.getId()));
    assertEquals(2, heard.size());
    assertTrue(heard.get(0) instanceof RemoveEdgeSegmentEvent);
    assertSame(closed, ((RemoveLinkSegmentTypeEvent) heard.get(1)).getRemovedLinkSegmentType());
  }

  // FIRED EVENT TYPES

  /**
   * The layer modifier gives the removal of a toll road and the recreation of toll road ids as its toll road event
   * types, and no type of another kind ("Toll road events of the layer modifier")
   */
  @Test
  public void tollRoadEventsOfTheLayerModifier() {
    assertEquals(Set.of(RemoveTollRoadEvent.EVENT_TYPE, RecreatedTollRoadsManagedIdsEvent.EVENT_TYPE),
        layer.getLayerModifier().getFiredTollRoadEventTypes());
  }

  /**
   * The layer modifier gives its graph, directed graph, intersection, link segment type and toll road event types
   * together ("Every type of the layer modifier")
   */
  @Test
  public void everyTypeOfTheLayerModifier() {
    var expected = new HashSet<EventType>(GraphModifierEventProducer.FIRED_GRAPH_EVENT_TYPES);
    expected.addAll(DirectedGraphModifierEventProducer.FIRED_DIRECTED_GRAPH_EVENT_TYPES);
    expected.addAll(IntersectionModifierEventProducer.FIRED_INTERSECTION_EVENT_TYPES);
    expected.addAll(LinkSegmentTypeModifierEventProducer.FIRED_LINK_SEGMENT_TYPE_EVENT_TYPES);
    expected.addAll(TollRoadModifierEventProducer.FIRED_TOLL_ROAD_EVENT_TYPES);
    assertEquals(expected, layer.getLayerModifier().getFiredEventTypes());
  }

  /**
   * A toll road listener also supporting directed graph, intersection and link segment type event types, registered on
   * the layer modifier's toll road events for every type it supports, hears the removal of a toll road ("Listener of
   * several kinds")
   */
  @Test
  public void listenerOfSeveralKinds() {
    createNodes(2);
    oneWay(0, 1);
    var tollRoad = layer.getTollRoads().getFactory().registerNew(
        List.of(segment(0, 1)), PricedModeSelection.unspecified());
    var listener = new OutsideTollRoadListener(RemoveEdgeSegmentEvent.EVENT_TYPE, RemoveIntersectionEvent.EVENT_TYPE,
        ChangeLinkSegmentTypeEvent.EVENT_TYPE, RemoveTollRoadEvent.EVENT_TYPE);
    layer.getLayerModifier().addListener(listener);

    layer.getLayerModifier().removeEdgeSegment(segment(0, 1));

    assertEquals(1, listener.received.size());
    assertSame(tollRoad, ((RemoveTollRoadEvent) listener.received.get(0)).getRemovedTollRoad());
  }

  /**
   * A toll road listener supporting only zoning event types is refused by the layer modifier's toll road events
   * ("Listener of types never fired")
   */
  @Test
  public void listenerOfTypesNeverFired() {
    var listener = new OutsideTollRoadListener(RemoveConnectoidEvent.EVENT_TYPE);

    assertThrows(IllegalArgumentException.class, () -> layer.getLayerModifier().addListener(listener));
  }
}
