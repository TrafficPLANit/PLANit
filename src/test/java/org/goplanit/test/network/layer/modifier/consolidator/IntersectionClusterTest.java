package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.junit.jupiter.api.Test;

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
    var settings = new IntersectionConsolidatorSettings();
    settings.setMinimumCoverageGainPerMember(minimumCoverageGainPerMember);
    settings.setCoverageTieMargin(coverageTieMargin);
    return layer.getLayerModifier().consolidateIntersections(PlanitJtsCrsUtils.CARTESIANCRS, settings, segment -> false);
  }

  /**
   * A junction with no other junction within 25 m along the road is kept as it is
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
    assertEquals(4, result.getApproachSegmentsMadeInternal().size());
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
    assertTrue(result.getApproachSegmentsMadeInternal().contains(stopLine));
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
   * Apart, P and Q score about 0.05 more than together. With the default tie margin of 0.001 they stay apart; with a
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

    consolidate();
    assertEquals(2, intersections.size());

    var result = consolidate(IntersectionConsolidatorSettings.DEFAULT_MINIMUM_COVERAGE_GAIN_PER_MEMBER, 0.1);
    assertEquals(1, intersections.size());
    assertEquals(2, result.getReplacedIntersections().size());
  }

  /**
   * Eleven junctions 20 m apart along a road form one intersection cluster, more than the ten that are subdivided. They
   * are kept as they are and the cluster is listed
   */
  @Test
  public void intersectionClusterTooLargeToSubdivideIsKeptAndListed() {
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

    assertEquals(11, intersections.size());
    assertEquals(1, result.getIntersectionClustersTooLargeToSubdivide().size());
    assertEquals(11, result.getIntersectionClustersTooLargeToSubdivide().get(0).size());
  }
}
