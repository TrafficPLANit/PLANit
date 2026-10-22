package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.MADE_INTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests which parsed intersections consolidation joins into one: the intersection clusters within reach along the road,
 * how they are subdivided, and what the replacing spatial intersection takes over from them
 *
 * @author markr
 */
public class IntersectionClusterTest extends IntersectionConsolidationTestBase {

  /** collects the intersection removal events */
  private static class RemovalListener implements IntersectionModifierListener {
    final List<IntersectionModificationEvent> received = new ArrayList<>();
    @Override public void onIntersectionModifierEvent(IntersectionModificationEvent event) { received.add(event); }
    @Override public EventType[] getKnownSupportedEventTypes() {
      return new EventType[] {RemoveIntersectionEvent.EVENT_TYPE};
    }
  }

  /** consolidate with the default values except the least gain per member and the tie margin */
  private IntersectionConsolidationModifierResult consolidate(double minimumCoverageGainPerMember,
      double coverageTieMargin) {
    return consolidateWith(settings -> {
      settings.setMinimumCoverageGainPerMember(minimumCoverageGainPerMember);
      settings.setCoverageTieMargin(coverageTieMargin);
    });
  }

  /**
   * A junction with no other junction within the joining reach along the road is kept as it is
   */
  @Test
  public void junctionOnItsOwnIsKeptAsItIs() {
    /*
     *            2
     *            |
     *   1 ------ 0 ------ 3      all two-way, 85 m
     *            |
     *            4
     */
    int junctionNode = node(0, 0);
    node(-85, 0); node(0, 85); node(85, 0); node(0, -85);
    for (int arm = 1; arm <= 4; ++arm) {
      twoWayRoad(junctionNode, arm, 85);
    }
    var junction = signalisedJunctionAt(junctionNode);

    var result = consolidate();

    assertSame(junction, onlyNonSpatialIntersection());
    assertEquals(1, junction.getMemberNodes().size());
    assertEquals(4, junction.getApproachSegments().size());
    assertTrue(result.getReplacedIntersections().isEmpty());
  }

  /**
   * Each node of a divided roads crossing on its own reaches about half of the directions, the four together all of
   * them, so they become one intersection: a new spatial intersection replaces all four, each removed with its removal
   * event and listed as replaced by the new one. The four segments between them become internal; with no vehicle
   * signal standing on them they are plain. Nothing is added to or removed from the graph. The consolidated
   * intersection keeps the external ids of the intersections it replaces, each once, so it can be traced back to its
   * source
   */
  @Test
  public void dividedRoadsCrossingBecomesOneIntersection() {
    dividedRoadsCrossing();
    var junctions = List.of(signalisedJunctionAt(A), signalisedJunctionAt(B), signalisedJunctionAt(C),
        signalisedJunctionAt(D));
    junctions.get(A).setExternalId("1,2");
    junctions.get(B).setExternalId("2,3");
    junctions.get(D).setExternalId("4");
    var removals = new RemovalListener();
    layer.getLayerModifier().addListener(removals);
    long nodesBefore = layer.getNodes().size();
    long linksBefore = layer.getLinks().size();

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    junctions.forEach(junction -> assertNotSame(junction, consolidated));
    assertEquals(4, removals.received.size());
    assertEquals(Set.of(nodes.get(A), nodes.get(B), nodes.get(C), nodes.get(D)),
        Set.copyOf(consolidated.getMemberNodes()));
    assertEquals(Set.of(segment(4, A), segment(8, B), segment(6, D), segment(10, C)),
        Set.copyOf(consolidated.getApproachSegments()));
    assertEquals(Set.of(segment(A, C), segment(B, A), segment(D, B), segment(C, D)),
        Set.copyOf(consolidated.getInternalSegments()));
    assertTrue(consolidated.getInternalSegmentsWithSignal().isEmpty());
    assertEquals(4, result.getApproaches(MADE_INTERNAL).size());
    assertEquals(nodesBefore, layer.getNodes().size());
    assertEquals(linksBefore, layer.getLinks().size());

    var replaced = result.getReplacedIntersections();
    assertEquals(4, replaced.size());
    for (var junction : junctions) {
      assertTrue(replaced.stream().anyMatch(pair -> pair.first() == junction && pair.second() == consolidated));
    }

    assertEquals(List.of("1", "2", "3", "4"), List.of(onlyIntersection().getSplitExternalId()));
  }

  /**
   * A stop line between the carriageways: a vehicle signal stands on the westbound segment from B to A, an approach of
   * A. Once B is a member, that segment becomes an internal segment with a signal
   */
  @Test
  public void signalBetweenTheCarriagewaysMakesAnInternalSegmentWithASignal() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var stopLine = segment(B, A);

    var result = consolidate(segment -> segment == stopLine);

    var consolidated = onlySpatialIntersection();
    assertEquals(List.of(stopLine), consolidated.getInternalSegmentsWithSignal());
    assertFalse(consolidated.getApproachSegments().contains(stopLine));
    assertTrue(result.getApproaches(MADE_INTERNAL).contains(stopLine));
  }

  /**
   * D has no signal, but is joined by 15 m links to two of the three signalised nodes, so it becomes a member of their
   * consolidated intersection
   */
  @Test
  public void nodeWithoutASignalJoinsBetweenSignalisedNodes() {
    dividedRoadsCrossing();
    List.of(A, B, C).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    assertTrue(onlyIntersection().hasMemberNode(nodes.get(D)));
    assertEquals(List.of(nodes.get(D)), result.getJoinedNodes());
  }

  /**
   * F is a fork before the divided roads crossing: the road from the north splits there into one-way branches into A
   * and B, each with a vehicle signal. F has short links to two signalised nodes and lies within the radius, but road
   * traffic only reaches it from outside, so it stays outside. The branches stay approaches with their signals, and no
   * approach without a signal is added. A footway runs from F through G1 and G2 and on, away from the crossing. G1 and
   * G2 lie within the radius and connect A and B, but only through F: with F outside, they would hang off the
   * intersection on their own, so they stay outside too
   * <pre>
   *                 N
   *                 |          N-F 8 m, one-way into F
   *                 v
   *         G2 ~~~~ G1 ~~~ F   F 2 m above the carriageways; footway F-G1-G2-H, 1.5, 2.5 and 23 m, H further north
   *               /   \
   *              v     v       F-A and F-B 8 m, one-way, a vehicle signal on each
   *     9 <----- A <--- B <----- 8
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11      the divided roads crossing, its other arms as drawn there
   * </pre>
   */
  @Test
  public void forkBeforeTheJunctionAndTheFootwaysBeyondItStayOutside() {
    dividedRoadsCrossing();
    int f = node(7.5, 17);
    oneWayRoad(node(7.5, 25), f, 8);
    oneWayRoad(f, A, 8);
    oneWayRoad(f, B, 8);
    int g1 = node(6, 17.2);
    int g2 = node(3.5, 17);
    link(f, g1, 1.5, true, footway);
    link(g1, g2, 2.5, true, footway);
    link(g2, node(3.5, 40), 23, true, footway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var branches = Set.of(segment(f, A), segment(f, B));

    var result = consolidate(branches::contains);

    var consolidated = onlySpatialIntersection();
    assertTrue(List.of(f, g1, g2).stream().noneMatch(index -> consolidated.hasMemberNode(nodes.get(index))));
    assertTrue(result.getJoinedNodes().isEmpty());
    assertTrue(consolidated.getApproachSegments().containsAll(branches));
    assertTrue(branches.stream().allMatch(consolidated::hasOwnSignal));
    assertTrue(consolidated.getInternalSegmentsWithSignal().isEmpty());
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE).isEmpty());
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }

  /**
   * S is where a two-way road from the north splits into the carriageways of the divided roads crossing: the inbound
   * one into A with a vehicle signal on it, the outbound one from B. Outbound traffic reaches S from B, but traffic from
   * outside enters S before the signal, so S lies in front of the stop line and stays outside: the inbound carriageway
   * stays an approach with its signal, the outbound one an exit, and no approach without a signal is added
   * <pre>
   *                 N
   *                 |          N-S 85 m, two-way
   *                 S          32 m above the carriageways
   *               /   ^
   *              v     \       S-A and B-S 33 m, one-way, a vehicle signal on S-A
   *     9 <----- A <--- B <----- 8
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11      the divided roads crossing, its other arms as drawn there
   * </pre>
   */
  @Test
  public void roadSplittingIntoCarriagewaysBeforeTheJunctionStaysOutside() {
    dividedRoadsCrossing();
    int s = node(7.5, 47);
    twoWayRoad(node(7.5, 132), s, 85);
    oneWayRoad(s, A, 33);
    oneWayRoad(B, s, 33);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    var inbound = segment(s, A);

    var result = consolidate(segment -> segment == inbound);

    var consolidated = onlySpatialIntersection();
    assertFalse(consolidated.hasMemberNode(nodes.get(s)));
    assertTrue(result.getJoinedNodes().isEmpty());
    assertTrue(consolidated.getApproachSegments().contains(inbound));
    assertTrue(consolidated.hasOwnSignal(inbound));
    assertFalse(consolidated.getInternalSegments().contains(segment(B, s)));
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }

  /**
   * G is a merge after the divided roads crossing: one-way roads from C and D meet there and leave to the south. G has
   * short links to two signalised nodes, and road traffic reaches it from them, so it joins, and the roads into it
   * become internal
   * <pre>
   *              A <--- B
   *              |      ^
   *              v      |
   *    10 -----> C ---> D -----> 11      the divided roads crossing, its other arms as drawn there
   *               \    /
   *                v  v        C-G and D-G 17 m, one-way
   *                 G          15 m below the carriageways
   *                 |          G-S 85 m, one-way out of G
   *                 v
   *                 S
   * </pre>
   */
  @Test
  public void mergeAfterTheJunctionJoins() {
    dividedRoadsCrossing();
    int g = node(7.5, -15);
    oneWayRoad(C, g, 17);
    oneWayRoad(D, g, 17);
    oneWayRoad(g, node(7.5, -100), 85);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(g)));
    assertEquals(List.of(nodes.get(g)), result.getJoinedNodes());
    assertTrue(consolidated.getInternalSegments().contains(segment(C, g)));
    assertTrue(consolidated.getInternalSegments().contains(segment(D, g)));
  }

  /**
   * A tram track runs beside the carriageway from A to C through T, a node with only the two tram links. No road
   * reaches T, but both its links end at member nodes, so T joins, and the tram links on either side of it become
   * internal instead of arms. A second tram track runs from T to C through Y. Y lies beyond the radius of the crossing at
   * first, but once T has joined, the radius grows and Y lies within it, connecting T and C, so Y joins too: the nodes
   * an intersection encloses are looked for again until none join
   * <pre>
   *              4      7
   *              |      ^
   *              v      |
   *     9 <----- A <--- B <----- 8
   *            # |      ^
   *           T  |      |        # tram track A-T-C, 8 m each way, two-way
   *          # # |      |
   *         Y  # v      |        tram track T-Y-C, 5.6 and 3.6 m, two-way, Y 3 m west of and 2 m above C
   *          # # |      |
   *    10 -----> C ---> D -----> 11
   *              |      ^
   *              v      |
   *              5      6
   * </pre>
   */
  @Test
  public void nodeOnATramTrackBetweenMemberNodesJoins() {
    dividedRoadsCrossing();
    var tramway = tramway();
    int t = node(-2, 7.5);
    link(A, t, 8, true, tramway);
    link(t, C, 8, true, tramway);
    int y = node(-3, 2);
    link(t, y, 5.6, true, tramway);
    link(y, C, 3.6, true, tramway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionWithRoadApproachesAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(t)));
    assertTrue(consolidated.hasMemberNode(nodes.get(y)));
    assertEquals(List.of(nodes.get(t), nodes.get(y)), result.getJoinedNodes());
    assertTrue(consolidated.getInternalSegments().containsAll(List.of(segment(t, y), segment(y, C))));
    assertTrue(consolidated.getInternalSegments().containsAll(
        List.of(segment(A, t), segment(t, A), segment(t, C), segment(C, t))));
  }

  /**
   * The tram track beside the carriageway from A to C passes two nodes, T and U, each with only tram links, and a
   * siding leaves U for V, beyond the radius of the crossing. Neither T nor U has all its links ending at member nodes.
   * But both lie within the radius and connect A and C without leaving it, so both join: the tram links between A and C
   * become internal, and the siding stays outside as an arm
   * <pre>
   *     9 <----- A <--- B <----- 8
   *            # |      ^
   *           T  |      |        # tram track A-T-U-C, 5 m each link, two-way
   *           #  |      |
   *   V # # # U  |      |        siding U-V, 23 m, two-way
   *            # v      |
   *    10 -----> C ---> D -----> 11      the divided roads crossing, its other arms as drawn there
   * </pre>
   */
  @Test
  public void nodesOnATramTrackBetweenMemberNodesJoinTogether() {
    dividedRoadsCrossing();
    var tramway = tramway();
    int t = node(-2, 10);
    int u = node(-2, 5);
    int v = node(-25, 5);
    link(A, t, 5, true, tramway);
    link(t, u, 5, true, tramway);
    link(u, C, 5, true, tramway);
    link(u, v, 23, true, tramway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionWithRoadApproachesAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(t)));
    assertTrue(consolidated.hasMemberNode(nodes.get(u)));
    assertFalse(consolidated.hasMemberNode(nodes.get(v)));
    assertEquals(List.of(nodes.get(t), nodes.get(u)), result.getJoinedNodes());
    assertTrue(consolidated.getInternalSegments().containsAll(List.of(segment(A, t), segment(t, u), segment(u, C))));
    assertFalse(consolidated.getInternalSegments().contains(segment(u, v)));
  }

  /**
   * A loop road leaves A and comes back at B through P and Q, each link 30 m. All their links end at each other or at
   * member nodes, but P and Q lie beyond the radius of the crossing, so they stay outside. Footway loops are different:
   * a node only footways use joins when it lies on a footway connection of at most 50 m between two member nodes,
   * however far from the centre. So R and S, 8 m above the carriageways, join over 31 m from A to B, and V and W, 12 m
   * below them, over 39 m from C to D. V2 and W2 further down stay outside: from V to W through them is 51 m. H, on a
   * pavement leading west from R, stays outside too, though from A out to H and back to B is 41 m: it links to the
   * intersection through R only, so it would hang off it. Nor do the footway nodes widen the radius, which only counts
   * the member nodes roads and rails use. So Y, on a tram track from A to C just beyond the radius, stays outside: with
   * R and S counted, it would lie within it
   * <pre>
   *              P -------> Q        P and Q 35 m above the carriageways
   *              ^          |
   *              |          v        loop road A-P-Q-B, 30 m each link, one-way as drawn
   *   ~~~~~ H ~~ R ~~~~~~~~ S        R and S 8 m above; footway A-R-S-B, 8, 15 and 8 m; pavement R-H 5 m, on 55 m
   *              ~          ~
   *     9 <----- A <------- B <----- 8
   *           #  |          ^
   *          Y   |          |        # tram track A-Y-C, 8.3 m each link, Y 3.5 m west of the carriageway
   *           #  v          |
   *    10 -----> C -------> D -----> 11      the divided roads crossing, its other arms as drawn there
   *              ~          ~
   *              V ~~~~~~~~ W        V and W 12 m below; footway C-V-W-D, 12, 15 and 12 m
   *              ~          ~
   *              V2 ~~~~~~ W2        V2 and W2 30 m below; footway V-V2-W2-W, 18, 15 and 18 m
   * </pre>
   * Footways and the tram track are two-way
   */
  @Test
  public void shortFootwayLoopsJoinWhileOtherNodesBeyondTheRadiusStayOutside() {
    dividedRoadsCrossing();
    int p = node(0, 45);
    int q = node(15, 45);
    oneWayRoad(A, p, 30);
    oneWayRoad(p, q, 30);
    oneWayRoad(q, B, 30);
    int r = node(0, 23);
    int s = node(15, 23);
    link(A, r, 8, true, footway);
    link(r, s, 15, true, footway);
    link(s, B, 8, true, footway);
    int h = node(-5, 23);
    link(r, h, 5, true, footway);
    link(h, node(-60, 23), 55, true, footway);
    int v = node(0, -12);
    int w = node(15, -12);
    link(C, v, 12, true, footway);
    link(v, w, 15, true, footway);
    link(w, D, 12, true, footway);
    int v2 = node(0, -30);
    int w2 = node(15, -30);
    link(v, v2, 18, true, footway);
    link(v2, w2, 15, true, footway);
    link(w2, w, 18, true, footway);
    var tramway = tramway();
    int y = node(-3.5, 7.5);
    link(A, y, 8.3, true, tramway);
    link(y, C, 8.3, true, tramway);
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertEquals(List.of(nodes.get(r), nodes.get(s), nodes.get(v), nodes.get(w)), result.getJoinedNodes());
    assertTrue(List.of(p, q, h, v2, w2, y).stream().noneMatch(index -> consolidated.hasMemberNode(nodes.get(index))));
  }

  /**
   * P and Q are 20 m apart. Together they would score higher than apart, but Q does not earn its place: its own traffic
   * from the north east already reaches the west through the link to P, and no traffic enters P from outside, so P is
   * not helped by it. P earns its place as an exit-only member. Both stay as they are
   * <pre>
   *                N   NE
   *                ^  /
   *                | v
   *   W <--- P <-> Q        P-Q 20 m two-way; arms 85 m, one-way as drawn
   * </pre>
   */
  @Test
  public void memberThatDoesNotEarnItsPlaceKeepsTheJunctionsApart() {
    int p = node(0, 0);
    int q = node(20, 0);
    twoWayRoad(p, q, 20);
    oneWayRoad(p, node(-85, 0), 85);
    oneWayRoad(q, node(20, 85), 85);
    oneWayRoad(node(80.1, 60.1), q, 85);
    signalisedJunctionAt(p);
    signalisedJunctionAt(q);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getReplacedIntersections().isEmpty());
  }

  /**
   * P and Q are 32 m apart, and each one's only approach with a signal comes from the other, as when the vehicle
   * signals either side of a crossing between them each pass it to the junction beyond. Together they would score
   * higher than apart, but both approaches would become internal, leaving the group without an approach with a signal.
   * So they stay apart, each keeping its approach
   * <pre>
   *              N
   *              |
   *   E1 ------> P             P-Q 32 m two-way, with P's approach Q->P and Q's approach P->Q, each with a signal;
   *              |             P-N and Q-S two-way, E1-P one-way into P, Q-E2 one-way out of Q, arms 85 m
   *              Q ------> E2
   *              |
   *              S
   * </pre>
   */
  @Test
  public void groupThatWouldLoseEveryApproachWithASignalStaysApart() {
    int p = node(0, 32);
    int q = node(0, 0);
    var between = twoWayRoad(p, q, 32);
    twoWayRoad(p, node(0, 117), 85);
    oneWayRoad(node(85, 32), p, 85);
    twoWayRoad(q, node(0, -85), 85);
    oneWayRoad(q, node(85, 0), 85);
    var junctionP = signalisedJunctionApproachedOnlyFrom(p, q);
    var junctionQ = signalisedJunctionApproachedOnlyFrom(q, p);
    var stopLines = Set.of(between.getLinkSegmentAb(), between.getLinkSegmentBa());

    var result = consolidate(stopLines::contains);

    assertTrue(result.getReplacedIntersections().isEmpty());
    assertEquals(2, intersections.size());
    assertTrue(junctionP.hasOwnSignal(segment(q, p)));
    assertTrue(junctionQ.hasOwnSignal(segment(p, q)));
  }

  /**
   * J1 and J2 lie 39 m apart along the road, which passes S, where a side road comes in without a signal, and then F,
   * after which J2's stop line stands. Traffic from the side road meets that stop line before it enters J2, so S and
   * then F lie in front of J2 and stay out of the cluster. The two junctions are then not connected through the
   * cluster's nodes, so they stay apart, each keeping its stop line as an approach with a signal. Nor is J2 pulled in
   * to salvage the road from S into J1, since that road would not become internal
   * <pre>
   *                                N
   *                                |
   *   W2 <--> J2 <-[s]- F <------> S <------> J1 <-[s]- E1     J2-F 14 m with J2's stop line [s], F-S 13 m,
   *           |                                |               S-J1 12 m, all two-way; side road S-N 85 m, two-way;
   *           S2                               N1              J1's stop line on E1-J1; other arms 85 m, two-way
   * </pre>
   */
  @Test
  public void junctionsWhoseRoadBetweenPassesASideRoadBeforeAStopLineStayApart() {
    int j2 = node(0, 0);
    int f = node(14, 0);
    int s = node(27, 0);
    int j1 = node(39, 0);
    var stopLineOfJ2 = twoWayRoad(f, j2, 14);
    twoWayRoad(s, f, 13);
    twoWayRoad(j1, s, 12);
    twoWayRoad(s, node(27, 85), 85);
    twoWayRoad(j2, node(-85, 0), 85);
    twoWayRoad(j2, node(0, -85), 85);
    int e1 = node(124, 0);
    var stopLineOfJ1 = twoWayRoad(e1, j1, 85);
    twoWayRoad(j1, node(39, 85), 85);
    var junctionJ2 = signalisedJunctionApproachedOnlyFrom(j2, f);
    var junctionJ1 = signalisedJunctionApproachedOnlyFrom(j1, e1);
    var stopLines = Set.of(stopLineOfJ2.getLinkSegmentAb(), stopLineOfJ1.getLinkSegmentAb());

    var result = consolidate(stopLines::contains);

    assertTrue(result.getReplacedIntersections().isEmpty());
    assertEquals(2, intersections.size());
    assertTrue(junctionJ2.hasOwnSignal(segment(f, j2)));
    assertTrue(junctionJ1.hasOwnSignal(segment(e1, j1)));
  }

  /**
   * Apart, P and Q score about 0.05 more than together. With a tie margin of 0.001 they stay apart; with a
   * margin of 0.1 the two count as a tie, which goes to fewer cluster subsets
   * <pre>
   *     N                         P-Q 20 m two-way, arms 85 m:
   *     |                         P-N two-way, the others one-way away from P and Q
   *     P <-> Q
   *     |    /|
   *     v   v v
   *     S  SW S
   * </pre>
   */
  @Test
  public void tieGoesToFewerClusterSubsets() {
    int p = node(0, 0);
    int q = node(20, 0);
    twoWayRoad(p, q, 20);
    twoWayRoad(p, node(0, 85), 85);
    oneWayRoad(p, node(0, -85), 85);
    oneWayRoad(q, node(20, -85), 85);
    oneWayRoad(q, node(-40.1, -60.1), 85);
    signalisedJunctionAt(p);
    signalisedJunctionAt(q);

    consolidate(IntersectionConsolidatorSettings.DEFAULT_MINIMUM_COVERAGE_GAIN_PER_MEMBER, 0.001);
    assertEquals(2, intersections.size());

    var result = consolidate(IntersectionConsolidatorSettings.DEFAULT_MINIMUM_COVERAGE_GAIN_PER_MEMBER, 0.1);
    assertEquals(1, intersections.size());
    assertEquals(2, result.getReplacedIntersections().size());
  }

  /**
   * Eleven junctions 20 m apart along a road form one intersection cluster, more than the ten for which every
   * subdivision is tried. It is subdivided step by step and listed, and each junction ends up in exactly one
   * intersection
   */
  @Test
  public void intersectionClusterTooLargeToTryEverySubdivisionIsSubdividedStepByStep() {
    for (int index = 0; index < 11; ++index) {
      node(20 * index, 0);
      if (index > 0) {
        twoWayRoad(index - 1, index, 20);
      }
    }
    for (int index = 0; index < 11; ++index) {
      signalisedJunctionAt(index);
    }

    var result = consolidate();

    assertEquals(1, result.getIntersectionClustersSubdividedStepByStep().size());
    assertEquals(11, result.getIntersectionClustersSubdividedStepByStep().get(0).size());
    for (int index = 0; index < 11; ++index) {
      int node = index;
      assertEquals(1, intersections.stream().filter(i -> i.hasMemberNode(nodes.get(node))).count());
    }
  }

  /**
   * The divided roads crossing becomes one intersection when every subdivision is tried, see
   * {@link ConsolidatedIntersectionTest}. With every subdivision tried only up to three parsed intersections, its four
   * are subdivided step by step instead, which finds the same one intersection
   */
  @Test
  public void stepByStepSearchFindsWhatTryingEverySubdivisionFinds() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);

    var result = consolidateWith(settings -> settings.setMaximumParsedIntersectionsToTryEverySubdivision(3));

    assertEquals(1, result.getIntersectionClustersSubdividedStepByStep().size());
    assertEquals(4, onlySpatialIntersection().getMemberNodes().size());
  }

  /**
   * As in {@link #tieGoesToFewerClusterSubsets}, P and Q score more apart than together, by more than a tie margin of
   * 0.001. Searched step by step from the cluster as one cluster subset, with every subdivision tried only for a single
   * parsed intersection, the search still finds them apart
   */
  @Test
  public void stepByStepSearchFindsASplitWhenThatScoresBest() {
    int p = node(0, 0);
    int q = node(20, 0);
    twoWayRoad(p, q, 20);
    twoWayRoad(p, node(0, 85), 85);
    oneWayRoad(p, node(0, -85), 85);
    oneWayRoad(q, node(20, -85), 85);
    oneWayRoad(q, node(-40.1, -60.1), 85);
    signalisedJunctionAt(p);
    signalisedJunctionAt(q);

    var result = consolidateWith(settings -> {
      settings.setCoverageTieMargin(0.001);
      settings.setMaximumParsedIntersectionsToTryEverySubdivision(1);
    });

    assertEquals(1, result.getIntersectionClustersSubdividedStepByStep().size());
    assertEquals(2, intersections.size());
  }
}
