package org.goplanit.test.network.layer.modifier.consolidator;

import java.util.List;
import java.util.Set;

import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.junit.jupiter.api.Test;

import static org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome.RAMP_METER_JOINED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests how consolidation treats a ramp meter: like a crossing, it joins a signalised junction within the joining reach
 * along the road, and beyond it stays alone; listed with a junction to join, it is joined whatever the distance. The
 * intersection joined is a junction holding a ramp meter, as one a crossing joined is a junction and a crossing.
 * Unlike a crossing's, a ramp meter's road approach keeps its
 * signal once it is internal: a ramp meter is not run with the junction's signals. Each node's external id is its name
 *
 * @author markr
 */
public class RampMetersTest extends IntersectionConsolidationTestBase {

  /** indices of the nodes of {@link #rampMeterBeforeJunction(double)} and {@link #rampMeterAfterJunction()} */
  private static final int A = 0, R = 1, J = 2, E = 3, N = 4, S = 5;

  /** the external id of each node, by index */
  private static final String[] NAMES = {"A", "R", "J", "E", "N", "S"};

  /** give each node its name as external id, the signalised junction J its road approaches, and R a ramp meter */
  private Intersection namesJunctionAndRampMeter() {
    for (int index = 0; index < NAMES.length; ++index) {
      nodes.get(index).setExternalId(NAMES[index]);
    }
    signalisedJunctionWithRoadApproachesAt(J);
    return intersectionAt(R, IntersectionControlType.SIGNALISED, IntersectionType.RAMP_METER);
  }

  /**
   * The ramp meter R on a one-way road east into the signalised junction J
   * <pre>
   *                      N
   *                      |
   *   A -->--- R -->---- J ------ E     A-R 80 m one-way east, R-J the given distance one-way east, R a ramp meter
   *                      |              the other arms 85 m two-way
   *                      S
   * </pre>
   *
   * @param metersBeforeJunction from R to J
   * @return the ramp meter
   */
  private Intersection rampMeterBeforeJunction(double metersBeforeJunction) {
    node(-metersBeforeJunction - 80, 0);
    node(-metersBeforeJunction, 0);
    node(0, 0);
    node(85, 0);
    node(0, 85);
    node(0, -85);
    oneWayRoad(A, R, 80);
    oneWayRoad(R, J, metersBeforeJunction);
    twoWayRoad(J, E, 85);
    twoWayRoad(N, J, 85);
    twoWayRoad(J, S, 85);
    return namesJunctionAndRampMeter();
  }

  /**
   * The ramp meter R on a one-way on-ramp leaving the signalised junction J south, 20 m from it
   * <pre>
   *                      N
   *                      |
   *   A ---------------- J ------ E     A-J, N-J and J-E 85 m two-way
   *                      |
   *                      v              J-R 20 m one-way south, R a ramp meter approached from J
   *                      R
   *                      |              R-S 85 m one-way south
   *                      S
   * </pre>
   *
   * @return the ramp meter
   */
  private Intersection rampMeterAfterJunction() {
    node(-85, 0);
    node(0, -20);
    node(0, 0);
    node(85, 0);
    node(0, 85);
    node(0, -105);
    twoWayRoad(A, J, 85);
    twoWayRoad(J, E, 85);
    twoWayRoad(N, J, 85);
    oneWayRoad(J, R, 20);
    oneWayRoad(R, S, 85);
    return namesJunctionAndRampMeter();
  }

  /** A ramp meter 20 m before the junction, within the joining reach, joins it: one intersection holds R and J, a
   * junction holding a ramp meter, approached from A as the ramp meter was */
  @Test
  public void rampMeterWithinReachBeforeJunctionJoinsIt() {
    var rampMeter = rampMeterBeforeJunction(20);

    var result = consolidate();

    var joined = onlyIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(R)));
    assertTrue(joined.hasMemberNode(nodes.get(J)));
    assertEquals(Set.of(IntersectionType.JUNCTION, IntersectionType.RAMP_METER), joined.getTypes());
    assertFalse(joined.isRampMeterOnly());
    assertTrue(joined.hasOwnSignal(segment(A, R)));
    assertEquals(List.of(rampMeter), result.getIntersections(RAMP_METER_JOINED));
  }

  /** A ramp meter 20 m along an on-ramp leaving the junction joins it, and its approach from the junction becomes an
   * internal segment with a signal */
  @Test
  public void rampMeterWithinReachAfterJunctionKeepsItsSignalOnceInternal() {
    rampMeterAfterJunction();

    consolidate();

    var joined = (SpatialIntersection) onlyIntersection();
    assertEquals(Set.of(nodes.get(J), nodes.get(R)), Set.copyOf(joined.getMemberNodes()));
    assertEquals(List.of(segment(J, R)), joined.getInternalSegmentsWithSignal());
  }

  /** A ramp meter 100 m before the junction, beyond the joining reach, stays as it is: its node, its approach and its
   * type */
  @Test
  public void rampMeterBeyondReachStaysAlone() {
    var rampMeter = rampMeterBeforeJunction(100);

    consolidate();

    assertTrue(intersections.contains(rampMeter));
    assertEquals(List.of(nodes.get(R)), rampMeter.getMemberNodes());
    assertEquals(List.of(segment(A, R)), rampMeter.getApproachSegments());
    assertEquals(Set.of(IntersectionType.RAMP_METER), rampMeter.getTypes());
  }

  /** Listed to join with the junction, a ramp meter beyond the joining reach is joined all the same: one intersection
   * holds R and J, a junction holding a ramp meter */
  @Test
  public void rampMeterListedToJoinWithJunctionBecomesPartOfJunction() {
    rampMeterBeforeJunction(100);

    consolidateWith(settings -> settings.addNodeIdsToJoinIntoOneIntersection(
        List.of("R", "J"), IdMapperType.EXTERNAL_ID));

    var joined = onlyIntersection();
    assertTrue(joined.hasMemberNode(nodes.get(R)));
    assertTrue(joined.hasMemberNode(nodes.get(J)));
    assertEquals(Set.of(IntersectionType.JUNCTION, IntersectionType.RAMP_METER), joined.getTypes());
  }
}
