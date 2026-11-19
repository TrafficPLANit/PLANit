package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.modifier.event.RemoveEdgeSegmentEvent;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.CollapseSpatialIntersectionEvent;
import org.goplanit.test.zoning.ZoningLossEventRecorder;
import org.goplanit.utils.event.EventState;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapserSettings;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.modifier.event.RemoveConnectoidAllowedModeEvent;
import org.goplanit.zoning.modifier.event.RemoveConnectoidEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests collapsing chosen modes onto one centre node per spatial intersection after consolidation, and how their bans
 * are carried over
 *
 * @author markr
 */
public class CollapseTest extends IntersectionConsolidationTestBase {

  /** indices of the footway ends P and Q added by {@link #dividedRoadsCrossingWithFootways(IntConsumer)} */
  private static final int P = 12, Q = 13;

  /**
   * register a stop: a waiting area, and a transfer connectoid named and with external id "stop" at the node the segment
   * enters, or leaves for an egress entry, with a vehicle stop entry over it allowing the given modes explicitly, or
   * every mode when none are given
   */
  private static TransferConnectoid stopReachedOver(
      Zoning zoning, MacroscopicLinkSegment segment, boolean entersItsNode, Mode... modes) {
    var waitingArea = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var stop = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        waitingArea, entersItsNode, segment, ZoneConnectoidType.PT_VEHICLE_STOP);
    stop.setName("stop");
    stop.setExternalId("stop");
    List.of(modes).forEach(onlyEntryOf(stop)::addExplicitAllowedMode);
    return stop;
  }

  /** the one entry of a transfer connectoid, refused when it has another number */
  private static DirectedConnectoidAccessZoneEntry onlyEntryOf(TransferConnectoid connectoid) {
    assertEquals(1, connectoid.getNumberOfAccessZoneEntries());
    return (DirectedConnectoidAccessZoneEntry) connectoid.getFirstAccessZoneEntry();
  }

  /** collapse the given modes on the given spatial intersections, no node excluded */
  private IntersectionCollapseModifierResult collapse(
      List<SpatialIntersection> spatialIntersections, List<Mode> collapsedModes) {
    return collapse(spatialIntersections, collapsedModes, IdMapperType.EXTERNAL_ID);
  }

  /** collapse the given modes on the given spatial intersections, excluding the given nodes, each by the given kind of
   * id */
  private IntersectionCollapseModifierResult collapse(List<SpatialIntersection> spatialIntersections,
      List<Mode> collapsedModes, IdMapperType nodeIdType, String... nodeIdsExcluded) {
    var settings = new IntersectionCollapserSettings();
    settings.setCollapsedModes(collapsedModes);
    settings.addNodeIdsExcludedFromCollapse(List.of(nodeIdsExcluded), nodeIdType);
    return layer.getLayerModifier().collapseIntersectionsToNonSpatialForModes(
        spatialIntersections, settings, PlanitJtsCrsUtils.CARTESIANCRS);
  }

  /** true when the turn from one segment onto the other is banned for the mode */
  private boolean isTurnBanned(MacroscopicLinkSegment from, MacroscopicLinkSegment onto, Mode mode) {
    return MacroscopicNetworkLayerUtils.isTurnBannedForMode(layer, from, onto, mode);
  }

  /** consolidate with the default values, but with active modes in the movements, since a collapsed pedestrian keeps
   * only the turns its movements give it */
  private IntersectionConsolidationModifierResult consolidateWithActiveModesInMovements() {
    return consolidateWith(settings -> settings.setIncludeActiveModesInMovements(true));
  }

  /** consolidate with active modes in the movements, then collapse the given modes on every spatial intersection */
  private IntersectionCollapseModifierResult consolidateThenCollapse(List<Mode> collapsedModes) {
    consolidateWithActiveModesInMovements();
    return collapse(layer.getIntersections().streamSpatial().collect(Collectors.toList()), collapsedModes);
  }

  /**
   * The divided roads crossing, its four corners signalised, with a footway crossing the westbound carriageway between
   * A and B, and footways from P to A and from Q to B. Roads allow cars only, footways pedestrians only
   * <pre>
   *     P                Q        P (-20, 30), Q (35, 30)
   *       \            /
   *         A ~~~~~~ B            ~ footway, 15 m
   *         C        D            roads as in dividedRoadsCrossing
   * </pre>
   *
   * @param signalisedJunctionAt registers the signalised junction at each corner, with the approaches it is to have
   */
  private void dividedRoadsCrossingWithFootways(IntConsumer signalisedJunctionAt) {
    dividedRoadsCrossing();
    node(-20, 30);
    node(35, 30);
    link(A, B, 15, true, footway);
    link(P, A, 25, true, footway);
    link(Q, B, 25, true, footway);
    List.of(A, B, C, D).forEach(signalisedJunctionAt::accept);
  }

  /** register a link segment type allowing cars and pedestrians */
  private MacroscopicLinkSegmentType carAndPedestrianType() {
    var carAndPedestrian = layer.getLinkSegmentTypes().getFactory().registerNew("shared", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carAndPedestrian, 50, List.of(car, pedestrian));
    return carAndPedestrian;
  }


  /**
   * Collapsing pedestrians moves the footways P-A and Q-B to a new centre node, and the footway between A and B, left
   * without access, is removed. The roads still meet at the four member nodes, and the movements stay as they were:
   * 16 for cars and 2 between the footways, the left turn for cars still not banned for them
   */
  @Test
  public void footwaysCollapsedRoadsKept() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var footwayAtA = layer.getLinks().stream().filter(link -> link.getNodeA() == nodes.get(P))
        .findFirst().orElseThrow();

    var result = consolidateThenCollapse(List.of(pedestrian));

    var consolidated = onlySpatialIntersection();
    assertEquals(5, consolidated.getMemberNodes().size());
    var centre = footwayAtA.getNodeB();
    assertTrue(consolidated.hasMemberNode(centre));
    assertFalse(List.of(A, B, C, D).stream().anyMatch(index -> nodes.get(index) == centre));
    assertEquals(2, result.getLinksMovedToCentreNode().size());
    assertTrue(layer.getLinks().stream().noneMatch(
        link -> Set.of(link.getNodeA(), link.getNodeB()).equals(Set.of(nodes.get(A), nodes.get(B))) &&
            link.getLinkSegmentAb().getLinkSegmentType() == footway));
    assertTrue(consolidated.getInternalSegments().containsAll(
        List.of(segment(A, C), segment(B, A), segment(D, B), segment(C, D))));
    assertEquals(18, consolidated.getMovements().size());
    assertEquals(18, layer.getSpatialMovements().size());
    var leftTurn = movementBetween(consolidated, segment(4, A), segment(D, 11));
    assertEquals(0.030, leftTurn.getLengthKm(), 1e-9);
    assertTrue(bannedModes(consolidated, leftTurn).isEmpty());
    assertTrue(result.getLinksAddedToCentreNode().isEmpty());
  }

  /**
   * Two divided roads crossings 500 m apart, each with a footway between its corners A and B, collapsed for pedestrians.
   * Each intersection is reported as started once, then both as done once, each done with the centre node and member
   * nodes it started with. When the first starts both footways allow pedestrians, when the second starts only its own
   * still does, and when each is done both footways, left without access, are gone
   * <pre>
   *     A ~~~~ B                    A2 ~~~~ B2        ~ footway, 15 m
   *     C      D    (500 m east)    C2      D2        roads as in dividedRoadsCrossing
   * </pre>
   */
  @Test
  public void eachCollapsedIntersectionIsReportedAsStartedThenDone() {
    dividedRoadsCrossing();
    int[] far = dividedRoadsCrossingAt(500, 0);
    var footways = List.of(link(A, B, 15, true, footway), link(far[0], far[1], 15, true, footway));
    for (int corner : List.of(A, B, C, D, far[0], far[1], far[2], far[3])) {
      signalisedJunctionAt(corner);
    }
    consolidateWithActiveModesInMovements();
    var spatial = layer.getIntersections().streamSpatial().collect(Collectors.toList());
    var heard = new ArrayList<CollapseSpatialIntersectionEvent>();
    var footwaysAllowingPedestriansWhenHeard = new ArrayList<Long>();
    layer.getLayerModifier().addListener(new IntersectionModifierListener() {
      @Override public void onIntersectionModifierEvent(IntersectionModificationEvent event) {
        heard.add((CollapseSpatialIntersectionEvent) event);
        footwaysAllowingPedestriansWhenHeard.add(footways.stream().filter(link ->
            layer.getLinks().get(link.getId()) == link && link.getLinkSegmentAb().isModeAllowed(pedestrian)).count());
      }
    }, CollapseSpatialIntersectionEvent.EVENT_TYPE);

    collapse(spatial, List.of(pedestrian));

    assertEquals(List.of(EventState.START, EventState.START, EventState.DONE, EventState.DONE),
        heard.stream().map(event -> event.getState().orElseThrow()).collect(Collectors.toList()));
    assertEquals(List.of(spatial.get(0), spatial.get(1), spatial.get(0), spatial.get(1)),
        heard.stream().map(CollapseSpatialIntersectionEvent::getIntersection).collect(Collectors.toList()));
    assertEquals(List.of(2L, 1L, 0L, 0L), footwaysAllowingPedestriansWhenHeard);
    for (int index = 0; index < 2; ++index) {
      var started = heard.get(index);
      var done = heard.get(index + 2);
      assertSame(started.getCentreNode(), done.getCentreNode());
      assertEquals(started.getMemberNodes(), done.getMemberNodes());
      assertEquals(4, started.getMemberNodes().size());
      assertFalse(started.getMemberNodes().contains(started.getCentreNode()));
      assertEquals(List.of(pedestrian), started.getCollapsedModes());
    }
    assertEquals(Set.of(nodes.get(A), nodes.get(B), nodes.get(C), nodes.get(D)),
        Set.copyOf(heard.get(0).getMemberNodes()));
  }

  /**
   * A transfer connectoid of a zoning on the network at corner A, reached only over the footway from P, moves with that
   * footway to the centre node when pedestrians are collapsed, and nothing is lost
   */
  @Test
  public void connectoidOnAFootwayMovedToTheCentreFollowsIt() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var waitingArea = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var fromP = segment(P, A);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        waitingArea, true, fromP, ZoneConnectoidType.PT_VEHICLE_STOP);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    assertNotSame(nodes.get(A), connectoid.getReferenceVertex());
    assertSame(fromP.getDownstreamVertex(), connectoid.getReferenceVertex());
    assertEquals(List.of(fromP), connectoid.getAsDirectedAccessZoneEntry(
        waitingArea, ZoneConnectoidType.PT_VEHICLE_STOP).getAccessLinkSegments());
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * A stop for pedestrians at corner B, reached over the footway from A, allowing pedestrians explicitly. Collapsing
   * pedestrians withdraws them from that footway and then removes it. When it is removed the stop's connectoid is still
   * at B with its entry over the footway, allowing pedestrians, and no zoning event has fired, since the zoning changes
   * only once the intersection is done
   */
  @Test
  public void stopOnAnInternalSegmentIsLeftAloneWhileCollapsing() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var waitingArea = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var internal = segment(A, B);
    var connectoid = zoning.getTransferConnectoids().getFactory().registerNewWithDirectedEntry(
        waitingArea, true, internal, ZoneConnectoidType.PT_VEHICLE_STOP);
    var entry = connectoid.getAsDirectedAccessZoneEntry(waitingArea, ZoneConnectoidType.PT_VEHICLE_STOP);
    entry.addExplicitAllowedMode(pedestrian);
    var recorder = new ZoningLossEventRecorder(zoning);
    var leftAloneWhenRemoved = new ArrayList<Boolean>();
    layer.getLayerModifier().addListener(new DirectedGraphModifierListener() {
      @Override public void onGraphModificationEvent(GraphModificationEvent event) {}
      @Override public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
        if (((RemoveEdgeSegmentEvent) event).getRemovedEdgeSegment() == internal) {
          leftAloneWhenRemoved.add(zoning.getTransferConnectoids().getByAccessNode(nodes.get(B)).contains(connectoid)
              && entry.hasAccessLinkSegment(internal) && entry.isModeAllowed(pedestrian)
              && recorder.getHeardTypes().isEmpty());
        }
      }
    }, RemoveEdgeSegmentEvent.EVENT_TYPE);

    consolidateThenCollapse(List.of(pedestrian));

    assertEquals(List.of(true), leftAloneWhenRemoved);
  }

  /**
   * The road from 8 into B also allows pedestrians, and a stop at A, allowing pedestrians, is reached over the footway
   * from B, which pedestrians reach from that road and from the footway from Q. Collapsing pedestrians removes the
   * footway between the corners. The stop's connectoid, left with nothing at A, is at the centre node with its one
   * entry, reached over the link collapse adds from B into the centre and over the footway from Q, which now ends at
   * the centre, and nothing is reported lost
   */
  @Test
  public void stopReachedFromASharedRoadAndAFootwayMovesToTheCentre() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    segment(8, B).setLinkSegmentType(carAndPedestrian);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var stop = stopReachedOver(zoning, segment(B, A, footway), true, pedestrian);
    var fromQ = segment(Q, B);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    var centre = fromQ.getDownstreamVertex();
    assertSame(centre, stop.getReferenceVertex());
    assertEquals(Set.of(fromQ, segment(nodes.get(B), centre)), Set.copyOf(onlyEntryOf(stop).getAccessLinkSegments()));
    assertTrue(onlyEntryOf(stop).isModeAllowed(pedestrian));
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * The road from B to A, inside the intersection, also allows pedestrians, and a stop at A allowing cars and
   * pedestrians is reached over it. Collapsing pedestrians leaves that road to cars, so the stop's connectoid keeps it
   * for cars, while a connectoid split off it at the centre node, with the same name and external id, has the stop's
   * entry for pedestrians, over the footways from P and Q, which reach the road and now end at the centre. Nothing is
   * reported lost
   */
  @Test
  public void modeNotCollapsedKeepsTheInternalSegment() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var internalRoad = segment(B, A, road);
    internalRoad.setLinkSegmentType(carAndPedestrian);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var stop = stopReachedOver(zoning, internalRoad, true, car, pedestrian);
    var fromP = segment(P, A);
    var fromQ = segment(Q, B);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    var centre = fromQ.getDownstreamVertex();
    assertSame(nodes.get(A), stop.getReferenceVertex());
    var kept = onlyEntryOf(stop);
    assertEquals(List.of(internalRoad), List.copyOf(kept.getAccessLinkSegments()));
    assertTrue(kept.isModeAllowed(car));
    assertFalse(kept.isModeAllowed(pedestrian));
    var atCentre = zoning.getTransferConnectoids().getByAccessNode(centre);
    assertEquals(1, atCentre.size());
    var split = atCentre.get(0);
    assertEquals("stop", split.getName());
    assertEquals("stop", split.getExternalId());
    var moved = onlyEntryOf(split);
    assertSame(kept.getAccessZone(), moved.getAccessZone());
    assertEquals(Set.of(fromP, fromQ), Set.copyOf(moved.getAccessLinkSegments()));
    assertTrue(moved.isModeAllowed(pedestrian));
    assertFalse(moved.isModeAllowed(car));
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * As {@link #stopReachedFromASharedRoadAndAFootwayMovesToTheCentre()}, but pedestrians may not turn from the road
   * from 8 onto the footway from B to A. That road then no longer serves the stop, which is reached at the centre node
   * over the footway from Q only
   */
  @Test
  public void bannedTurnKeepsAnApproachFromServingTheStop() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    segment(8, B).setLinkSegmentType(carAndPedestrian);
    layer.getBannedTurns().getFactory().registerNew(segment(8, B), segment(B, A, footway), List.of(pedestrian));
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var stop = stopReachedOver(zoning, segment(B, A, footway), true, pedestrian);
    var fromQ = segment(Q, B);

    consolidateThenCollapse(List.of(pedestrian));

    assertSame(fromQ.getDownstreamVertex(), stop.getReferenceVertex());
    assertEquals(List.of(fromQ), List.copyOf(onlyEntryOf(stop).getAccessLinkSegments()));
  }

  /**
   * The road from B to A, inside the intersection, also allows pedestrians, and a stop at A allowing cars and
   * pedestrians is reached over it; no footway leads into the intersection, so pedestrians reach that road from no
   * approach. Collapsing cars and pedestrians removes the corners. The stop's connectoid is at the centre node, its
   * entry reached over the four roads into the centre, from each of which cars reached the road, and allowing cars
   * only; the loss of pedestrian access is reported once
   */
  @Test
  public void onlyModesAnApproachBringsMove() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var internalRoad = segment(B, A);
    internalRoad.setLinkSegmentType(carAndPedestrian);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var stop = stopReachedOver(zoning, internalRoad, true, car, pedestrian);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(car, pedestrian));

    var centre = onlyNonSpatialIntersection().getNode();
    assertSame(centre, stop.getReferenceVertex());
    var entry = onlyEntryOf(stop);
    assertEquals(4, entry.getAccessLinkSegments().size());
    assertTrue(entry.getAccessLinkSegments().stream().allMatch(segment -> segment.getDownstreamVertex() == centre));
    assertTrue(entry.isModeAllowed(car));
    assertFalse(entry.isModeAllowed(pedestrian));
    assertEquals(List.of(RemoveConnectoidAllowedModeEvent.EVENT_TYPE), recorder.getHeardTypes());
  }

  /**
   * A stop at A whose egress entry leaves over the footway to B, from which pedestrians reach the footway from B to Q.
   * Collapsing pedestrians moves that footway's end at B to the centre node. The stop's connectoid, left with nothing at
   * A, is at the centre, its entry over that footway, which now starts at the centre, and nothing is reported lost
   */
  @Test
  public void egressEntryFollowsTheExits() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var stop = stopReachedOver(zoning, segment(A, B), false);
    var toQ = segment(B, Q);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    var centre = toQ.getUpstreamVertex();
    assertNotSame(nodes.get(B), centre);
    assertSame(centre, stop.getReferenceVertex());
    assertEquals(List.of(toQ), List.copyOf(onlyEntryOf(stop).getAccessLinkSegments()));
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * A stop at B, allowing pedestrians, reached over the footway from A, inside a crossing no other footway leads into,
   * so pedestrians reach it from no approach. Collapsing pedestrians removes that footway, and the stop's connectoid
   * goes with it, reported once
   */
  @Test
  public void stopNothingReachesIsLostOnce() {
    dividedRoadsCrossing();
    link(A, B, 15, true, footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    stopReachedOver(zoning, segment(A, B), true, pedestrian);
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(RemoveConnectoidEvent.EVENT_TYPE), recorder.getHeardTypes());
  }

  /**
   * A stop at A reached over the footway from P and over the footway from B, which pedestrians reach from Q, its
   * connectoid also giving access to another waiting area over the road from 4. Collapsing pedestrians moves the
   * footway from P to the centre node, splitting the connectoid there, and the stop's access over the footway from B
   * joins that split: one connectoid at the centre, reached over the footways from P and Q, while the connectoid keeps
   * the other waiting area at A, and nothing is reported lost
   */
  @Test
  public void movedLinkAndInternalSegmentEndOnOneConnectoidAtTheCentre() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var fromP = segment(P, A);
    var fromQ = segment(Q, B);
    var stop = stopReachedOver(zoning, fromP, true);
    var stopArea = onlyEntryOf(stop).getAccessZone();
    onlyEntryOf(stop).addAccessLinkSegment(segment(B, A, footway));
    var otherArea = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    stop.createDirectedAccessZoneEntry(otherArea, ZoneConnectoidType.PT_VEHICLE_STOP, segment(4, A));
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(pedestrian));

    var atCentre = zoning.getTransferConnectoids().getByAccessNode(fromQ.getDownstreamVertex());
    assertEquals(1, atCentre.size());
    var moved = onlyEntryOf(atCentre.get(0));
    assertSame(stopArea, moved.getAccessZone());
    assertEquals(Set.of(fromP, fromQ), Set.copyOf(moved.getAccessLinkSegments()));
    assertSame(nodes.get(A), stop.getReferenceVertex());
    assertSame(otherArea, onlyEntryOf(stop).getAccessZone());
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * Collapsing cars and pedestrians removes the four corners, see {@link #everyModeCollapsedLeavesOneNode()}. A transfer
   * connectoid with an undirected access and egress entry at corner C, and an OD connectoid at corner D, are at the
   * centre node with their entries once the intersection is done, and nothing is reported lost
   */
  @Test
  public void connectoidsAtRemovedMemberNodesMoveToTheCentre() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    var waitingArea = zoning.getTransferZones().getFactory().registerNew(TransferZoneType.POLE, true);
    var transfer = zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(C));
    transfer.createUndirectedAccessZoneEntry(waitingArea, ZoneConnectoidType.ZONE_ACCESS_EGRESS);
    var odZone = zoning.getOdZones().getFactory().registerNew();
    var od = zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(odZone, nodes.get(D));
    var recorder = new ZoningLossEventRecorder(zoning);

    consolidateThenCollapse(List.of(car, pedestrian));

    var centre = onlyNonSpatialIntersection().getNode();
    assertSame(centre, transfer.getReferenceVertex());
    assertSame(centre, od.getReferenceVertex());
    assertTrue(transfer.hasAccessZoneEntry(waitingArea, ZoneConnectoidType.ZONE_ACCESS_EGRESS));
    assertTrue(od.hasAccessZoneEntry(odZone));
    assertEquals(List.of(transfer), zoning.getTransferConnectoids().getByAccessNode(centre));
    assertEquals(List.of(od), zoning.getOdConnectoids().getByAccessNode(centre));
    assertTrue(recorder.getHeardTypes().isEmpty());
  }

  /**
   * Collapsing pedestrians keeps the corners for cars. A transfer connectoid at corner C, removed with C after the
   * intersection is done, goes with it, reported as a loss once
   */
  @Test
  public void connectoidAtAMemberNodeRemovedAfterCollapseIsRemoved() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), network);
    zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(C));
    consolidateThenCollapse(List.of(pedestrian));
    var recorder = new ZoningLossEventRecorder(zoning);

    layer.getLayerModifier().removeVertex(nodes.get(C));

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(RemoveConnectoidEvent.EVENT_TYPE), recorder.getHeardTypes());
  }

  /**
   * The road arriving at B from the east also allows pedestrians. Collapsing pedestrians keeps that road on B, and
   * joins the centre to B with a link for pedestrians only, so they can still reach the collapsed footways
   */
  @Test
  public void sharedRoadKeepsTheCollapsedModeConnected() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    segment(8, B).setLinkSegmentType(carAndPedestrian);

    var result = consolidateThenCollapse(List.of(pedestrian));

    assertEquals(1, result.getLinksAddedToCentreNode().size());
    var added = result.getLinksAddedToCentreNode().get(0);
    assertTrue(Set.of(added.getNodeA(), added.getNodeB()).contains(nodes.get(B)));
    for (var segment : added.getEdgeSegments()) {
      assertEquals(Set.of(pedestrian), ((MacroscopicLinkSegment) segment).getAllowedModes());
      assertTrue(onlySpatialIntersection().getInternalSegments().contains((MacroscopicLinkSegment) segment));
    }
    assertSame(nodes.get(B), segment(8, B).getDownstreamVertex());
  }

  /**
   * Collapsing every mode leaves one node: a non-spatial intersection on the centre node replaces the spatial one,
   * every arm ends at the centre, and the inner links, the former member nodes and the movements are gone
   */
  @Test
  public void everyModeCollapsedLeavesOneNode() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var corners = List.of(nodes.get(A), nodes.get(B), nodes.get(C), nodes.get(D));

    consolidateThenCollapse(List.of(car, pedestrian));

    var consolidated = onlyNonSpatialIntersection();
    assertEquals(1, consolidated.getMemberNodes().size());
    var centre = consolidated.getNode();
    assertEquals(10, centre.getEdges().size());
    corners.forEach(corner -> assertNull(layer.getNodes().get(corner.getId())));
    assertEquals(10, layer.getLinks().size());
    assertTrue(layer.getSpatialMovements().isEmpty());
  }

  /**
   * Cars may not turn from A-C onto C-D at member node C, which stops them going from the northern approach to the
   * eastern exit. Once every mode is collapsed, that ban is carried to the centre node: the turn there from the
   * northern approach onto the eastern exit is banned for cars, while the turn onto the southern exit, which cars could
   * make, is not
   */
  @Test
  public void banCarriedToTheCentreNode() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D), List.of(car));
    var north = segment(4, A);
    var east = segment(D, 11);
    var south = segment(C, 5);

    var result = consolidateThenCollapse(List.of(car, pedestrian));

    var centre = onlyNonSpatialIntersection().getNode();
    var bansFromNorth = layer.getBannedTurns().getBySegment(north).stream()
        .filter(ban -> ban.getSegmentFrom() == north).collect(Collectors.toList());
    var northToEast = (MacroscopicBannedTurn) bansFromNorth.stream()
        .filter(ban -> ban.getSegmentTo() == east).findFirst().orElseThrow();
    assertSame(centre, northToEast.getCentreVertex());
    assertTrue(northToEast.isModeBanned(car));
    assertTrue(result.getBannedTurnsAddedOrExtendedForCollapsedModes().stream().anyMatch(ban -> ban == northToEast));
    assertTrue(bansFromNorth.stream().noneMatch(ban -> ban.getSegmentTo() == south));
  }

  /**
   * Pedestrians are collapsed. The footways F1 (P-A) and F2 (Q-B) move to the centre node, while the roads R1 (B-7)
   * and R2 (B-8), which allow cars and pedestrians, stay on B. A turn at B banned for pedestrians, from the footway A-B
   * onto R1, stops them going from F1 to R1, while they can go from F1 to R2 and from F2 to R1. After collapse,
   * pedestrians from F1 to R1 turn at the centre onto the new link to B, and at B onto R1: the first turn is shared
   * with F1 to R2, the second with F2 to R1. So no new turn is banned for it, and the ban is counted as not held
   * <pre>
   *     P            7   Q
   *       \          ^  /         F1 = P-A, F2 = Q-B and A-B are footways; R1 = B-7 and R2 = B-8 allow cars and
   *         A ~~~~~~ B ---> 8     pedestrians; the other roads are those of the divided roads crossing
   *         C        D
   * </pre>
   */
  @Test
  public void banThatNoNewTurnCanHoldIsCountedAsNotHeld() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var r1 = segment(B, 7);
    r1.setLinkSegmentType(carAndPedestrian);
    var r2 = layer.getLinkSegments().getFactory().registerNew(segment(8, B).getParent(), false, true);
    r2.setLinkSegmentType(carAndPedestrian);
    var f1 = segment(P, A);
    layer.getBannedTurns().getFactory().registerNew(segment(A, B), r1, List.of(pedestrian));

    var result = consolidateThenCollapse(List.of(pedestrian));

    var centreToB = result.getLinksAddedToCentreNode().stream().flatMap(link -> link.getEdgeSegments().stream())
        .map(MacroscopicLinkSegment.class::cast).filter(segment -> segment.getDownstreamVertex() == nodes.get(B))
        .findFirst().orElseThrow();
    for (var turn : List.of(List.of(f1, centreToB), List.of(centreToB, r1))) {
      assertTrue(layer.getBannedTurns().getBySegment(turn.get(0)).stream().noneMatch(
          ban -> ban.getSegmentFrom() == turn.get(0) && ban.getSegmentTo() == turn.get(1) &&
              ((MacroscopicBannedTurn) ban).isModeBanned(pedestrian)));
    }
    assertEquals(1, result.getCollapsedModeBansNotHeld().size());
    var notHeld = result.getCollapsedModeBansNotHeld().get(0);
    assertSame(f1, notHeld.first().getLinkSegmentFrom());
    assertSame(r1, notHeld.first().getLinkSegmentTo());
    assertSame(pedestrian, notHeld.second());
  }

  /**
   * Banned turns on internal segments lose the collapsed modes: once pedestrians are collapsed, the turn from A-C onto
   * C-D banned for cars and pedestrians is banned for cars only, and the turn from C-D onto D-B banned for pedestrians
   * only is removed
   */
  @Test
  public void bannedTurnsOnInternalSegmentsLoseTheCollapsedModes() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    var bans = layer.getBannedTurns().getFactory();
    var carAndPedestrianBan = bans.registerNew(segment(A, C), segment(C, D), List.of(car, pedestrian));
    var pedestrianBan = bans.registerNew(segment(C, D), segment(D, B), List.of(pedestrian));

    var result = consolidateThenCollapse(List.of(pedestrian));

    assertSame(carAndPedestrianBan, layer.getBannedTurns().get(carAndPedestrianBan.getId()));
    assertEquals(Set.of(car), carAndPedestrianBan.getExplicitlyBannedModes());
    assertTrue(result.getBannedTurnsThatLostCollapsedModes().stream().anyMatch(ban -> ban == carAndPedestrianBan));
    assertNull(layer.getBannedTurns().get(pedestrianBan.getId()));
    assertTrue(result.getRemovedBannedTurns().stream().anyMatch(ban -> ban == pedestrianBan));
  }

  /**
   * Link A-B is two-way. Cars may not turn from the northern approach 4-A onto the exit A-9, nor from A-C onto C-D, so
   * a car could reach A-9 only by driving along A-B and straight back, which no driver does inside a junction. Once
   * every mode is collapsed, the turn at the centre node from 4-A onto A-9 is banned for cars
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <--> B <----- 8        A-B two-way; 4-A onto A-9 and A-C onto C-D banned for cars
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void banGotRoundOnlyByGoingBackAlongAnInnerLinkIsCarriedToTheCentreNode() {
    dividedRoadsCrossing();
    layer.getLinkSegments().getFactory().registerNew(segment(B, A).getParent(), false, true).setLinkSegmentType(road);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    layer.getBannedTurns().getFactory().registerNew(segment(4, A), segment(A, 9), List.of(car));
    layer.getBannedTurns().getFactory().registerNew(segment(A, C), segment(C, D), List.of(car));
    var north = segment(4, A);
    var west = segment(A, 9);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(north, west, car));
  }

  /**
   * The northern approach 4-A and the exit B-7 allow cars and pedestrians, and pedestrians may also walk from A to B on
   * link A-B. D-B is a footway, so cars cannot get from A to B. Nothing is banned, yet only pedestrians can go from 4-A
   * to B-7. Once every mode is collapsed, the turn at the centre node from 4-A onto B-7 is banned for cars, and not for
   * pedestrians
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <~~> B <----- 8        4-A and B-7 cars and pedestrians; A to B and D-B footways
   *              |      ~
   *              v      ~
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void turnOnlyOtherModesCouldMakeIsBannedForTheCollapsedModes() {
    var carAndPedestrian = carAndPedestrianType();
    dividedRoadsCrossing();
    segment(4, A).setLinkSegmentType(carAndPedestrian);
    segment(B, 7).setLinkSegmentType(carAndPedestrian);
    segment(D, B).setLinkSegmentType(footway);
    layer.getLinkSegments().getFactory().registerNew(segment(B, A).getParent(), false, true)
        .setLinkSegmentType(footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var north = segment(4, A);
    var exitAtB = segment(B, 7);
    consolidateWithActiveModesInMovements();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(north, exitAtB, car));
    assertFalse(isTurnBanned(north, exitAtB, pedestrian));
  }

  /**
   * D-B is a footway, so cars arriving from the south at D can only go on east along D-11: no mode goes from the
   * southern approach 6-D to the exit B-7. Once every mode is collapsed, the turn at the centre node from 6-D onto B-7
   * is banned for cars, while the turn from the eastern approach 8-B onto B-7, which cars could make, is not
   */
  @Test
  public void turnNoModeCouldMakeIsBannedAtTheCentreNode() {
    dividedRoadsCrossing();
    segment(D, B).setLinkSegmentType(footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var south = segment(6, D);
    var east = segment(8, B);
    var exitAtB = segment(B, 7);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(car, pedestrian));

    assertTrue(isTurnBanned(south, exitAtB, car));
    assertFalse(isTurnBanned(east, exitAtB, car));
  }

  /**
   * Only the roads are approaches, as vehicle signals give them, so no movement starts from the footways P-A and Q-B.
   * While the intersection was spatial, pedestrians could still walk from P to Q, and from Q to P, over the footway
   * A-B. Once pedestrians are collapsed, the turns at the centre node from P onto Q and from Q onto P stay open
   */
  @Test
  public void entryThatIsNotAnApproachKeepsTheTurnsItCouldMake() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionWithRoadApproachesAt);
    var pIn = segment(P, A);
    var pOut = segment(A, P);
    var qIn = segment(Q, B);
    var qOut = segment(B, Q);
    consolidate();
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());

    collapse(List.of(onlySpatialIntersection()), List.of(pedestrian));

    assertFalse(isTurnBanned(pIn, qOut, pedestrian));
    assertFalse(isTurnBanned(qIn, pOut, pedestrian));
  }

  /**
   * A spatial intersection that is not registered on the layer is refused before anything changes, even when it comes
   * after a registered one: the registered intersection keeps its four member nodes, and no node or link is added or
   * removed
   */
  @Test
  public void intersectionNotOnTheLayerIsRefusedBeforeAnyChange() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    consolidate();
    var registered = onlySpatialIntersection();
    var notRegistered = intersections.getFactory().createSpatial(
        List.of(nodes.get(P), nodes.get(Q)), IntersectionControlType.SIGNALISED, List.of(IntersectionType.JUNCTION));
    var nodesBefore = layer.getNodes().size();
    var linksBefore = layer.getLinks().size();

    assertThrows(PlanItRunTimeException.class,
        () -> collapse(List.of(registered, notRegistered), List.of(pedestrian)));

    assertEquals(4, registered.getMemberNodes().size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());
  }

  /**
   * Collapse given no modes, or no intersections, changes nothing: the consolidated intersection stays, with its four
   * member nodes, no node or link is added or removed, and neither result lists a change
   */
  @Test
  public void noModesOrNoIntersectionsChangeNothing() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    consolidate();
    var consolidated = onlySpatialIntersection();
    var nodesBefore = layer.getNodes().size();
    var linksBefore = layer.getLinks().size();

    var noModes = collapse(List.of(consolidated), List.of());
    var noIntersections = collapse(List.of(), List.of(pedestrian));

    for (var result : List.of(noModes, noIntersections)) {
      assertTrue(result.getLinksMovedToCentreNode().isEmpty());
      assertTrue(result.getLinksAddedToCentreNode().isEmpty());
      assertTrue(result.getRemovedBannedTurns().isEmpty());
      assertTrue(result.getModeAccessCleanup().isEmpty());
    }
    assertSame(consolidated, onlySpatialIntersection());
    assertEquals(4, consolidated.getMemberNodes().size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());
  }

  /**
   * Corner A is excluded from collapse, so its intersection is not collapsed at all, though its other corners are not
   * excluded: no centre node is added, no link is moved, added or removed, and the four corners stay its member nodes.
   * The external id 404 is excluded too: no node has it, so it is listed as not found
   * <pre>
   *     P                Q
   *       \            /
   *         A ~~~~~~ B            A excluded from collapse
   *         C        D            roads as in dividedRoadsCrossing
   * </pre>
   */
  @Test
  public void intersectionWithANodeExcludedFromCollapseIsNotCollapsed() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    nodes.get(A).setExternalId("A");
    consolidate();
    var consolidated = onlySpatialIntersection();
    var nodesBefore = layer.getNodes().size();
    var linksBefore = layer.getLinks().size();

    var result = collapse(List.of(consolidated), List.of(pedestrian), IdMapperType.EXTERNAL_ID, "A", "404");

    assertSame(consolidated, onlySpatialIntersection());
    assertEquals(4, consolidated.getMemberNodes().size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());
    assertTrue(result.getLinksMovedToCentreNode().isEmpty());
    assertTrue(result.getLinksAddedToCentreNode().isEmpty());
    assertEquals(List.of("404"), result.getExcludedNodeIdsNotFound());
  }

  /**
   * The nodes to exclude from collapse may be given by any kind of id. Corner A given by its PLANit id keeps its
   * intersection from collapsing, as when given by its external id, see
   * {@link #intersectionWithANodeExcludedFromCollapseIsNotCollapsed()}
   * <pre>
   *     P                Q
   *       \            /
   *         A ~~~~~~ B            A excluded from collapse by its id
   *         C        D            roads as in dividedRoadsCrossing
   * </pre>
   */
  @Test
  public void nodeExcludedFromCollapseByItsIdKeepsItsIntersectionFromCollapsing() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    consolidate();
    var consolidated = onlySpatialIntersection();
    var idOfA = String.valueOf(nodes.get(A).getId());

    var result = collapse(List.of(consolidated), List.of(pedestrian), IdMapperType.ID, idOfA);

    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());
    assertTrue(result.getLinksMovedToCentreNode().isEmpty());
  }

  /**
   * The footway end P is excluded from collapse, but it is a member node of no intersection given to collapse, so the
   * intersection collapses as it would otherwise: both footways move to the centre node, see
   * {@link #footwaysCollapsedRoadsKept()}
   * <pre>
   *     P                Q        P excluded from collapse
   *       \            /
   *         A ~~~~~~ B
   *         C        D            roads as in dividedRoadsCrossing
   * </pre>
   */
  @Test
  public void nodeExcludedFromCollapseInNoIntersectionChangesNothing() {
    dividedRoadsCrossingWithFootways(this::signalisedJunctionAt);
    nodes.get(P).setExternalId("P");
    consolidate();
    var consolidated = onlySpatialIntersection();

    var result = collapse(List.of(consolidated), List.of(pedestrian), IdMapperType.EXTERNAL_ID, "P");

    assertEquals(5, onlySpatialIntersection().getMemberNodes().size());
    assertEquals(2, result.getLinksMovedToCentreNode().size());
  }
}
