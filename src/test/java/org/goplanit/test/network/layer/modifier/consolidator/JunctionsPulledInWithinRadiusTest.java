package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome.ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL;
import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.JUNCTION_PULLED_IN_WITHIN_RADIUS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pulling junctions into a consolidated intersection whose radius, plus a margin, they lie within: the divided
 * roads crossing becomes one intersection, its centre at (7.5, 7.5) and its radius 10.6 m, the distance to each corner.
 * A signalised junction J below it is joined to corner C by a footway only, so no road brings it into the cluster
 * <pre>
 *         A <--- B
 *         |      ^
 *         v      |
 *         C ---> D          the divided roads crossing, corners 15 m apart
 *          \
 *           ~               ~ footway from C to J
 *            \
 *     W <---> J <---> E     J's own roads, 85 m
 * </pre>
 *
 * @author markr
 */
public class JunctionsPulledInWithinRadiusTest extends IntersectionConsolidationTestBase {

  /** radius of the divided roads crossing once consolidated: from its centre to a corner */
  private static final double RADIUS = Math.hypot(7.5, 7.5);

  /**
   * The divided roads crossing, its corners signalised, and a signalised junction J with roads west and east, at a
   * distance from the crossing's centre and joined to C by a footway of the given length
   *
   * @param distanceFromCentre of J, straight down from the centre
   * @param footwayMeters length of the footway from C to J
   * @return index of J
   */
  private int crossingWithJunctionBelow(double distanceFromCentre, double footwayMeters) {
    dividedRoadsCrossing();
    int j = node(7.5, 7.5 - distanceFromCentre);
    twoWayRoad(j, node(-77.5, 7.5 - distanceFromCentre), 85);
    twoWayRoad(j, node(92.5, 7.5 - distanceFromCentre), 85);
    link(C, j, footwayMeters, true, footway);
    List.of(A, B, C, D, j).forEach(this::signalisedJunctionAt);
    return j;
  }

  /**
   * The divided roads crossing, its corners signalised, and a signalised junction J below it, beyond the radius plus
   * the margin. A one-way road from J into C was not an approach of C when its signals were integrated, so it is a
   * road entering the crossing without a signal
   * <pre>
   *         A <--- B
   *         |      ^
   *         v      |
   *         C ---> D          the divided roads crossing
   *         ^
   *         |                 road J-C, one-way into C, 44 m
   *         |
   *  W <--> J <--> E          J lies the radius plus the margin plus 10 m below the centre, its own roads 85 m
   * </pre>
   *
   * @return the index of J
   */
  private int crossingWithRoadWithoutSignalFromJunctionBelow() {
    dividedRoadsCrossing();
    List.of(A, B, C, D).forEach(this::signalisedJunctionAt);
    int j = node(7.5, 7.5 - BEYOND_THE_MARGIN);
    oneWayRoad(j, C, 44);
    twoWayRoad(j, node(-77.5, 7.5 - BEYOND_THE_MARGIN), 85);
    twoWayRoad(j, node(92.5, 7.5 - BEYOND_THE_MARGIN), 85);
    return j;
  }

  /**
   * Two signalised junctions, J1 with its own roads west and east, and J2 the given distance below it with roads of its
   * own, joined by a footway only
   *
   * @param metersApart from J1 down to J2
   * @param footwayMeters length of the footway from J1 to J2
   * @return indices of J1 and J2
   */
  private int[] loneJunctionsLinkedByAFootway(double metersApart, double footwayMeters) {
    int j1 = node(0, 0);
    twoWayRoad(j1, node(-85, 0), 85);
    twoWayRoad(j1, node(85, 0), 85);
    int j2 = node(0, -metersApart);
    twoWayRoad(j2, node(-85, -metersApart), 85);
    twoWayRoad(j2, node(85, -metersApart), 85);
    link(j1, j2, footwayMeters, true, footway);
    signalisedJunctionAt(j1);
    signalisedJunctionAt(j2);
    return new int[] {j1, j2};
  }

  /**
   * J lies within the crossing's radius plus the margin and 30 m from C along the footway, so it is pulled in: one
   * intersection is left, with J a member node, and J's intersection is listed
   */
  @Test
  public void junctionWithinRadiusIsPulledIn() {
    double distance = RADIUS + IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS - 1;
    int j = crossingWithJunctionBelow(distance, 30);

    var result = consolidate();

    assertEquals(1, intersections.size());
    assertTrue(onlySpatialIntersection().hasMemberNode(nodes.get(j)));
    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
  }

  /**
   * The road from C to J crosses a tram track at X, so J may lie 15 m further beyond the radius than the margin, about
   * the width of a tram corridor. J lies beyond the radius plus the margin, but within that and 15 m more, 45 m from C
   * along the road, so it is pulled in
   * <pre>
   *       A <--- B
   *       |      ^
   *       v      |
   *       C ---> D
   *       |
   *   # # X # #             # tram track crossing the road at X; C-X 20 m, X-J 25 m, two-way
   *       |
   *   W <---> J <---> E     J lies the radius plus 1.5 times the margin below the centre
   * </pre>
   */
  @Test
  public void junctionWhoseConnectionCrossesATramTrackIsPulledInFurther() {
    double distance = RADIUS + 1.5 * IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS;
    dividedRoadsCrossing();
    int j = node(7.5, 7.5 - distance);
    int x = node(3, -20);
    twoWayRoad(C, x, 20);
    twoWayRoad(x, j, 25);
    twoWayRoad(j, node(-77.5, 7.5 - distance), 85);
    twoWayRoad(j, node(92.5, 7.5 - distance), 85);
    var tramway = tramway();
    link(x, node(-82, -20), 85, true, tramway);
    link(x, node(88, -20), 85, true, tramway);
    List.of(A, B, C, D, j).forEach(this::signalisedJunctionAt);

    var result = consolidate();

    assertEquals(1, intersections.size());
    assertTrue(onlySpatialIntersection().hasMemberNode(nodes.get(j)));
    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
  }

  /**
   * A tram track meets the road at corner A, but the connection from C to J is a footway that crosses no track, so J may lie
   * only the margin beyond the radius. It lies further, so it stays apart
   * <pre>
   *     #
   *      #
   *       A <--- B          # tram track from A, 85 m
   *       |      ^
   *       v      |
   *       C ---> D
   *        \
   *         ~               ~ footway from C to J, 45 m
   *          \
   *   W <---> J <---> E     J lies the radius plus 1.5 times the margin below the centre
   * </pre>
   */
  @Test
  public void junctionWhoseConnectionCrossesNoTramTrackKeepsTheMargin() {
    double distance = RADIUS + 1.5 * IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS;
    crossingWithJunctionBelow(distance, 45);
    link(A, node(-60, 75), 85, true, tramway());

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /** J lies just beyond the crossing's radius plus the margin, so it stays apart */
  @Test
  public void junctionBeyondTheMarginStaysApart() {
    double distance = RADIUS + IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS + 1;
    crossingWithJunctionBelow(distance, 40);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /**
   * J lies within the crossing's radius plus the margin, but the footway from C is longer than the longest connection
   * a junction may be pulled in over, so it stays apart
   */
  @Test
  public void junctionWithinRadiusBeyondTheConnectionLengthStaysApart() {
    double distance = RADIUS + IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS - 1;
    crossingWithJunctionBelow(
        distance, IntersectionConsolidatorSettings.DEFAULT_MAXIMUM_PULL_IN_CONNECTION_LENGTH_METERS + 5);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /**
   * J lies within the crossing's radius plus the margin, and its only connection to the crossing runs through F, a fork
   * before both: the road from the north splits there into one-way branches into B and into J, each with a vehicle
   * signal. J is pulled in, but F lies in front of the junction, so it stays outside: both branches stay approaches
   * with their signals, and the road into F is no approach without a signal
   * <pre>
   *                       N
   *                       |          N-F 85 m, one-way into F
   *                       F          (25, 45)
   *                     /   \
   *                    v     v       F-B 32 m and F-J 22 m, one-way, a vehicle signal on each
   *         A <--- B          J <--> E      J (35, 25), a signalised junction with its own road east, 85 m
   *         |      ^
   *         v      |
   *         C ---> D          the divided roads crossing, its other arms as drawn there
   * </pre>
   */
  @Test
  public void forkOnTheConnectionOfAJunctionPulledInStaysOutside() {
    dividedRoadsCrossing();
    int f = node(25, 45);
    int j = node(35, 25);
    oneWayRoad(node(25, 130), f, 85);
    oneWayRoad(f, B, 32);
    oneWayRoad(f, j, 22);
    twoWayRoad(j, node(120, 25), 85);
    List.of(A, B, C, D, j).forEach(this::signalisedJunctionAt);
    var branches = Set.of(segment(f, B), segment(f, j));

    var result = consolidate(branches::contains);

    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(j)));
    assertFalse(consolidated.hasMemberNode(nodes.get(f)));
    assertTrue(consolidated.getApproachSegments().containsAll(branches));
    assertTrue(branches.stream().allMatch(consolidated::hasOwnSignal));
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }

  /** distance below the centre of the crossing at which J lies, beyond the radius plus the margin by 10 m */
  private static final double BEYOND_THE_MARGIN = RADIUS + IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS + 10;

  /**
   * The road from J into C would become an approach without a signal. Its road leads back to J, which lies beyond the
   * radius plus the margin, but within the further 15 m allowed to salvage such an approach. So J is pulled in after
   * all: the road becomes internal, no approach without a signal is added, and J is listed as pulled in to salvage it.
   * A tram track from C to J passes T. T lies beyond the radius of the crossing on its own, but within the radius of
   * the intersection once J has joined, and it connects two member nodes, so it joins too. A one-way road leaves D and
   * comes back into J through X, 55 m in all. Once J has joined, traffic reaches it only through the intersection, so
   * X joins and the road becomes internal: the links reached only through the intersection are looked for again once
   * it has grown, up to 60 m back along the road. X is no enclosed node, since the link from D to it is longer than an
   * internal link may be
   * <pre>
   *         C ---> D
   *        ^ #      \         # tram track C-T-J, 22 m each link
   *        |   T     \        D-X 44 m, X-J 11 m, one-way as drawn
   *        | #        X
   *        |  /-------'
   *        | v
   *  W <-> J <-> E
   * </pre>
   */
  @Test
  public void junctionAnApproachWithoutSignalLeadsBackToIsPulledInFurther() {
    int j = crossingWithRoadWithoutSignalFromJunctionBelow();
    var tramway = tramway();
    int t = node(6, -21);
    link(C, t, 22, true, tramway);
    link(t, j, 22, true, tramway);
    int x = node(14, -40);
    oneWayRoad(D, x, 44);
    oneWayRoad(x, j, 11);
    var junctionBelow = signalisedJunctionAt(j);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(j)));
    assertTrue(consolidated.hasMemberNode(nodes.get(t)));
    assertTrue(consolidated.hasMemberNode(nodes.get(x)));
    assertTrue(consolidated.getInternalSegments().containsAll(List.of(segment(j, C), segment(D, x), segment(x, j))));
    assertEquals(List.of(junctionBelow),
        result.getIntersections(JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL));
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).isEmpty());
  }

  /**
   * K lies above the crossing as far beyond the radius plus the margin as J below, joined to A by a footway only, so
   * no approach without a signal leads back to it. Once J is pulled in to salvage the road from it, the centre and
   * radius move, and K lies within the radius plus the margin, so it is pulled in as well
   * <pre>
   *  W <--> K <--> E          K lies the radius plus the margin plus 10 m above the centre, its own roads 85 m
   *        ~
   *       ~                   ~ footway from A to K, 44 m
   *         A <--- B
   *         |      ^
   *         v      |
   *         C ---> D          the crossing, with J below as drawn there
   * </pre>
   */
  @Test
  public void junctionWithinTheRadiusOnceAnApproachWithoutSignalIsSalvagedIsPulledIn() {
    int j = crossingWithRoadWithoutSignalFromJunctionBelow();
    var junctionBelow = signalisedJunctionAt(j);
    int k = node(7.5, 7.5 + BEYOND_THE_MARGIN);
    link(A, k, 44, true, footway);
    twoWayRoad(k, node(-77.5, 7.5 + BEYOND_THE_MARGIN), 85);
    twoWayRoad(k, node(92.5, 7.5 + BEYOND_THE_MARGIN), 85);
    var junctionAbove = signalisedJunctionAt(k);

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(j)));
    assertTrue(consolidated.hasMemberNode(nodes.get(k)));
    assertEquals(List.of(junctionBelow),
        result.getIntersections(JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL));
    assertEquals(List.of(junctionAbove), result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS));
  }

  /**
   * A crossing that joins another junction never brings that junction within reach of a pull-in: crossings join once
   * the junctions are grouped. Crossing X lies on the west arm of a lone junction L and joins it. X lies within the
   * radius plus the margin of the divided roads crossing once J below has been pulled in to salvage the road from it,
   * and a footway joins it to D, but L stays apart: X was no member of L while junctions were pulled in
   * <pre>
   *         A <--- B
   *         |      ^
   *         v      |
   *         C ---> D ~~~~~~~~~~~~ X ------- L <--> E2     footway D-X 51 m; dead end W2-X 25 m, X-L 35 m, two-way;
   *         ^              W2 ----'                       L at (100, 7.5), its own roads 85 m east and north
   *         |
   *  W <--> J <--> E          J below as in the other tests, its road into C without a signal
   * </pre>
   */
  @Test
  public void crossingThatJoinsAnotherJunctionNeverBringsItWithinReach() {
    int j = crossingWithRoadWithoutSignalFromJunctionBelow();
    signalisedJunctionAt(j);
    int l = node(100, 7.5);
    twoWayRoad(l, node(185, 7.5), 85);
    twoWayRoad(l, node(100, 92.5), 85);
    int x = node(65, 7.5);
    twoWayRoad(x, l, 35);
    twoWayRoad(node(40, 7.5), x, 25);
    link(D, x, 51, true, footway);
    signalisedJunctionAt(l);
    signalisedCrossingAt(x);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
    var withL = intersections.stream().filter(intersection -> intersection.hasMemberNode(nodes.get(l)))
        .findFirst().orElseThrow();
    assertTrue(withL.hasMemberNode(nodes.get(x)));
    assertFalse(withL.hasMemberNode(nodes.get(C)));
  }

  /**
   * A lone junction pulls in a junction within the margin of it like a consolidated intersection does, its radius
   * being nil: two signalised junctions 10 m apart, joined by a footway only, so no road brings them into one cluster,
   * become one intersection
   * <pre>
   *   W1 <--> J1 <--> E1      J1's own roads, 85 m
   *           ~               ~ footway from J1 to J2, 12 m
   *   W2 <--> J2 <--> E2      J2 10 m below J1, its own roads 85 m
   * </pre>
   */
  @Test
  public void loneJunctionPullsInAJunctionWithinTheMargin() {
    int[] junctions = loneJunctionsLinkedByAFootway(10, 12);

    var result = consolidate();

    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
    var consolidated = onlySpatialIntersection();
    assertTrue(consolidated.hasMemberNode(nodes.get(junctions[0])));
    assertTrue(consolidated.hasMemberNode(nodes.get(junctions[1])));
  }

  /**
   * Two lone junctions either side of a tram track, P and J, each with its only approach with a signal on the road from
   * the track. J lies within the margin of P, plus the allowance for the track, so P would pull it in, with X. Both
   * approaches would then become internal, leaving no approach with a signal, so J is not pulled in and each keeps its
   * own
   * <pre>
   *   W1 <---> P <---> E1     P's own roads, 85 m; its approach X->P with a signal
   *            |
   *    # # # # X # # # #      # tram track crossing the road at X; P-X 20 m, X-J 25 m, two-way
   *            |
   *   W2 <---> J <---> E2     J's own roads, 85 m, J 44 m below P; its approach X->J with a signal
   * </pre>
   */
  @Test
  public void junctionWhosePullInWouldLeaveNoApproachWithASignalStaysApart() {
    int p = node(0, 0);
    int x = node(0, -20);
    int j = node(0, -44);
    twoWayRoad(p, x, 20);
    twoWayRoad(x, j, 25);
    twoWayRoad(p, node(-85, 0), 85);
    twoWayRoad(p, node(85, 0), 85);
    twoWayRoad(j, node(-85, -44), 85);
    twoWayRoad(j, node(85, -44), 85);
    var tramway = tramway();
    link(x, node(-85, -20), 85, true, tramway);
    link(x, node(85, -20), 85, true, tramway);
    var junctionP = signalisedJunctionApproachedOnlyFrom(p, x);
    var junctionJ = signalisedJunctionApproachedOnlyFrom(j, x);

    var result = consolidate();

    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
    assertEquals(2, intersections.size());
    assertTrue(junctionP.hasOwnSignal(segment(x, p)));
    assertTrue(junctionJ.hasOwnSignal(segment(x, j)));
  }

  /**
   * Lone junction J1's road from S has no signal, and the road back from S reaches junction J2 within the salvage
   * reach, through F, after which J2's stop line stands. A side road comes into S, so its traffic meets that stop line
   * before it enters J2: S and F lie in front of J2. The road would not become internal, so J2 is not pulled in to
   * salvage it, and the road stays an approach without a signal
   * <pre>
   *                              N
   *                              |
   *   W2 <--> J2 <-[s]- F <----> S <----> J1 <-[s]- E1     J2-F 16 m with J2's stop line [s], F-S 14 m, S-J1 12 m,
   *                                                        42 m along the road, beyond the cluster reach of 40 m;
   *                                                        J2 40 m from J1 in a straight line; side road S-N 85 m;
   *                                                        other arms 85 m; all two-way
   * </pre>
   */
  @Test
  public void junctionWhoseRoadBackPassesANodeInFrontIsNotPulledInToSalvageIt() {
    int j2 = node(0, 0);
    int f = node(15, 0);
    int s = node(28, 0);
    int j1 = node(40, 0);
    var stopLineOfJ2 = twoWayRoad(f, j2, 16);
    twoWayRoad(s, f, 14);
    twoWayRoad(j1, s, 12);
    twoWayRoad(s, node(28, 85), 85);
    twoWayRoad(j2, node(-85, 0), 85);
    int e1 = node(125, 0);
    var stopLineOfJ1 = twoWayRoad(e1, j1, 85);
    signalisedJunctionApproachedOnlyFrom(j2, f);
    signalisedJunctionApproachedOnlyFrom(j1, e1);
    var stopLines = Set.of(stopLineOfJ2.getLinkSegmentAb(), stopLineOfJ1.getLinkSegmentAb());

    var result = consolidate(stopLines::contains);

    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL).isEmpty());
    assertEquals(2, intersections.size());
    assertTrue(result.getApproaches(ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE).contains(segment(s, j1)));
  }

  /** Two lone junctions 35 m apart, beyond the margin, joined by a footway only, stay apart */
  @Test
  public void loneJunctionBeyondTheMarginStaysApart() {
    loneJunctionsLinkedByAFootway(IntersectionConsolidatorSettings.DEFAULT_PULL_IN_MARGIN_BEYOND_RADIUS_METERS + 5, 36);

    var result = consolidate();

    assertEquals(2, intersections.size());
    assertTrue(result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).isEmpty());
  }

  /**
   * A second divided roads crossing lies 40 m below the first, joined to it only by a footway from C to its corner A2,
   * so each becomes a consolidated intersection of its own. A2 lies within the radius plus the margin of the first, so
   * the pull-in merges the two. Each of the eight parsed junctions is then listed as replaced once, by the one
   * intersection left. The intersection consolidation made for one crossing and merged away is not listed itself
   * <pre>
   *         A <--- B
   *         |      ^
   *         v      |
   *         C ---> D          the divided roads crossing, its arms as drawn there
   *         ~
   *         ~                 ~ footway from C to A2, 25 m
   *        A2 <--- B2
   *         |      ^
   *         v      |
   *        C2 ---> D2         the same crossing again, 40 m lower, with arms of its own
   * </pre>
   */
  @Test
  public void eachJunctionIsListedAsReplacedOnceWhenAConsolidatedIntersectionIsPulledIn() {
    dividedRoadsCrossing();
    int[] lower = dividedRoadsCrossingAt(0, -40);
    link(C, lower[0], 25, true, footway);
    var junctions = new HashSet<Intersection>();
    for (int corner : List.of(A, B, C, D, lower[0], lower[1], lower[2], lower[3])) {
      junctions.add(signalisedJunctionAt(corner));
    }

    var result = consolidate();

    var consolidated = onlySpatialIntersection();
    assertEquals(1, result.getIntersections(JUNCTION_PULLED_IN_WITHIN_RADIUS).size());
    var replaced = result.getReplacedIntersections();
    assertEquals(junctions.size(), replaced.size());
    assertEquals(junctions, replaced.stream().map(Pair::first).collect(Collectors.toSet()));
    assertTrue(replaced.stream().allMatch(pair -> pair.second() == consolidated));
  }
}
