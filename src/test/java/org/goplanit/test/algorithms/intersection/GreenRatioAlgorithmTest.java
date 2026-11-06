package org.goplanit.test.algorithms.intersection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.goplanit.algorithms.intersection.GreenRatioAlgorithmBase;
import org.goplanit.algorithms.intersection.GreenRatioAlgorithmFactory;
import org.goplanit.algorithms.intersection.GreenRatioAlgorithmType;
import org.goplanit.algorithms.intersection.GreenRatioAndSaturationFlowFactorResult;
import org.goplanit.algorithms.intersection.GreenRatioAndSaturationFlowFactorSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegmentKind;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the algorithms computing green ratios and saturation flow factors at signalised intersections, on small layers
 * whose junctions have arms of 100 m at given compass bearings from their centre node. At the defaults a phase change
 * loses 5 s of a 90 s cycle
 *
 * @author markr
 */
public class GreenRatioAlgorithmTest extends LayerTestBase {

  /** An algorithm sharing out more green time than there is, to test that the base refuses it */
  private static class SharingTooMuchGreenTime extends GreenRatioAlgorithmBase {

    private SharingTooMuchGreenTime() {
      super(new GreenRatioAndSaturationFlowFactorSettings());
    }

    @Override
    protected double[] shareUsableGreenTime(
        Intersection junction, List<List<IntersectionSegment>> phases, double usableGreenTimeSeconds) {
      return phases.stream().mapToDouble(phase -> usableGreenTimeSeconds).toArray();
    }

    @Override
    public GreenRatioAlgorithmType getType() {
      return GreenRatioAlgorithmType.EQUAL_SPLIT;
    }
  }

  /** green ratio of each approach of a four-arm junction under equal split: (90 - 2 x 5) / 2 / 90 */
  private static final double FOUR_ARM_EQUAL_SPLIT = 40.0 / 90;

  /** green ratio of each approach of a six-arm junction under equal split: (90 - 3 x 5) / 3 / 90 */
  private static final double SIX_ARM_EQUAL_SPLIT = 25.0 / 90;

  /** run the algorithm of the settings on the intersections, in the Cartesian coordinates of the nodes */
  private static GreenRatioAndSaturationFlowFactorResult compute(
      GreenRatioAndSaturationFlowFactorSettings settings, Intersection... intersections) {
    return GreenRatioAlgorithmFactory.create(settings).computeGreenRatiosAndSaturationFlowFactors(
        List.of(intersections), CARTESIAN);
  }

  /** run equal split, otherwise at the defaults, on the intersections */
  private static GreenRatioAndSaturationFlowFactorResult computeEqualSplit(Intersection... intersections) {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setAlgorithmType(GreenRatioAlgorithmType.EQUAL_SPLIT);
    return compute(settings, intersections);
  }

  /** the green ratio of an approach with a signal */
  private static double greenRatioOf(IntersectionSegment approach) {
    return ((IntersectionSignalSegment) approach).getGreenRatio();
  }

  /** how the green ratio of an approach with a signal got its value */
  private static CapacityFactorOrigin greenRatioOriginOf(IntersectionSegment approach) {
    return ((IntersectionSignalSegment) approach).getGreenRatioOrigin();
  }

  /** assert that a junction has approaches of a type, each with the expected green ratio */
  private static void assertGreenRatioOfApproachesOfType(
      Intersection junction, MacroscopicLinkSegmentType type, double expectedGreenRatio) {
    var ofType = junction.getApproaches().stream().filter(
        approach -> ((MacroscopicLinkSegment) approach.getLinkSegment()).getLinkSegmentType() == type)
        .collect(Collectors.toList());
    assertFalse(ofType.isEmpty());
    ofType.forEach(approach -> assertEquals(expectedGreenRatio, greenRatioOf(approach), 1e-9));
  }

  private MacroscopicLinkSegmentType road;

  /** a light rail track, at the capacity a parsed network gives rail */
  private MacroscopicLinkSegmentType track;

  /**
   * Add an arm of 100 m to a node, a two-way road at a compass bearing from it
   *
   * @param node index of the node
   * @param bearingDegrees of the arm's far end seen from the node, 0 north, 90 east
   * @param type of the road
   * @return index of the arm's far end
   */
  private int arm(int node, double bearingDegrees, MacroscopicLinkSegmentType type) {
    var position = nodes.get(node).getPosition().getCoordinate();
    double radians = Math.toRadians(bearingDegrees);
    int end = node(position.x + 100 * Math.sin(radians), position.y + 100 * Math.cos(radians));
    twoWay(end, node, type);
    return end;
  }

  /**
   * A centre node with a road arm at each of the given compass bearings
   *
   * @param x metres east of the centre
   * @param y metres north of the centre
   * @param bearingsDegrees of the arms
   * @return index of the centre node
   */
  private int centreWithArms(double x, double y, double... bearingsDegrees) {
    int centre = node(x, y);
    for (double bearing : bearingsDegrees) {
      arm(centre, bearing, road);
    }
    return centre;
  }

  /**
   * A centre node at the origin with one axis of two opposite arms per type, the axes spread evenly: two axes at 0 and
   * 90 degrees, three at 0, 60 and 120
   *
   * @param typePerAxis the type of both arms of each axis
   * @return index of the centre node
   */
  private int centreWithAxesOf(MacroscopicLinkSegmentType... typePerAxis) {
    int centre = node(0, 0);
    for (int axis = 0; axis < typePerAxis.length; ++axis) {
      double bearing = 180.0 * axis / typePerAxis.length;
      arm(centre, bearing, typePerAxis[axis]);
      arm(centre, bearing + 180, typePerAxis[axis]);
    }
    return centre;
  }

  /** the approach entering on the link segment from node a to node b */
  private IntersectionSegment approach(Intersection intersection, int a, int b) {
    return intersection.getApproach(segment(a, b));
  }

  /**
   * A signalised ramp meter in the middle of a one-way road of 200 m running east, its only approach the segment
   * arriving from the west
   *
   * @param y metres north of the origin
   * @return the ramp meter
   */
  private Intersection rampMeterOnOneWayRoadAt(double y) {
    int meter = node(0, y);
    link(node(-100, y), meter, road, null);
    link(meter, node(100, y), road, null);
    return intersectionAt(meter, IntersectionControlType.SIGNALISED, IntersectionType.RAMP_METER);
  }

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var lightRail = network.getModes().getFactory().registerNew(PredefinedModeType.LIGHTRAIL);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(lightRail));
    road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    track = layer.getLinkSegmentTypes().getFactory().registerNew("track", 10000, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(track, 50, List.of(lightRail));
  }

  // CHOOSING AN ALGORITHM

  /** Default settings create capacity weighted, and the type in the settings chooses the algorithm */
  @Test
  public void typeInTheSettingsChoosesTheAlgorithm() {
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    assertEquals(GreenRatioAlgorithmType.CAPACITY_WEIGHTED, GreenRatioAlgorithmFactory.create(settings).getType());

    settings.setAlgorithmType(GreenRatioAlgorithmType.EQUAL_SPLIT);
    assertEquals(GreenRatioAlgorithmType.EQUAL_SPLIT, GreenRatioAlgorithmFactory.create(settings).getType());
  }

  /** The reach over which the direction at the stop line is measured is 25 m by default, and a negative one is
   * refused */
  @Test
  public void stopLineDirectionReachIsTwentyFiveMetresByDefault() {
    var algorithm = (GreenRatioAlgorithmBase) GreenRatioAlgorithmFactory.create(
        new GreenRatioAndSaturationFlowFactorSettings());

    assertEquals(25, algorithm.getStopLineDirectionReachMeters());
    assertFalse(algorithm.setStopLineDirectionReachMeters(-1));
    assertEquals(25, algorithm.getStopLineDirectionReachMeters());
    assertTrue(algorithm.setStopLineDirectionReachMeters(10));
    assertEquals(10, algorithm.getStopLineDirectionReachMeters());
  }

  /** An unsignalised junction gets no values */
  @Test
  public void unsignalisedJunctionGetsNoValues() {
    var junction = intersectionAt(centreWithArms(0, 0, 0, 90, 180, 270), IntersectionControlType.UNSIGNALISED,
        IntersectionType.JUNCTION);

    var result = computeEqualSplit(junction);

    junction.getApproaches().forEach(approach -> {
      assertEquals(1, approach.getEffectiveCapacityFactor());
      assertNull(approach.getSaturationFlowFactorOrigin());
    });
    assertTrue(result.getComputedGreenRatios().isEmpty());
    assertNull(result.getNumberOfPhases(junction));
  }

  // JUNCTIONS

  /**
   * A four-arm junction has two phases of opposite approaches, each approach with a computed green ratio of 0.44 and
   * its saturation flow factor left at 1
   */
  @Test
  public void fourArmJunctionHasTwoPhases() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));

    var result = computeEqualSplit(junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    assertEquals(4, result.getComputedGreenRatios().size());
    junction.getApproaches().forEach(approach -> {
      assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9);
      assertEquals(CapacityFactorOrigin.COMPUTED, greenRatioOriginOf(approach));
      assertEquals(1, approach.getSaturationFlowFactor());
      assertNull(approach.getSaturationFlowFactorOrigin());
    });
  }

  /** A T junction has two phases, the main road and the side street on its own, so all get 0.44 */
  @Test
  public void tJunctionHasTwoPhases() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180));

    var result = computeEqualSplit(junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    junction.getApproaches().forEach(approach -> assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9));
  }

  /** A six-arm junction of three pairs facing each other has three phases, so each approach gets 0.28 */
  @Test
  public void sixArmJunctionHasThreePhases() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 60, 120, 180, 240, 300));

    var result = computeEqualSplit(junction);

    assertEquals(3, result.getNumberOfPhases(junction));
    junction.getApproaches().forEach(approach -> assertEquals(SIX_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9));
  }

  /**
   * Approaches arriving side by side share a phase with the traffic facing them: a crossroads with a tram track 5
   * degrees beside each road has eight approaches on two axes, so two phases and 0.44 for all eight
   */
  @Test
  public void approachesArrivingSideBySideShareAPhase() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 5, 90, 95, 180, 185, 270, 275));

    var result = computeEqualSplit(junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    assertEquals(8, junction.getApproaches().size());
    junction.getApproaches().forEach(approach -> assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9));
  }

  /**
   * A junction with approaches on more axes than the maximum number of phases has the closest axes merged: five arms
   * 36 degrees apart are five axes, merged down to the default of four phases, so each approach gets
   * (90 - 4 x 5) / 4 / 90, about 0.19
   */
  @Test
  public void moreAxesThanTheMaximumAreMergedDownToIt() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 36, 72, 108, 144));

    var result = computeEqualSplit(junction);

    assertEquals(4, result.getNumberOfPhases(junction));
    junction.getApproaches().forEach(approach -> assertEquals(70.0 / 4 / 90, greenRatioOf(approach), 1e-9));
  }

  /**
   * With a maximum of three phases, the two closest axes merge first. Arms at 0, 35, 90 and 130 degrees are four axes,
   * the closest 0 and 35. Capacity weighted, with 3,600 on the arm at 35 and 1,800 on the others, the merged phase
   * weighs as 3,600, so the arm at 0 gets 75 x 3,600 / 7,200 = 37.5 s of green
   */
  @Test
  public void closestAxesMergeFirst() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 3600, 180);
    int centre = node(0, 0);
    int north = arm(centre, 0, road);
    arm(centre, 35, mainRoad);
    arm(centre, 90, road);
    arm(centre, 130, road);
    var junction = signalisedJunctionAt(centre);
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setAlgorithmType(GreenRatioAlgorithmType.CAPACITY_WEIGHTED);
    settings.setMaximumNumberOfPhases(3);

    var result = compute(settings, junction);

    assertEquals(3, result.getNumberOfPhases(junction));
    assertEquals(37.5 / 90, greenRatioOf(approach(junction, north, centre)), 1e-9);
  }

  /**
   * An approach without a signal of its own, here a fifth arm at 45 degrees, gets a computed saturation flow factor of
   * 0.5, which is its effective capacity factor, and takes no part in the phases, so the junction still has two
   */
  @Test
  public void approachWithoutSignalOfItsOwnGetsAHalf() {
    int centre = centreWithArms(0, 0, 0, 90, 180, 270);
    int minor = arm(centre, 45, road);
    var junction = signalisedJunctionAt(centre);
    junction.swapApproachToNewSignalOrUnsignalised(segment(minor, centre), false);
    var withoutSignal = approach(junction, minor, centre);

    var result = computeEqualSplit(junction);

    assertFalse(withoutSignal.hasSignal());
    assertEquals(0.5, withoutSignal.getSaturationFlowFactor());
    assertEquals(CapacityFactorOrigin.COMPUTED, withoutSignal.getSaturationFlowFactorOrigin());
    assertEquals(0.5, withoutSignal.getEffectiveCapacityFactor());
    assertEquals(2, result.getNumberOfPhases(junction));
    assertTrue(result.getApproachesWithoutSignalGivenValues().contains(withoutSignal));
    assertFalse(result.getComputedGreenRatios().containsKey(withoutSignal));
  }

  /**
   * The direction of an approach is that of its last stretch into the junction, along its link's geometry, in either
   * direction of the geometry. The north arm starts north-east of the centre and curves to arrive from the north; the
   * west arm, with its geometry from the centre outwards, starts north-west and curves to arrive from the west. Seen
   * node to node neither is opposite another arm, so pairing them with the south and east arms takes their last stretch
   * <pre>
   *   (-70,70)          (70,70)
   *         \          /
   *          \  (0,10)
   *  (-10,0)  \___|      (100,0)
   *    -------- (0,0) --------
   *               |
   *            (0,-100)
   * </pre>
   */
  @Test
  public void directionAtTheStopLineIsThatOfTheLastStretch() {
    int centre = node(0, 0);
    int north = node(70, 70);
    twoWay(north, centre, road).setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate[] {new Coordinate(70, 70), new Coordinate(0, 10), new Coordinate(0, 0)}));
    int west = node(-70, 70);
    twoWay(centre, west, road).setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(-10, 0), new Coordinate(-70, 70)}));
    arm(centre, 180, road);
    arm(centre, 90, road);
    var junction = signalisedJunctionAt(centre);

    var result = computeEqualSplit(junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach(junction, north, centre)), 1e-9);
    assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach(junction, west, centre)), 1e-9);
  }

  /** A cycle of 8 s leaves no green time for two phases: the junction gets no values and is listed as not computable,
   * with its usable green time of 8 - 2 x 5 s */
  @Test
  public void junctionWithoutUsableGreenTimeIsNotComputable() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setCycleTimeSeconds(8);

    var result = compute(settings, junction);

    junction.getApproaches().forEach(approach -> assertNull(greenRatioOriginOf(approach)));
    assertEquals(-2, result.getNotComputableIntersections().get(junction), 1e-9);
  }

  /** Green times shared out that add up to more than the usable green time are refused: the junction gets no values and
   * is listed as not computable */
  @Test
  public void invalidGreenTimesAreRefused() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));

    var result = new SharingTooMuchGreenTime().computeGreenRatiosAndSaturationFlowFactors(
        junction, CARTESIAN);

    junction.getApproaches().forEach(approach -> assertNull(greenRatioOriginOf(approach)));
    assertTrue(result.getNotComputableIntersections().containsKey(junction));
    assertNull(result.getNumberOfPhases(junction));
  }

  // CROSSINGS

  /** At a signalised mid-block crossing both road approaches get 0.9 */
  @Test
  public void midBlockCrossingGetsNineTenths() {
    var crossing = intersectionAt(
        centreWithArms(0, 0, 0, 180), IntersectionControlType.SIGNALISED, IntersectionType.CROSSING);

    var result = computeEqualSplit(crossing);

    crossing.getApproaches().forEach(approach -> {
      assertEquals(0.9, greenRatioOf(approach), 1e-9);
      assertEquals(CapacityFactorOrigin.COMPUTED, greenRatioOriginOf(approach));
    });
    assertNull(result.getNumberOfPhases(crossing));
  }

  // RAMP METERS

  /** The one approach of a ramp meter is of its own kind and gets 0.9, so its effective capacity factor is 0.9 */
  @Test
  public void rampMeterGetsNineTenths() {
    var rampMeter = rampMeterOnOneWayRoadAt(0);

    var result = computeEqualSplit(rampMeter);

    var approach = rampMeter.getApproaches().get(0);
    assertEquals(1, rampMeter.getApproaches().size());
    assertEquals(IntersectionSegmentKind.APPROACH_WITH_SIGNAL_AT_RAMP_METER,
        rampMeter.getIntersectionSegmentKindOf(approach.getLinkSegment()));
    assertEquals(0.9, greenRatioOf(approach), 1e-9);
    assertEquals(CapacityFactorOrigin.COMPUTED, greenRatioOriginOf(approach));
    assertEquals(0.9, rampMeter.getEffectiveCapacityFactorOf(approach.getLinkSegment()), 1e-9);
    assertNull(result.getNumberOfPhases(rampMeter));
  }

  /** The ramp meter green ratio is a setting of its own: set to 0.6, a ramp meter gets 0.6 while a signalised
   * mid-block crossing keeps the isolated crossing green ratio of 0.9 */
  @Test
  public void rampMeterGreenRatioIsASettingApartFromIsolatedCrossings() {
    var rampMeter = rampMeterOnOneWayRoadAt(0);
    var crossing = intersectionAt(
        centreWithArms(0, 500, 0, 180), IntersectionControlType.SIGNALISED, IntersectionType.CROSSING);
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setRampMeterGreenRatio(0.6);

    compute(settings, rampMeter, crossing);

    assertEquals(0.6, greenRatioOf(rampMeter.getApproaches().get(0)), 1e-9);
    crossing.getApproaches().forEach(approach -> assertEquals(0.9, greenRatioOf(approach), 1e-9));
  }

  // WHAT AN ALGORITHM MAY CHANGE

  /** A green ratio of 0.6 set directly is kept, and its approach still takes part in the phases */
  @Test
  public void valueSetDirectlyIsKept() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    var setDirectly = (IntersectionSignalSegment) junction.getApproaches().get(0);
    setDirectly.setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);

    var result = computeEqualSplit(junction);

    assertEquals(0.6, setDirectly.getGreenRatio());
    assertEquals(CapacityFactorOrigin.SET_DIRECTLY, setDirectly.getGreenRatioOrigin());
    assertTrue(result.getApproachesKeepingValueSetDirectly().contains(setDirectly));
    assertEquals(2, result.getNumberOfPhases(junction));
    assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(junction.getApproaches().get(1)), 1e-9);
  }

  /** Run again once the junction is no longer signalised, its computed values are removed, and listed */
  @Test
  public void staleComputedValueIsRemoved() {
    var junction = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    computeEqualSplit(junction);
    junction.setControlType(IntersectionControlType.UNSIGNALISED);

    var result = computeEqualSplit(junction);

    junction.getApproaches().forEach(approach -> {
      assertEquals(1, greenRatioOf(approach));
      assertNull(greenRatioOriginOf(approach));
    });
    assertEquals(4, result.getApproachesWithStaleComputedValueRemoved().size());
  }

  /**
   * The internal segments and movements of a spatial junction keep their values. Its east arm enters node 1 east of the
   * centre, opposite the west arm, so it still has two phases
   */
  @Test
  public void internalSegmentsAndMovementsKeepTheirValues() {
    int centre = node(0, 0);
    int north = arm(centre, 0, road);
    int south = arm(centre, 180, road);
    arm(centre, 270, road);
    int east = node(10, 0);
    arm(east, 90, road);
    twoWay(centre, east, road);
    var junction = spatialJunctionOver(centre, east);
    nodes.get(centre).streamEntrySegments().map(MacroscopicLinkSegment.class::cast).forEach(junction::addNewApproach);
    nodes.get(east).streamEntrySegments().map(MacroscopicLinkSegment.class::cast).forEach(junction::addNewApproach);
    junction.addNewInternalSegmentWithSignal(segment(centre, east));
    junction.addNewInternalSegment(segment(east, centre));
    var withSignal = (IntersectionSignalSegment) junction.getIntersectionInternalSegment(segment(centre, east));
    withSignal.setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);
    var movement =
        layer.getSpatialMovements().getFactory().registerNew(segment(north, centre), segment(centre, south), 0);
    movement.setGreenRatio(0.15);
    junction.addMovement(movement);
    layer.getIntersections().getFactory().register(junction);

    var result = computeEqualSplit(junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    assertEquals(4, junction.getApproaches().size());
    assertEquals(0.6, withSignal.getGreenRatio());
    assertNull(junction.getIntersectionInternalSegment(segment(east, centre)).getSaturationFlowFactorOrigin());
    assertEquals(0.15, movement.getGreenRatio());
  }

  // RESULT AND SCOPE

  /** Run on two junctions at once, the result records two phases for the four-arm and three for the six-arm one */
  @Test
  public void resultRecordsThePhasesOfEachJunction() {
    var fourArms = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    var sixArms = signalisedJunctionAt(centreWithArms(1000, 0, 0, 60, 120, 180, 240, 300));

    var result = computeEqualSplit(fourArms, sixArms);

    assertEquals(2, result.getNumberOfPhases(fourArms));
    assertEquals(3, result.getNumberOfPhases(sixArms));
  }

  /** Run on two four-arm and one six-arm junction, the result gives two junctions with two phases and one with three */
  @Test
  public void resultGivesTheJunctionsPerNumberOfPhases() {
    var firstFourArms = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    var secondFourArms = signalisedJunctionAt(centreWithArms(1000, 0, 0, 90, 180, 270));
    var sixArms = signalisedJunctionAt(centreWithArms(2000, 0, 0, 60, 120, 180, 240, 300));

    var result = computeEqualSplit(firstFourArms, secondFourArms, sixArms);

    assertEquals(Map.of(2, 2L, 3, 1L), result.getNumberOfJunctionsByNumberOfPhases());
  }

  /** Run on one intersection, another is left as it was */
  @Test
  public void runOnOneIntersectionLeavesTheOthers() {
    var computed = signalisedJunctionAt(centreWithArms(0, 0, 0, 90, 180, 270));
    var leftAlone = signalisedJunctionAt(centreWithArms(1000, 0, 0, 90, 180, 270));

    GreenRatioAlgorithmFactory.create(new GreenRatioAndSaturationFlowFactorSettings())
        .computeGreenRatiosAndSaturationFlowFactors(computed, CARTESIAN);

    computed.getApproaches().forEach(approach -> assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9));
    leftAlone.getApproaches().forEach(approach -> assertNull(greenRatioOriginOf(approach)));
  }

  // CAPACITY WEIGHTED

  /**
   * Capacity weighted, a main road of 3,600 pcu/h each way gets 80 s x 3,600 / 5,400 of green, 0.59 of the cycle, and
   * a side road of 1,800 pcu/h each way the rest, 0.30
   */
  @Test
  public void greenWeightedByCapacity() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 3600, 180);
    int centre = node(0, 0);
    int north = arm(centre, 0, mainRoad);
    int south = arm(centre, 180, mainRoad);
    int east = arm(centre, 90, road);
    int west = arm(centre, 270, road);
    var junction = signalisedJunctionAt(centre);
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setAlgorithmType(GreenRatioAlgorithmType.CAPACITY_WEIGHTED);

    var result = compute(settings, junction);

    assertEquals(2, result.getNumberOfPhases(junction));
    double mainRoadGreenRatio = 80.0 * 3600 / 5400 / 90;
    double sideRoadGreenRatio = 80.0 * 1800 / 5400 / 90;
    assertEquals(mainRoadGreenRatio, greenRatioOf(approach(junction, north, centre)), 1e-9);
    assertEquals(mainRoadGreenRatio, greenRatioOf(approach(junction, south, centre)), 1e-9);
    assertEquals(sideRoadGreenRatio, greenRatioOf(approach(junction, east, centre)), 1e-9);
    assertEquals(sideRoadGreenRatio, greenRatioOf(approach(junction, west, centre)), 1e-9);
  }

  /** The capacity of a phase is the largest of its approaches': a main road of 3,600 one way and 1,800 the other
   * weighs as 3,600 */
  @Test
  public void phaseCapacityIsTheLargestOfItsApproaches() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 3600, 180);
    int centre = node(0, 0);
    int north = arm(centre, 0, mainRoad);
    arm(centre, 180, road);
    arm(centre, 90, road);
    arm(centre, 270, road);
    var junction = signalisedJunctionAt(centre);
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setAlgorithmType(GreenRatioAlgorithmType.CAPACITY_WEIGHTED);

    compute(settings, junction);

    assertEquals(80.0 * 3600 / 5400 / 90, greenRatioOf(approach(junction, north, centre)), 1e-9);
  }

  /** Without capacity on any phase, capacity weighted falls back to an equal split */
  @Test
  public void withoutCapacityGreenIsSplitEqually() {
    var noCapacity = layer.getLinkSegmentTypes().getFactory().registerNew("no capacity", 0, 180);
    int centre = node(0, 0);
    for (double bearing : new double[] {0, 90, 180, 270}) {
      arm(centre, bearing, noCapacity);
    }
    var junction = signalisedJunctionAt(centre);
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setAlgorithmType(GreenRatioAlgorithmType.CAPACITY_WEIGHTED);

    compute(settings, junction);

    junction.getApproaches().forEach(approach -> assertEquals(FOUR_ARM_EQUAL_SPLIT, greenRatioOf(approach), 1e-9));
  }

  // RAIL-BASED PHASE

  /**
   * A road crossing a light rail line has a road phase and a rail-based phase. Under either algorithm the rail-based
   * phase gets 0.1 of the cycle, 9 s, which comes off the usable green time of 90 - 2 x 5 = 80 s, so the road gets
   * 71 s
   */
  @Test
  public void railBasedPhaseGetsATenthOfTheCycleUnderEitherAlgorithm() {
    var junction = signalisedJunctionAt(centreWithAxesOf(track, road));
    for (var algorithmType : GreenRatioAlgorithmType.values()) {
      var settings = new GreenRatioAndSaturationFlowFactorSettings();
      settings.setAlgorithmType(algorithmType);

      var result = compute(settings, junction);

      assertEquals(2, result.getNumberOfPhases(junction));
      assertTrue(result.getJunctionsWithRailBasedPhase().contains(junction));
      assertGreenRatioOfApproachesOfType(junction, track, 0.1);
      assertGreenRatioOfApproachesOfType(junction, road, 71.0 / 90);
    }
  }

  /**
   * Rail approaches never share a phase with roads, and all of them share one rail-based phase. A crossroads with a
   * light rail line 5 degrees beside its north-south road, and a third track arriving from 95 degrees, has two road
   * phases and one rail-based phase: 90 - 3 x 5 = 75 s usable, 9 s for the rail, and 33 s for each road. Capacity
   * weighted gives the same, as the 10,000 per lane of the track does not weigh in a road phase
   */
  @Test
  public void railApproachesShareOneRailBasedPhaseApartFromTheRoads() {
    int centre = centreWithArms(0, 0, 0, 90, 180, 270);
    arm(centre, 5, track);
    arm(centre, 185, track);
    arm(centre, 95, track);
    var junction = signalisedJunctionAt(centre);
    for (var algorithmType : GreenRatioAlgorithmType.values()) {
      var settings = new GreenRatioAndSaturationFlowFactorSettings();
      settings.setAlgorithmType(algorithmType);

      var result = compute(settings, junction);

      assertEquals(3, result.getNumberOfPhases(junction));
      assertGreenRatioOfApproachesOfType(junction, track, 0.1);
      assertGreenRatioOfApproachesOfType(junction, road, 33.0 / 90);
    }
  }

  /**
   * A rail-based phase taking more green time than is usable leaves none to share: at a ratio of 0.9 it takes 81 s of
   * the 80 s usable, so the junction gets no values and is listed as not computable, with -1 s left to share
   */
  @Test
  public void railBasedGreenLeavingNoGreenToShareIsNotComputable() {
    var junction = signalisedJunctionAt(centreWithAxesOf(track, road));
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setFixedRailBasedPhaseGreenRatio(0.9);

    var result = compute(settings, junction);

    junction.getApproaches().forEach(approach -> assertNull(greenRatioOriginOf(approach)));
    assertEquals(-1, result.getNotComputableIntersections().get(junction), 1e-9);
  }

  // MINIMUM GREEN TIME

  /**
   * Capacity weighted, a side road of 600 against two main roads of 10,000 would get 75 x 600 / 20,600, about 2.2 s,
   * below the minimum green time of 5 s. It gets 5 s, and the main roads share the other 70 s by capacity, 35 s each
   */
  @Test
  public void phaseBelowTheMinimumGreenTimeGetsItAndTheOthersShareTheRest() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 10000, 180);
    var sideRoad = layer.getLinkSegmentTypes().getFactory().registerNew("side road", 600, 180);
    var junction = signalisedJunctionAt(centreWithAxesOf(mainRoad, mainRoad, sideRoad));

    var result = compute(new GreenRatioAndSaturationFlowFactorSettings(), junction);

    assertEquals(1, result.getNumberOfPhasesRaisedToMinimumGreenTime(junction));
    assertGreenRatioOfApproachesOfType(junction, sideRoad, 5.0 / 90);
    assertGreenRatioOfApproachesOfType(junction, mainRoad, 35.0 / 90);
  }

  /**
   * Phases sharing what is left after the minimum green time can drop below it in turn. Capacity weighted, phases of
   * 100, 750 and 10,000 first get 75 x 100 / 10,850, about 0.7 s, raised to 5 s, and 75 x 750 / 10,850, about 5.2 s.
   * Sharing the other 70 s gives the 750 phase 70 x 750 / 10,750, about 4.9 s, so it is raised to 5 s as well, and the
   * 10,000 phase gets the last 65 s
   */
  @Test
  public void phaseDroppingBelowTheMinimumGreenTimeWhenSharedAgainGetsItToo() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 10000, 180);
    var secondaryRoad = layer.getLinkSegmentTypes().getFactory().registerNew("secondary road", 750, 180);
    var lane = layer.getLinkSegmentTypes().getFactory().registerNew("lane", 100, 180);
    var junction = signalisedJunctionAt(centreWithAxesOf(mainRoad, secondaryRoad, lane));

    var result = compute(new GreenRatioAndSaturationFlowFactorSettings(), junction);

    assertEquals(2, result.getNumberOfPhasesRaisedToMinimumGreenTime(junction));
    assertGreenRatioOfApproachesOfType(junction, lane, 5.0 / 90);
    assertGreenRatioOfApproachesOfType(junction, secondaryRoad, 5.0 / 90);
    assertGreenRatioOfApproachesOfType(junction, mainRoad, 65.0 / 90);
  }

  /**
   * When the green time to share cannot give every phase the minimum green time, the phases share it equally. A cycle
   * of 25 s with three phases leaves 25 - 3 x 5 = 10 s, less than 3 x 5 s, so each phase gets 10 / 3 s whatever its
   * capacity
   */
  @Test
  public void phasesShareEquallyWhenTheMinimumGreenTimeCannotBeGivenToEach() {
    var mainRoad = layer.getLinkSegmentTypes().getFactory().registerNew("main road", 10000, 180);
    var lane = layer.getLinkSegmentTypes().getFactory().registerNew("lane", 100, 180);
    var junction = signalisedJunctionAt(centreWithAxesOf(mainRoad, road, lane));
    var settings = new GreenRatioAndSaturationFlowFactorSettings();
    settings.setCycleTimeSeconds(25);

    var result = compute(settings, junction);

    assertEquals(3, result.getNumberOfPhases(junction));
    for (var type : List.of(mainRoad, road, lane)) {
      assertGreenRatioOfApproachesOfType(junction, type, 10.0 / 3 / 25);
    }
  }
}
