package org.goplanit.test.network;

import org.goplanit.test.LayerTestBase;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.network.transport.TransportModelNetworkImpl;
import org.goplanit.network.transport.TransportModelNetworkUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUnsignalisedSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.physical.CompiledRelationIndex;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the intersections memory model: the two kinds of intersection and their ids, member nodes, control type,
 * kinds, approach and internal segments, their green ratios and saturation flow factors and those of movements, the
 * movements of a spatial intersection, turns restricted by the layer's
 * banned turns and the modes those ban, the container's lookup by member node, and that intersections leave the
 * network itself untouched
 *
 * @author markr
 */
public class IntersectionsTest extends LayerTestBase {

  private MacroscopicNetwork network;
  private MacroscopicLinkSegmentType road;
  private IntersectionsImpl intersections;

  /** the approach of the intersection entering on the link segment from node a to node b, one with a signal */
  private IntersectionSignalSegment approachWithSignal(Intersection intersection, int a, int b) {
    return (IntersectionSignalSegment) intersection.getApproach(segment(a, b));
  }

  /** the internal segment of the intersection on the link segment from node a to node b, one with a signal */
  private IntersectionSignalSegment internalWithSignal(SpatialIntersection intersection, int a, int b) {
    return (IntersectionSignalSegment) intersection.getIntersectionInternalSegment(segment(a, b));
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
    road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    intersections = (IntersectionsImpl) layer.getIntersections();
  }

  /** centre node 0 with arms to nodes 1 to 4, node 5 next to the centre and node 6 beyond it, all links two-way */
  private void createJunctionArea() {
    createNodes(7);
    for (int arm = 1; arm <= 4; ++arm) {
      twoWay(0, arm, road);
    }
    twoWay(0, 5, road);
    twoWay(5, 6, road);
  }

  /** a signalised non-spatial junction at node 0, not registered */
  private NonSpatialIntersection junctionAtCentre() {
    return intersections.getFactory().createNonSpatial(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
  }

  /** spatial junction over nodes 0 and 5: approaches from 1 and 6, internal both ways, not registered */
  private SpatialIntersection twoNodeJunction() {
    var junction = spatialJunctionOver(0, 5);
    junction.addNewApproachSegment(segment(1, 0));
    junction.addNewApproachSegment(segment(6, 5));
    junction.addNewInternalSegment(segment(0, 5));
    junction.addNewInternalSegment(segment(5, 0));
    return junction;
  }

  // MEMBER NODES

  @Test
  public void removingMemberNodeWithReferencesRemovesThem() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertTrue(junction.removeMemberNode(nodes.get(5), true));
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
    assertEquals(List.of(segment(1, 0)), junction.getApproachSegments());
    assertTrue(junction.getInternalSegments().isEmpty());
  }

  @Test
  public void removingOnlyMemberNodeKeepsReferences() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertTrue(junction.removeMemberNode(nodes.get(5), false));
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
    assertEquals(2, junction.getApproachSegments().size());
    assertEquals(2, junction.getInternalSegments().size());
  }

  // CONTROL TYPE

  @Test
  public void changingControlTypeKeepsTheRest() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    junction.setControlType(IntersectionControlType.UNSIGNALISED);
    assertEquals(IntersectionControlType.UNSIGNALISED, junction.getControlType());
    assertFalse(intersections.isSignalised(nodes.get(0)));
    assertEquals(List.of(nodes.get(0), nodes.get(5)), junction.getMemberNodes());
    assertEquals(2, junction.getApproachSegments().size());
    assertEquals(2, junction.getInternalSegments().size());
  }

  // SIGNALS PER APPROACH AND PER INTERNAL SEGMENT

  /**
   * An approach follows the control type unless set otherwise: a driveway inside a signalised junction has no signal
   * of its own, and stays an approach; a signalised approach at an unsignalised junction has one. Only an approach can
   * have a signal of its own
   */
  @Test
  public void approachSignalFollowsTheControlTypeUnlessSet() {
    createJunctionArea();
    var signalised = junctionAtCentre();
    for (int arm = 1; arm <= 4; ++arm) {
      signalised.addNewApproachSegment(segment(arm, 0));
    }

    assertNotNull(signalised.swapApproachToNewSignalOrUnsignalised(segment(4, 0), false));
    for (int arm = 1; arm <= 3; ++arm) {
      assertTrue(signalised.hasOwnSignal(segment(arm, 0)));
    }
    assertFalse(signalised.hasOwnSignal(segment(4, 0)));
    assertEquals(4, signalised.getApproachSegments().size());

    var unsignalised = intersections.getFactory().createNonSpatial(
        nodes.get(0), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    for (int arm = 1; arm <= 3; ++arm) {
      unsignalised.addNewApproachSegment(segment(arm, 0));
    }

    assertNotNull(unsignalised.swapApproachToNewSignalOrUnsignalised(segment(3, 0), true));
    assertTrue(unsignalised.hasOwnSignal(segment(3, 0)));
    assertFalse(unsignalised.hasOwnSignal(segment(1, 0)));
    assertFalse(unsignalised.hasOwnSignal(segment(2, 0)));

    var withoutApproaches = junctionAtCentre();
    assertNull(withoutApproaches.swapApproachToNewSignalOrUnsignalised(segment(0, 1), true));
    assertFalse(withoutApproaches.hasOwnSignal(segment(0, 1)));
  }

  @Test
  public void changingControlTypeKeepsOwnSignals() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproachSegment(segment(1, 0));
    junction.addNewApproachSegment(segment(2, 0));
    junction.swapApproachToNewSignalOrUnsignalised(segment(2, 0), false);

    junction.setControlType(IntersectionControlType.UNSIGNALISED);
    assertTrue(junction.hasOwnSignal(segment(1, 0)));
    assertFalse(junction.hasOwnSignal(segment(2, 0)));
  }

  /** Added again after removal, an approach follows the control type */
  @Test
  public void removingApproachRemovesItsOwnSignal() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproachSegment(segment(1, 0));
    junction.swapApproachToNewSignalOrUnsignalised(segment(1, 0), false);

    junction.removeApproachSegment(segment(1, 0));
    junction.addNewApproachSegment(segment(1, 0));
    assertTrue(junction.hasOwnSignal(segment(1, 0)));
  }

  /**
   * An internal segment given a signal stays internal, and is listed with a signal, once. A
   * segment between members given a signal is added as internal too, while one with an end outside is refused
   */
  @Test
  public void internalSegmentWithSignalIsAlsoInternal() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertTrue(junction.addNewInternalSegmentWithSignal(segment(5, 0)));
    assertEquals(List.of(segment(5, 0)), junction.getInternalSegmentsWithSignal());
    assertEquals(2, junction.getInternalSegments().size());
    assertFalse(junction.addNewInternalSegmentWithSignal(segment(5, 0)));

    var bare = spatialJunctionOver(0, 5);
    assertTrue(bare.addNewInternalSegmentWithSignal(segment(0, 5)));
    assertEquals(List.of(segment(0, 5)), bare.getInternalSegments());
    assertEquals(List.of(segment(0, 5)), bare.getInternalSegmentsWithSignal());
    assertFalse(bare.addNewInternalSegmentWithSignal(segment(5, 6)));
  }

  /** Added again after removal, an internal segment is plain */
  @Test
  public void removingInternalSegmentRemovesItsSignal() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));

    junction.removeInternalSegment(segment(0, 5));
    junction.addNewInternalSegment(segment(0, 5));
    assertTrue(junction.getInternalSegmentsWithSignal().isEmpty());
  }

  @Test
  public void removingMemberNodeRemovesSignalsOfItsSegments() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    junction.swapApproachToNewSignalOrUnsignalised(segment(6, 5), false);

    junction.removeMemberNode(nodes.get(5), true);
    junction.addMemberNode(nodes.get(5));
    junction.addNewApproachSegment(segment(6, 5));
    junction.addNewInternalSegment(segment(0, 5));
    assertTrue(junction.getInternalSegmentsWithSignal().isEmpty());
    assertTrue(junction.hasOwnSignal(segment(6, 5)));
  }

  /** A copy, deep or shallow, keeps the signals, and changing them on the copy leaves the original as it was */
  @Test
  public void copyOfIntersectionKeepsSignals() {
    createJunctionArea();
    var original = twoNodeJunction();
    original.swapApproachToNewSignalOrUnsignalised(segment(1, 0), false);
    original.addNewInternalSegmentWithSignal(segment(0, 5));

    for (var copy : List.of(original.deepClone(), original.shallowClone())) {
      assertFalse(copy.hasOwnSignal(segment(1, 0)));
      assertEquals(List.of(segment(0, 5)), copy.getInternalSegmentsWithSignal());

      copy.swapApproachToNewSignalOrUnsignalised(segment(1, 0), true);
      copy.removeInternalSegment(segment(0, 5));
    }

    assertFalse(original.hasOwnSignal(segment(1, 0)));
    assertEquals(List.of(segment(0, 5)), original.getInternalSegmentsWithSignal());
  }

  // GREEN RATIOS AND SATURATION FLOW FACTORS

  /**
   * Approaches are held in the order added, each found by the link segment it enters on; another segment has none. At
   * a signalised intersection an approach added has a signal, at an unsignalised one it has none
   */
  @Test
  public void approachesAreFoundByTheirLinkSegmentInTheOrderAdded() {
    createJunctionArea();
    var junction = junctionAtCentre();
    var fromTwo = junction.addNewApproach(segment(2, 0));
    var fromOne = junction.addNewApproach(segment(1, 0));

    assertEquals(List.of(fromTwo, fromOne), junction.getApproaches());
    assertSame(fromOne, junction.getApproach(segment(1, 0)));
    assertSame(segment(1, 0), fromOne.getLinkSegment());
    assertEquals(List.of(segment(2, 0), segment(1, 0)), junction.getApproachSegments());
    assertNull(junction.getApproach(segment(0, 1)));
    assertNull(junction.addNewApproach(segment(1, 0)));
    assertTrue(fromOne instanceof IntersectionSignalSegment);

    var unsignalised = intersections.getFactory().createNonSpatial(
        nodes.get(0), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    assertTrue(unsignalised.addNewApproach(segment(1, 0)) instanceof IntersectionUnsignalisedSegment);
  }

  /** An approach added with or without a signal of its own is of that kind whatever the control type: one without a
   * signal at a signalised junction, one with a signal at an unsignalised one */
  @Test
  public void approachAddedWithOrWithoutSignalWhateverTheControlType() {
    createJunctionArea();
    var signalised = junctionAtCentre();
    var unsignalised = intersections.getFactory().createNonSpatial(
        nodes.get(0), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);

    assertTrue(signalised.addNewApproach(segment(1, 0), false) instanceof IntersectionUnsignalisedSegment);
    assertFalse(signalised.hasOwnSignal(segment(1, 0)));
    assertTrue(unsignalised.addNewApproach(segment(1, 0), true) instanceof IntersectionSignalSegment);
    assertTrue(unsignalised.hasOwnSignal(segment(1, 0)));
    assertNull(signalised.addNewApproach(segment(1, 0), true));
  }

  /** An approach without values has a green ratio, saturation flow factor and effective capacity factor of 1 */
  @Test
  public void approachWithoutValuesHasOne() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproach(segment(1, 0));
    var approach = approachWithSignal(junction, 1, 0);

    assertEquals(1, approach.getGreenRatio());
    assertEquals(1, approach.getSaturationFlowFactor());
    assertEquals(1, approach.getEffectiveCapacityFactor());
    assertNull(approach.getGreenRatioOrigin());
    assertNull(approach.getSaturationFlowFactorOrigin());
  }

  /**
   * Each value is set on its own, with how it got its value: 0.44 and 0.95 give an effective capacity factor of 0.418,
   * and another approach keeps 1. A removed value is 1 again, and the other value stays
   */
  @Test
  public void approachValuesAreSetWithTheirOriginAndRemoved() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproach(segment(1, 0));
    junction.addNewApproach(segment(2, 0));
    var approach = approachWithSignal(junction, 1, 0);

    assertTrue(approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED));
    assertTrue(approach.setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY));
    assertEquals(0.44, approach.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, approach.getGreenRatioOrigin());
    assertEquals(0.95, approach.getSaturationFlowFactor());
    assertEquals(CapacityFactorOrigin.SET_DIRECTLY, approach.getSaturationFlowFactorOrigin());
    assertEquals(0.418, approach.getEffectiveCapacityFactor(), 1e-9);
    assertEquals(1, junction.getApproach(segment(2, 0)).getEffectiveCapacityFactor());

    assertTrue(approach.removeGreenRatio());
    assertFalse(approach.removeGreenRatio());
    assertEquals(1, approach.getGreenRatio());
    assertNull(approach.getGreenRatioOrigin());
    assertEquals(0.95, approach.getSaturationFlowFactor());
  }

  /** A value outside 0 to 1 is refused and leaves the values as they were */
  @Test
  public void valueOutsideRangeIsRefused() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproach(segment(1, 0));
    var approach = approachWithSignal(junction, 1, 0);
    approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);

    assertFalse(approach.setGreenRatio(1.2, CapacityFactorOrigin.COMPUTED));
    assertFalse(approach.setGreenRatio(-0.1, CapacityFactorOrigin.COMPUTED));
    assertFalse(approach.setSaturationFlowFactor(Double.NaN, CapacityFactorOrigin.SET_DIRECTLY));
    assertEquals(0.44, approach.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, approach.getGreenRatioOrigin());
    assertEquals(1, approach.getSaturationFlowFactor());
  }

  /**
   * An approach losing its signal is replaced, at its place, by one without a signal that keeps its saturation flow
   * factor, which is then its effective capacity factor, and has no green ratio. Given its signal back, it has a green
   * ratio of 1 again
   */
  @Test
  public void approachLosingItsSignalKeepsItsPlaceAndSaturationFlowFactor() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproach(segment(1, 0));
    junction.addNewApproach(segment(2, 0));
    var approach = approachWithSignal(junction, 1, 0);
    approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    approach.setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY);

    assertNotNull(junction.swapApproachToNewSignalOrUnsignalised(segment(1, 0), false));
    var withoutSignal = junction.getApproaches().get(0);
    assertTrue(withoutSignal instanceof IntersectionUnsignalisedSegment);
    assertSame(segment(1, 0), withoutSignal.getLinkSegment());
    assertEquals(0.95, withoutSignal.getSaturationFlowFactor());
    assertEquals(CapacityFactorOrigin.SET_DIRECTLY, withoutSignal.getSaturationFlowFactorOrigin());
    assertEquals(0.95, withoutSignal.getEffectiveCapacityFactor());
    assertFalse(junction.hasOwnSignal(segment(1, 0)));

    junction.swapApproachToNewSignalOrUnsignalised(segment(1, 0), true);
    var withSignalAgain = approachWithSignal(junction, 1, 0);
    assertSame(withSignalAgain, junction.getApproaches().get(0));
    assertEquals(1, withSignalAgain.getGreenRatio());
    assertEquals(0.95, withSignalAgain.getSaturationFlowFactor());
  }

  /**
   * An internal segment with a signal has values of its own, 1 until set. A plain internal segment has no signal, and
   * a segment that is not internal has no internal segment
   */
  @Test
  public void internalSegmentWithSignalHasValuesOfItsOwn() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    var withSignal = internalWithSignal(junction, 0, 5);

    assertEquals(1, withSignal.getGreenRatio());
    assertTrue(withSignal.setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY));
    assertTrue(withSignal.setSaturationFlowFactor(0.9, CapacityFactorOrigin.SET_DIRECTLY));
    assertEquals(0.54, withSignal.getEffectiveCapacityFactor(), 1e-9);
    assertTrue(junction.getIntersectionInternalSegment(segment(5, 0)) instanceof IntersectionUnsignalisedSegment);
    assertNull(junction.getIntersectionInternalSegment(segment(1, 0)));
  }

  /** A plain internal segment given a signal is replaced, at its place, by one with a signal keeping its saturation
   * flow factor */
  @Test
  public void plainInternalSegmentGivenASignalKeepsItsPlaceAndSaturationFlowFactor() {
    createJunctionArea();
    var junction = twoNodeJunction();
    var plain = junction.getIntersectionInternalSegment(segment(5, 0));
    plain.setSaturationFlowFactor(0.9, CapacityFactorOrigin.SET_DIRECTLY);

    assertTrue(junction.addNewInternalSegmentWithSignal(segment(5, 0)));
    assertFalse(junction.addNewInternalSegmentWithSignal(segment(5, 0)));
    var withSignal = internalWithSignal(junction, 5, 0);
    assertSame(withSignal, junction.getIntersectionInternalSegments().get(1));
    assertEquals(0.9, withSignal.getSaturationFlowFactor());
    assertEquals(List.of(segment(5, 0)), junction.getInternalSegmentsWithSignal());
  }

  /**
   * A movement's own value is used in place of its approach's, each value on its own. On an approach of 0.44 and 0.9, a
   * right turn with its own 0.15 uses 0.15 and 0.9, a movement without values uses 0.44 and 0.9, and a movement set to
   * 1 uses 1. The movements of an approach are those starting on it
   */
  @Test
  public void movementValueIsUsedInPlaceOfItsApproachValue() {
    createJunctionArea();
    var junction = twoNodeJunction();
    var movements = layer.getSpatialMovements().getFactory();
    var rightTurn = movements.create(segment(1, 0), segment(0, 2), 0);
    var withoutValues = movements.create(segment(1, 0), segment(0, 4), 0);
    var setToOne = movements.create(segment(1, 0), segment(0, 3), 0);
    var fromOtherApproach = movements.create(segment(6, 5), segment(0, 2), 0.01);
    List.of(rightTurn, withoutValues, setToOne, fromOtherApproach).forEach(junction::addMovement);
    var approach = approachWithSignal(junction, 1, 0);
    approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    approach.setSaturationFlowFactor(0.9, CapacityFactorOrigin.SET_DIRECTLY);

    assertTrue(rightTurn.setGreenRatio(0.15));
    assertTrue(setToOne.setGreenRatio(1));

    assertEquals(0.15, junction.getGreenRatioUsedBy(rightTurn));
    assertEquals(0.9, junction.getSaturationFlowFactorUsedBy(rightTurn));
    assertNull(withoutValues.getGreenRatio());
    assertEquals(0.44, junction.getGreenRatioUsedBy(withoutValues));
    assertEquals(0.9, junction.getSaturationFlowFactorUsedBy(withoutValues));
    assertEquals(1, junction.getGreenRatioUsedBy(setToOne));
    assertEquals(List.of(rightTurn, withoutValues, setToOne), junction.getMovementsFrom(approach));
  }

  /** Once an approach has no signal of its own, its movements lose their green ratios as the approach does, and keep
   * their saturation flow factors; the movements of other approaches keep their green ratios */
  @Test
  public void approachWithoutSignalRemovesGreenRatioOfItsMovements() {
    createJunctionArea();
    var junction = twoNodeJunction();
    var movements = layer.getSpatialMovements().getFactory();
    var rightTurn = movements.create(segment(1, 0), segment(0, 2), 0);
    var fromOtherApproach = movements.create(segment(6, 5), segment(0, 2), 0.01);
    List.of(rightTurn, fromOtherApproach).forEach(junction::addMovement);
    rightTurn.setGreenRatio(0.15);
    rightTurn.setSaturationFlowFactor(0.8);
    fromOtherApproach.setGreenRatio(0.3);

    junction.swapApproachToNewSignalOrUnsignalised(segment(1, 0), false);

    assertNull(rightTurn.getGreenRatio());
    assertEquals(0.8, rightTurn.getSaturationFlowFactor());
    assertEquals(0.3, fromOtherApproach.getGreenRatio());
  }

  /** A movement refuses a value outside 0 to 1, keeping what it had, and uses its approach's value again once its own
   * is removed */
  @Test
  public void movementValueOutsideRangeIsRefusedAndRemovable() {
    createJunctionArea();
    var movement = layer.getSpatialMovements().getFactory().create(segment(1, 0), segment(0, 2), 0);
    movement.setGreenRatio(0.15);

    assertFalse(movement.setGreenRatio(1.5));
    assertFalse(movement.setSaturationFlowFactor(-1));
    assertEquals(0.15, movement.getGreenRatio());
    assertNull(movement.getSaturationFlowFactor());

    assertTrue(movement.removeGreenRatio());
    assertFalse(movement.removeGreenRatio());
    assertNull(movement.getGreenRatio());
  }

  /** Added again after removal, an approach has no values */
  @Test
  public void removingApproachRemovesItsValues() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addNewApproach(segment(1, 0));
    var approach = approachWithSignal(junction, 1, 0);
    approach.setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    approach.setSaturationFlowFactor(0.95, CapacityFactorOrigin.SET_DIRECTLY);

    junction.removeApproachSegment(segment(1, 0));
    assertNull(junction.getApproach(segment(1, 0)));
    var addedAgain = junction.addNewApproach(segment(1, 0));
    assertEquals(1, addedAgain.getEffectiveCapacityFactor());
    assertNull(addedAgain.getSaturationFlowFactorOrigin());
  }

  /** Added again after removal, an internal segment with a signal has no values */
  @Test
  public void removingInternalSegmentRemovesItsValues() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    internalWithSignal(junction, 0, 5).setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);

    junction.removeInternalSegment(segment(0, 5));
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    assertEquals(1, internalWithSignal(junction, 0, 5).getGreenRatio());
  }

  /** Removing a member node with its references removes the approaches and internal segments touching it, with their
   * values, and keeps the others */
  @Test
  public void removingMemberNodeRemovesValuesOfItsSegments() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    approachWithSignal(junction, 6, 5).setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    approachWithSignal(junction, 1, 0).setGreenRatio(0.3, CapacityFactorOrigin.COMPUTED);
    internalWithSignal(junction, 0, 5).setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);

    junction.removeMemberNode(nodes.get(5), true);
    junction.addMemberNode(nodes.get(5));
    junction.addNewApproachSegment(segment(6, 5));
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    assertEquals(1, approachWithSignal(junction, 6, 5).getGreenRatio());
    assertEquals(1, internalWithSignal(junction, 0, 5).getGreenRatio());
    assertEquals(0.3, approachWithSignal(junction, 1, 0).getGreenRatio());
  }

  /** A copy, deep or shallow, holds its own approaches and internal segments of the same kinds with the same values, so
   * changing them on the copy leaves the original as it was */
  @Test
  public void copyOfIntersectionKeepsValues() {
    createJunctionArea();
    var original = twoNodeJunction();
    original.addNewInternalSegmentWithSignal(segment(0, 5));
    approachWithSignal(original, 1, 0).setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    internalWithSignal(original, 0, 5).setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);

    for (var copy : List.of(original.deepClone(), original.shallowClone())) {
      assertNotSame(original.getApproach(segment(1, 0)), copy.getApproach(segment(1, 0)));
      assertEquals(0.44, approachWithSignal(copy, 1, 0).getGreenRatio());
      assertEquals(CapacityFactorOrigin.COMPUTED, approachWithSignal(copy, 1, 0).getGreenRatioOrigin());
      assertEquals(0.6, internalWithSignal(copy, 0, 5).getGreenRatio());
      assertTrue(copy.getIntersectionInternalSegment(segment(5, 0)) instanceof IntersectionUnsignalisedSegment);

      approachWithSignal(copy, 1, 0).setGreenRatio(0.2, CapacityFactorOrigin.SET_DIRECTLY);
      internalWithSignal(copy, 0, 5).removeGreenRatio();
    }

    assertEquals(0.44, approachWithSignal(original, 1, 0).getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, approachWithSignal(original, 1, 0).getGreenRatioOrigin());
    assertEquals(0.6, internalWithSignal(original, 0, 5).getGreenRatio());
  }

  /** A clone, deep or shallow, of an approach or internal segment is a new object of the same kind, on the same link
   * segment and with the same values, so changing the clone leaves the original as it was */
  @Test
  public void cloneOfIntersectionSegmentKeepsKindAndValues() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    var withSignal = internalWithSignal(junction, 0, 5);
    withSignal.setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);
    withSignal.setSaturationFlowFactor(0.9, CapacityFactorOrigin.COMPUTED);
    var unsignalised = junction.getIntersectionInternalSegment(segment(5, 0));
    unsignalised.setSaturationFlowFactor(0.8, CapacityFactorOrigin.SET_DIRECTLY);

    for (var clone : List.of(withSignal.deepClone(), withSignal.shallowClone())) {
      assertNotSame(withSignal, clone);
      assertSame(segment(0, 5), clone.getLinkSegment());
      assertEquals(0.6, clone.getGreenRatio());
      assertEquals(0.9, clone.getSaturationFlowFactor());
      assertEquals(CapacityFactorOrigin.COMPUTED, clone.getSaturationFlowFactorOrigin());
      clone.removeGreenRatio();
    }
    for (var clone : List.of(unsignalised.deepClone(), unsignalised.shallowClone())) {
      assertNotSame(unsignalised, clone);
      assertTrue(clone instanceof IntersectionUnsignalisedSegment);
      assertEquals(0.8, clone.getSaturationFlowFactor());
      clone.removeSaturationFlowFactor();
    }

    assertEquals(0.6, withSignal.getGreenRatio());
    assertEquals(0.8, unsignalised.getSaturationFlowFactor());
  }

  /** A deep copy of the layer holds the values on its own approaches, internal segments with a signal and movements,
   * which refer to its own link segments */
  @Test
  public void copyOfLayerKeepsValuesOnItsOwnEntities() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    approachWithSignal(junction, 1, 0).setGreenRatio(0.44, CapacityFactorOrigin.COMPUTED);
    internalWithSignal(junction, 0, 5).setGreenRatio(0.6, CapacityFactorOrigin.SET_DIRECTLY);
    var movement = layer.getSpatialMovements().getFactory().registerNew(segment(1, 0), segment(5, 6), 0.01);
    movement.setGreenRatio(0.15);
    junction.addMovement(movement);
    intersections.getFactory().register(junction);

    var copy = layer.deepClone();

    var copied = (SpatialIntersection) copy.getIntersections().getFirst();
    var copiedApproach = (IntersectionSignalSegment) copied.getApproach(
        copy.getLinkSegments().get(segment(1, 0).getId()));
    assertEquals(0.44, copiedApproach.getGreenRatio());
    assertEquals(CapacityFactorOrigin.COMPUTED, copiedApproach.getGreenRatioOrigin());
    assertNull(copied.getApproach(segment(1, 0)));
    var copiedWithSignal = (IntersectionSignalSegment) copied.getIntersectionInternalSegment(
        copy.getLinkSegments().get(segment(0, 5).getId()));
    assertEquals(0.6, copiedWithSignal.getGreenRatio());
    var copiedMovement = copy.getSpatialMovements().get(movement.getId());
    assertNotSame(movement, copiedMovement);
    assertEquals(0.15, copiedMovement.getGreenRatio());
    assertEquals(0.44, approachWithSignal(junction, 1, 0).getGreenRatio());
  }

  // TWO KINDS OF INTERSECTION

  /**
   * Two non-spatial intersections and then a spatial one: the intersection ids run from 0 to 2 over both kinds, and
   * each kind numbers its own from 0. An external id set before registering is kept, and the id mapper maps an
   * intersection by its xml id, its external id and its id
   */
  @Test
  public void eachKindOfIntersectionHasItsOwnIdsAndIsMappedByEachIdType() {
    createJunctionArea();
    var first = intersections.getFactory().createNonSpatial(
        nodes.get(3), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    first.setExternalId("osm:123");
    assertEquals("osm:123", intersections.getFactory().register(first).getExternalId());
    var second = intersections.getFactory().registerNewNonSpatial(
        nodes.get(4), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    var spatial = twoNodeJunction();
    intersections.getFactory().register(spatial);

    assertEquals(List.of(0L, 1L, 2L), List.of(first.getId(), second.getId(), spatial.getId()));
    assertEquals(List.of(0L, 1L), List.of(first.getNonSpatialIntersectionId(), second.getNonSpatialIntersectionId()));
    assertEquals(0, spatial.getSpatialIntersectionId());

    second.setXmlId("x1");
    second.setExternalId("osm:1");
    assertEquals("x1", new NetworkIdMapper(IdMapperType.XML).getIntersectionIdMapper().apply(second));
    assertEquals("osm:1", new NetworkIdMapper(IdMapperType.EXTERNAL_ID).getIntersectionIdMapper().apply(second));
    assertEquals(String.valueOf(second.getId()),
        new NetworkIdMapper(IdMapperType.ID).getIntersectionIdMapper().apply(second));
  }

  /** Recreating the layer's ids makes the ids of both kinds contiguous again, the intersection ids over both kinds and
   * the ids of each kind on their own */
  @Test
  public void recreatingLayerIdsRenumbersEachKindOfIntersection() {
    createJunctionArea();
    var removedNonSpatial = intersections.getFactory().registerNewNonSpatial(
        nodes.get(3), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    var keptNonSpatial = intersections.getFactory().registerNewNonSpatial(
        nodes.get(4), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    var removedSpatial = intersections.getFactory().registerNewSpatial(List.of(nodes.get(1), nodes.get(2)),
        IntersectionControlType.SIGNALISED, List.of(IntersectionType.JUNCTION));
    var keptSpatial = twoNodeJunction();
    intersections.getFactory().register(keptSpatial);
    intersections.remove(removedNonSpatial);                     // leaves gaps in the ids of both kinds
    intersections.remove(removedSpatial);

    layer.recreateManagedIds(IdGroupingToken.collectGlobalToken());

    assertEquals(Set.of(0L, 1L), Set.copyOf(List.of(keptNonSpatial.getId(), keptSpatial.getId())));
    assertEquals(0, keptNonSpatial.getNonSpatialIntersectionId());
    assertEquals(0, keptSpatial.getSpatialIntersectionId());
  }

  // KINDS AND APPROACHES

  /**
   * A non-spatial intersection takes each kind once. It refuses an approach that does not enter it, one starting at its
   * node or not ending there, and takes an approach that enters it once
   */
  @Test
  public void nonSpatialIntersectionTakesKindsAndApproachesOnce() {
    createJunctionArea();
    var junction = junctionAtCentre();

    assertTrue(junction.addType(IntersectionType.CROSSING));
    assertFalse(junction.addType(IntersectionType.JUNCTION));
    assertEquals(EnumSet.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), junction.getTypes());

    assertFalse(junction.addNewApproachSegment(segment(0, 1)));   // starts at a member
    assertFalse(junction.addNewApproachSegment(segment(5, 6)));   // does not end at a member
    assertTrue(junction.getApproachSegments().isEmpty());

    assertTrue(junction.addNewApproachSegment(segment(1, 0)));
    assertFalse(junction.addNewApproachSegment(segment(1, 0)));
    assertEquals(1, junction.getApproachSegments().size());
    assertSame(segment(1, 0), junction.getApproachSegment(segment(1, 0).getId()));
  }

  // TURNS

  /** Every banned turn of the layer names the modes it bans: one registered without modes bans every mode, one
   * registered for buses only bans buses and not cars */
  @Test
  public void bannedTurnBansEveryModeUnlessItNamesModes() {
    createJunctionArea();
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);

    var everyMode = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));
    var busOnly = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 3), List.of(bus));

    assertTrue(everyMode instanceof MacroscopicBannedTurn);
    var everyModeBan = (MacroscopicBannedTurn) everyMode;
    assertFalse(everyModeBan.hasExplicitlyBannedModes());
    assertTrue(everyModeBan.isModeBanned(car));
    assertTrue(everyModeBan.isModeBanned(bus));
    assertEquals(Set.of(bus), busOnly.getExplicitlyBannedModes());
    assertTrue(busOnly.isModeBanned(bus));
    assertFalse(busOnly.isModeBanned(car));
    assertEquals(Set.of(bus), busOnly.getBannedModesFrom(List.of(car, bus)));
    assertSame(segment(1, 0), busOnly.getLinkSegmentFrom());
    assertSame(segment(0, 3), busOnly.getLinkSegmentTo());
  }

  /** A banned turn keeps the last mode it names, since one that names no modes would ban every mode */
  @Test
  public void bannedTurnKeepsItsLastNamedMode() {
    createJunctionArea();
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2), List.of(car, bus));

    assertTrue(ban.removeExplicitlyBannedMode(car));
    assertFalse(ban.removeExplicitlyBannedMode(bus));
    assertEquals(Set.of(bus), ban.getExplicitlyBannedModes());
    assertTrue(ban.addExplicitlyBannedMode(car));
    assertFalse(ban.addExplicitlyBannedMode(car));
  }

  /** The compiled turns of the sLTM leave out every banned turn whatever modes it names, so a turn banned for buses
   * only is left out for cars as well, while the other turns are kept */
  @Test
  public void busOnlyBannedTurnIsLeftOutOfTheCompiledTurns() {
    createJunctionArea();
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2), List.of(bus));
    var zoning = new Zoning(IdGroupingToken.collectGlobalToken(), layer.getLayerIdGroupingToken());

    var compiledTurns =
        TransportModelNetworkUtils.createCompiledTurnIdMapping(new TransportModelNetworkImpl(network, zoning));

    assertEquals(CompiledRelationIndex.BANNED, compiledTurns.get(segment(1, 0).getId(), segment(0, 2).getId()));
    assertNotEquals(CompiledRelationIndex.BANNED, compiledTurns.get(segment(1, 0).getId(), segment(0, 3).getId()));
  }

  // INTERNAL SEGMENTS

  /**
   * A spatial intersection takes only what fits it: a segment between its members is internal only when listed, an
   * internal segment with an end outside is refused, and so is the node an approach starts at as a member. An exit
   * starts at a member and ends outside, so neither an approach nor a segment between members is one
   */
  @Test
  public void spatialIntersectionTakesOnlySegmentsAndNodesThatFit() {
    createJunctionArea();
    var junction = spatialJunctionOver(0, 5);

    assertNull(junction.getInternalSegment(segment(0, 5).getId()));
    assertNull(junction.getApproachSegment(segment(0, 5).getId()));
    assertFalse(junction.addNewInternalSegment(segment(0, 1)));
    assertTrue(junction.getInternalSegments().isEmpty());

    junction.addNewApproachSegment(segment(1, 0));
    assertFalse(junction.addMemberNode(nodes.get(1)));
    assertEquals(List.of(nodes.get(0), nodes.get(5)), junction.getMemberNodes());

    assertTrue(junction.isExitSegment(segment(0, 1)));
    assertTrue(junction.isExitSegment(segment(5, 6)));
    assertFalse(junction.isExitSegment(segment(1, 0)));   // an approach
    assertFalse(junction.isExitSegment(segment(0, 5)));   // between members
  }

  /** Removing a member node, approach or internal segment the intersection does not hold changes nothing, removing
   * one it holds removes only that one */
  @Test
  public void onlyHeldReferencesAreRemoved() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertFalse(junction.removeMemberNode(nodes.get(1), true));
    assertFalse(junction.removeApproachSegment(segment(2, 0)));
    assertFalse(junction.removeInternalSegment(segment(0, 1)));
    assertEquals(List.of(nodes.get(0), nodes.get(5)), junction.getMemberNodes());
    assertEquals(List.of(segment(1, 0), segment(6, 5)), junction.getApproachSegments());
    assertEquals(List.of(segment(0, 5), segment(5, 0)), junction.getInternalSegments());

    assertTrue(junction.removeInternalSegment(segment(5, 0)));
    assertEquals(List.of(segment(0, 5)), junction.getInternalSegments());
    assertEquals(2, junction.getApproachSegments().size());
  }

  // MOVEMENTS OF A SPATIAL INTERSECTION

  /** A movement of a spatial intersection starts on one of its approaches: one starting elsewhere is refused, and a
   * movement is held once */
  @Test
  public void movementMustStartOnAnApproach() {
    createJunctionArea();
    var junction = twoNodeJunction();
    var movements = layer.getSpatialMovements().getFactory();
    var fromApproach = movements.create(segment(1, 0), segment(0, 2), 0);
    var fromElsewhere = movements.create(segment(2, 0), segment(0, 3), 0);

    assertTrue(junction.addMovement(fromApproach));
    assertFalse(junction.addMovement(fromApproach));
    assertFalse(junction.addMovement(fromElsewhere));
    assertEquals(List.of(fromApproach), junction.getMovements());

    assertTrue(junction.removeMovement(fromApproach));
    assertFalse(junction.removeMovement(fromApproach));
    assertTrue(junction.getMovements().isEmpty());
  }

  // LOOKUP BY MEMBER NODE

  /**
   * A registered spatial intersection is found by each of its members and not by another node, and registering it
   * leaves the network unchanged. A banned turn from one of its approaches is found by approach segment and by
   * intersection, while one from elsewhere is not, and the intersection keeps its approaches
   */
  @Test
  public void registeredIntersectionIsFoundByEachMemberAndByTheBansFromItsApproaches() {
    createJunctionArea();
    var segmentsBefore = new ArrayList<>(layer.getLinkSegments().toCollection());
    int linksBefore = layer.getLinks().size();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));
    assertSame(junction, intersections.getByMemberNode(nodes.get(5)));
    assertNull(intersections.getByMemberNode(nodes.get(1)));

    assertEquals(linksBefore, layer.getLinks().size());
    assertEquals(segmentsBefore.size(), layer.getLinkSegments().size());
    assertEquals(1, layer.getLinkSegmentTypes().size());
    for (var linkSegment : segmentsBefore) {
      assertSame(linkSegment, layer.getLinkSegments().get(linkSegment.getId()));
      assertSame(road, linkSegment.getLinkSegmentType());
    }

    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));
    layer.getBannedTurns().getFactory().registerNew(segment(2, 0), segment(0, 3));   // not from an approach

    var bansByApproachSegment =
        IntersectionUtils.findBannedTurnsByApproachSegment(intersections, layer.getBannedTurns());
    assertEquals(1, bansByApproachSegment.size());
    assertEquals(List.of(ban), bansByApproachSegment.get(segment(1, 0)));

    var bansByIntersection =
        IntersectionUtils.findBannedTurnsByIntersection(intersections, layer.getBannedTurns());
    assertEquals(List.of(ban), bansByIntersection.get(junction));
    assertEquals(2, junction.getApproachSegments().size());
  }

  @Test
  public void memberAddedAfterRegistrationIsNotFound() {
    createJunctionArea();
    var junction = intersections.getFactory().registerNewSpatial(List.of(nodes.get(0), nodes.get(5)),
        IntersectionControlType.SIGNALISED, List.of(IntersectionType.JUNCTION));

    assertTrue(junction.addMemberNode(nodes.get(6)));
    assertNull(intersections.getByMemberNode(nodes.get(6)));
  }

  @Test
  public void registeredIntersectionIsFoundBySegmentUntilRemoved() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    assertSame(junction, intersections.getBySegment(segment(1, 0)));   // approach
    assertSame(junction, intersections.getBySegment(segment(0, 5)));   // internal
    assertNull(intersections.getBySegment(segment(0, 1)));             // leaves it

    intersections.remove(junction);
    assertNull(intersections.getBySegment(segment(1, 0)));
  }

  @Test
  public void removedIntersectionIsNoLongerFound() {
    createJunctionArea();
    var junction = intersections.getFactory().registerNewNonSpatial(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    intersections.remove(junction);
    assertNull(intersections.getByMemberNode(nodes.get(0)));
    assertFalse(intersections.isSignalised(nodes.get(0)));
  }

  // LAYER

  /**
   * A new layer has no intersections. Once a signalised junction is registered at node 0 with the approach from node
   * 1, the layer and its intersections report node 0 as signalised and node 1 not. A banned turn at node 0 belongs to
   * the node: it is found through the segments meeting there, and no movement is held for it. A second intersection
   * over nodes 1 and 0 leaves node 0 reported as one in two intersections
   */
  @Test
  public void layerReportsItsRegisteredIntersections() {
    createJunctionArea();

    assertTrue(layer.getIntersections().isEmpty());
    assertFalse(layer.hasIntersections());
    assertFalse(layer.isSignalised(nodes.get(0)));

    var junction = junctionAtCentre();
    junction.addNewApproachSegment(segment(1, 0));
    intersections.getFactory().register(junction);

    assertTrue(layer.hasIntersections());
    assertTrue(layer.isSignalised(nodes.get(0)));
    assertFalse(layer.isSignalised(nodes.get(1)));
    assertTrue(intersections.isSignalised(nodes.get(0)));
    assertFalse(intersections.isSignalised(nodes.get(1)));

    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2));

    assertEquals(List.of(ban), layer.getBannedTurns().getBySegment(segment(1, 0)));
    assertSame(junction.getNode(), ban.getCentreVertex());
    assertTrue(layer.getSpatialMovements().isEmpty());

    var second = intersections.getFactory().registerNewSpatial(List.of(nodes.get(1), nodes.get(0)),
        IntersectionControlType.UNSIGNALISED, List.of(IntersectionType.JUNCTION));

    var shared = IntersectionUtils.findNodesInMoreThanOneIntersection(intersections);
    assertEquals(1, shared.size());
    assertTrue(shared.get(nodes.get(0)).contains(junction) && shared.get(nodes.get(0)).contains(second));
  }

  @Test
  public void recreatingLayerIdsKeepsReferencesAndLookups() {
    createJunctionArea();
    var gone = intersections.getFactory().registerNewNonSpatial(
        nodes.get(3), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);
    intersections.remove(gone);                                   // leaves a gap in the ids
    var approachSegment = segment(1, 0);

    layer.recreateManagedIds(IdGroupingToken.collectGlobalToken());

    assertEquals(0, junction.getId());                            // contiguous again
    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));
    assertSame(junction, intersections.getBySegment(approachSegment));
    assertSame(approachSegment, junction.getApproachSegment(approachSegment.getId()));
    assertTrue(junction.removeApproachSegment(approachSegment));
  }

  @Test
  public void resettingLayerEmptiesIntersections() {
    createJunctionArea();
    intersections.getFactory().registerNewNonSpatial(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    layer.resetChildManagedIdEntities();

    assertTrue(layer.getIntersections().isEmpty());
    assertNull(intersections.getByMemberNode(nodes.get(0)));
  }

  // COPYING

  /** A copy of an intersection, deep or shallow, holds its own lists of the same nodes and segments, so changing the
   * copy leaves the original as it was */
  @Test
  public void copyOfIntersectionHasItsOwnReferences() {
    createJunctionArea();
    var original = twoNodeJunction();

    for (var copy : List.of(original.deepClone(), original.shallowClone())) {
      assertNotSame(original, copy);
      assertEquals(original.getId(), copy.getId());
      assertSame(nodes.get(5), copy.getMemberNode(nodes.get(5).getId()));
      assertSame(segment(6, 5), copy.getApproachSegment(segment(6, 5).getId()));
      assertEquals(original.getInternalSegments(), copy.getInternalSegments());

      copy.removeMemberNode(nodes.get(5), true);
      copy.addType(IntersectionType.CROSSING);
      copy.setControlType(IntersectionControlType.UNSIGNALISED);
    }

    assertEquals(List.of(nodes.get(0), nodes.get(5)), original.getMemberNodes());
    assertEquals(2, original.getApproachSegments().size());
    assertEquals(2, original.getInternalSegments().size());
    assertEquals(EnumSet.of(IntersectionType.JUNCTION), original.getTypes());
    assertEquals(IntersectionControlType.SIGNALISED, original.getControlType());
  }

  /** A deep copy of the container hands each original intersection with its copy to the caller, the copy registered
   * under the same id in the copied container */
  @Test
  public void deepCopyOfIntersectionsHandsEachPairToTheMapper() {
    createJunctionArea();
    var junction = intersections.getFactory().register(twoNodeJunction());
    var crossing = intersections.getFactory().registerNewNonSpatial(
        nodes.get(2), IntersectionControlType.SIGNALISED, IntersectionType.CROSSING);
    var copies = new HashMap<Intersection, Intersection>();

    var copied = intersections.deepCloneWithMapping(copies::put);

    assertEquals(2, copies.size());
    for (var original : List.of(junction, crossing)) {
      var copy = copies.get(original);
      assertNotSame(original, copy);
      assertSame(copy, copied.get(original.getId()));
    }
    assertSame(junction, intersections.get(junction.getId()));
  }

  /**
   * A deep copy of the layer refers only to its own entities: its intersection keeps its ids, kinds, control type,
   * external id and signals, and refers to the copy's own nodes and segments; the copy's lookups find it through those
   * and not through the original's. The copy holds copies of the movements and banned turns, with their modes, on its
   * own segments. The original is untouched. A shallow copy of the layer shares the intersections
   */
  @Test
  public void copyOfLayerRefersOnlyToItsOwnEntities() {
    createJunctionArea();
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    var junction = twoNodeJunction();
    junction.addType(IntersectionType.CROSSING);
    junction.setExternalId("osm:123");
    junction.swapApproachToNewSignalOrUnsignalised(segment(1, 0), false);
    junction.addNewInternalSegmentWithSignal(segment(0, 5));
    var movement = layer.getSpatialMovements().getFactory().registerNew(segment(1, 0), segment(5, 6), 0.01);
    movement.banDirectly(List.of(bus));
    junction.addMovement(movement);
    intersections.getFactory().register(junction);
    var ban = layer.getBannedTurns().getFactory().registerNew(segment(1, 0), segment(0, 2), List.of(bus));

    var copy = layer.deepClone();

    assertEquals(1, copy.getIntersections().size());
    assertTrue(copy.getIntersections().getFirst() instanceof SpatialIntersection);
    var copied = (SpatialIntersection) copy.getIntersections().getFirst();
    assertNotSame(junction, copied);
    assertEquals(junction.getSpatialIntersectionId(), copied.getSpatialIntersectionId());
    assertEquals("osm:123", copied.getExternalId());
    assertEquals(junction.getTypes(), copied.getTypes());
    assertEquals(junction.getControlType(), copied.getControlType());

    /* every reference is the copy's own entity, checked by identity since a copy equals its original by id */
    assertEquals(2, copied.getMemberNodes().size());
    for (var node : copied.getMemberNodes()) {
      assertSame(copy.getNodes().get(node.getId()), node);
    }
    var copiedSegments = new ArrayList<>(copied.getApproachSegments());
    copiedSegments.addAll(copied.getInternalSegments());
    assertEquals(4, copiedSegments.size());
    for (var segment : copiedSegments) {
      assertSame(copy.getLinkSegments().get(segment.getId()), segment);
    }

    /* the copy's lookups find the copy through the copy's own entities, and not through the original's */
    var copiedIntersections = (IntersectionsImpl) copy.getIntersections();
    assertSame(copied, copiedIntersections.getByMemberNode(copy.getNodes().get(nodes.get(0).getId())));
    assertSame(copied, copiedIntersections.getBySegment(copy.getLinkSegments().get(segment(1, 0).getId())));
    assertNull(copiedIntersections.getByMemberNode(nodes.get(0)));

    /* the signals are on the copy's own segments */
    var copiedSignalSegments = copied.getInternalSegmentsWithSignal();
    assertEquals(1, copiedSignalSegments.size());
    assertSame(copy.getLinkSegments().get(segment(0, 5).getId()), copiedSignalSegments.get(0));
    assertFalse(copied.hasOwnSignal(copy.getLinkSegments().get(segment(1, 0).getId())));
    assertTrue(copied.hasOwnSignal(copy.getLinkSegments().get(segment(6, 5).getId())));

    /* the movements and banned turns are the copy's own, on its own segments */
    var copiedMovement = copy.getSpatialMovements().get(movement.getId());
    assertNotSame(movement, copiedMovement);
    assertSame(copiedMovement, ((SpatialIntersection) copy.getIntersections().getFirst()).getMovements().get(0));
    assertSame(copy.getLinkSegments().get(segment(1, 0).getId()), copiedMovement.getLinkSegmentFrom());
    assertSame(copy.getLinkSegments().get(segment(5, 6).getId()), copiedMovement.getLinkSegmentTo());
    assertEquals(Set.of(bus), copiedMovement.getExplicitlyDirectlyBannedModes());
    assertEquals(List.of(copiedMovement), copy.getSpatialMovements().getBySegment(copiedMovement.getLinkSegmentTo()));

    var copiedBan = (MacroscopicBannedTurn) copy.getBannedTurns().get(ban.getId());
    assertNotSame(ban, copiedBan);
    assertSame(copy.getLinkSegments().get(segment(1, 0).getId()), copiedBan.getSegmentFrom());
    assertSame(copy.getLinkSegments().get(segment(0, 2).getId()), copiedBan.getSegmentTo());
    assertEquals(Set.of(bus), copiedBan.getExplicitlyBannedModes());
    assertEquals(List.of(copiedBan), copy.getBannedTurns().getBySegment(copiedBan.getSegmentFrom()));

    /* the original is untouched */
    assertSame(segment(1, 0), movement.getLinkSegmentFrom());
    assertSame(segment(1, 0), ban.getSegmentFrom());
    assertSame(nodes.get(0), junction.getMemberNodes().get(0));
    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));

    var shallowCopy = layer.shallowClone();

    assertSame(junction, shallowCopy.getIntersections().getFirst());
    assertSame(junction, shallowCopy.getIntersections().getByMemberNode(nodes.get(0)));
  }
}
