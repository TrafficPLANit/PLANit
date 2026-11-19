package org.goplanit.test.graph;

import org.goplanit.test.LayerTestBase;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.ModeAccessRestrictionResult;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.modifier.ModeAccessCleanupModifierResult;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for withdrawing mode access where a mode cannot both reach and leave, rather than removing the
 * infrastructure it sits on.
 *
 * @author markr
 */
public class ModeAccessRestrictionTest extends LayerTestBase {

  private MacroscopicNetwork network;

  private Mode car;
  private Mode bus;
  private Mode pedestrian;

  private MacroscopicLinkSegmentType roadWithFootway;
  private MacroscopicLinkSegmentType busAndWalk;
  private MacroscopicLinkSegmentType walkOnly;
  private MacroscopicLinkSegmentType carOnly;

  /** the safest configuration, a single subnetwork the mode can route across in full */
  private ModeAccessRestrictionResult restrict(Mode mode) {
    return MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, mode, Integer.MAX_VALUE, Integer.MAX_VALUE, true, Connectivity.STRONG, null);
  }

  /** run every mode then clean up, i.e. what a caller does */
  private ModeAccessCleanupModifierResult restrictAllAndCleanUp() {
    for (var mode : List.of(car, bus, pedestrian)) {
      restrict(mode);
    }
    return layer.getLayerModifier().removeInfrastructureWithoutModeAccess();
  }

  /**
   * A two-way street with a one-way ramp leaving it at node 1 that leads back into it at node 2, the street being of
   * type road with footway
   * <pre>
   *           3 ----&gt; 4
   *     ramp  ^        |
   *           |        v
   *   0 ===== 1 ====== 2
   * </pre>
   *
   * @param rampType type of the three one-way link segments from node 1 by way of nodes 3 and 4 to node 2
   * @return the three one-way link segments, the ramp from node 1 first
   */
  private List<MacroscopicLinkSegment> createStreetWithRamp(MacroscopicLinkSegmentType rampType) {
    createNodes(5);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(1, 2, roadWithFootway, roadWithFootway);
    link(1, 3, rampType, null);
    link(3, 4, rampType, null);
    link(4, 2, rampType, null);
    return List.of(segment(1, 3), segment(3, 4), segment(4, 2));
  }

  /**
   * Ban both turns from the street onto the ramp of {@link #createStreetWithRamp(MacroscopicLinkSegmentType)}
   *
   * @param modes the modes to ban, every mode when empty
   */
  private void banTurnsOntoRamp(List<Mode> modes) {
    MacroscopicNetworkLayerUtils.banTurnForModes(layer, segment(0, 1), segment(1, 3), modes);
    MacroscopicNetworkLayerUtils.banTurnForModes(layer, segment(2, 1), segment(1, 3), modes);
  }

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    pedestrian = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, bus, pedestrian));

    /* access groups carry properties the modes in them share, so vehicular modes and pedestrians belong in
     * separate groups: they have nothing in common to share, least of all a speed. Car and bus do share, which is
     * what makes them the right pair for exercising withdrawal from a group holding more than one mode.
     *
     * Built through the collection form throughout, because the single mode overload silently does nothing when
     * the speed passed exceeds the mode's own maximum, which is trivially the case for a pedestrian */
    carOnly = layer.getLinkSegmentTypes().getFactory().registerNew("carOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(carOnly, 50, List.of(car));

    walkOnly = layer.getLinkSegmentTypes().getFactory().registerNew("walkOnly", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(walkOnly, 5, List.of(pedestrian));

    /* a street: carriageway shared by car and bus, plus a footway alongside it */
    roadWithFootway = layer.getLinkSegmentTypes().getFactory().registerNew("roadWithFootway", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(roadWithFootway, 50, List.of(car, bus));
    AccessGroupPropertiesFactory.createOnLinkSegmentType(roadWithFootway, 5, List.of(pedestrian));

    /* the same street where cars are banned but buses are not, e.g. a bus gate */
    busAndWalk = layer.getLinkSegmentTypes().getFactory().registerNew("busAndWalk", 1800, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(busAndWalk, 50, List.of(bus));
    AccessGroupPropertiesFactory.createOnLinkSegmentType(busAndWalk, 5, List.of(pedestrian));
  }

  /**
   * The case the whole treatment exists for. Node 2 can be driven into but not out of, because the return
   * direction only permits walking. Car access must go, the link must stay, and walking must be untouched.
   */
  @Test
  public void carTrapWithWalkableReturnWithdrawsCarAndKeepsTheLink() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    var trap = link(1, 2, roadWithFootway, walkOnly);   // car in, walk back only

    assertEquals(1, restrict(car).getWithdrawn(), "car withdrawn from the segment leading into the trap");
    assertEquals(0, restrict(pedestrian).getWithdrawn(), "walking is bidirectional here, nothing to withdraw");

    /* the infrastructure survives intact, which is the entire point */
    assertEquals(2, layer.getLinks().size());
    assertEquals(4, layer.getLinkSegments().size());
    assertNotNull(layer.getLinks().get(trap.getId()));

    assertFalse(trap.getLinkSegmentAb().isModeAllowed(car), "car access withdrawn");
    assertTrue(trap.getLinkSegmentAb().isModeAllowed(pedestrian), "walking untouched");
    assertTrue(trap.getLinkSegmentBa().isModeAllowed(pedestrian), "return direction untouched");
  }

  /**
   * Withdrawing one mode must leave the others in the same access group alone. Car and bus share the carriageway
   * properties here, and only car is trapped, since buses may return through the bus gate. If withdrawal reached
   * into the shared group rather than only removing the mode from it, bus would lose access it is entitled to.
   */
  @Test
  public void withdrawingOneModeLeavesAnotherSharingItsAccessGroup() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    /* into the pocket everything may go, back out only buses and pedestrians */
    var trap = link(1, 2, roadWithFootway, busAndWalk);

    assertEquals(1, restrict(car).getWithdrawn(), "only car cannot get back out");
    assertEquals(0, restrict(bus).getWithdrawn(), "buses can return through the bus gate");
    assertEquals(0, restrict(pedestrian).getWithdrawn());

    assertFalse(trap.getLinkSegmentAb().isModeAllowed(car));
    assertTrue(trap.getLinkSegmentAb().isModeAllowed(bus), "bus shares the group car was withdrawn from");
    assertTrue(trap.getLinkSegmentAb().isModeAllowed(pedestrian));

    assertTrue(roadWithFootway.isModeAllowed(car), "original type unchanged");
    assertTrue(roadWithFootway.isModeAllowed(bus), "original type unchanged");
  }

  /**
   * The final pass. Once a segment grants nothing, it goes, and with it a link left with no segments and a node
   * left with no edges. Its now empty type goes too, having no purpose.
   */
  @Test
  public void whatNoModeCanUseIsRemovedByTheFinalPass() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    var stub = link(1, 2, carOnly, null);   // car only, one way, so cars enter node 2 and cannot return
    var stubSegment = stub.getLinkSegmentAb();

    var cleanup = restrictAllAndCleanUp();

    assertEquals(List.of(stubSegment), cleanup.getRemovedLinkSegments(), "nothing could use the segment any more");
    assertEquals(List.of(stub), cleanup.getRemovedLinks(), "the link was left with no segments");
    assertEquals(List.of(nodes.get(2)), cleanup.getRemovedNodes(), "and node 2 was left with no edges");
    assertEquals(List.of(stubSegment.getLinkSegmentType()), cleanup.getRemovedLinkSegmentTypes(),
        "the emptied type has no purpose");

    assertEquals(1, layer.getLinks().size());
    assertEquals(2, layer.getLinkSegments().size());
    assertEquals(2, layer.getNodes().size());
    assertNull(layer.getLinks().get(stub.getId()));
  }

  /**
   * An intersection whose only approach nothing can use any more controls no traffic once that approach goes, so the
   * final pass removes it too, while its node stays for the road still using it.
   */
  @Test
  public void intersectionLeftWithoutApproachesIsRemovedByTheFinalPass() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(2, 1, carOnly, null);   // car only, one way, so cars can leave node 2 but never reach it
    var junction = layer.getIntersections().getFactory().createNonSpatial(
        nodes.get(1), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    junction.addNewApproachSegment(segment(2, 1));
    layer.getIntersections().getFactory().register(junction);

    var cleanup = restrictAllAndCleanUp();

    assertEquals(List.of(junction), cleanup.getRemovedIntersections(),
        "its only approach went, it controls no traffic");
    assertTrue(layer.getIntersections().isEmpty());
    assertNotNull(layer.getNodes().get(nodes.get(1).getId()), "its node stays for the road still using it");
  }

  /**
   * An intersection on a node the final pass removes, left without edges, goes with it, and is stated by the result
   * like one left without approaches.
   */
  @Test
  public void intersectionOnRemovedNodeIsRemovedByTheFinalPass() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(1, 2, carOnly, null);   // car only, one way, so cars enter node 2 and cannot return
    var deadEnd = layer.getIntersections().getFactory().createNonSpatial(
        nodes.get(2), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    deadEnd.addNewApproachSegment(segment(1, 2));
    layer.getIntersections().getFactory().register(deadEnd);

    var cleanup = restrictAllAndCleanUp();

    assertEquals(List.of(nodes.get(2)), cleanup.getRemovedNodes());
    assertEquals(List.of(deadEnd), cleanup.getRemovedIntersections(), "it went with its node");
    assertTrue(layer.getIntersections().isEmpty());
  }

  /**
   * The thresholds decide which subnetworks keep access, exactly as they do for dangling subnetwork removal. Two
   * islands, each internally routable: the safest configuration keeps only the largest, while a size threshold
   * keeps both. Which is wanted is a modelling decision, and it is the caller's to make.
   */
  @Test
  public void thresholdsDecideWhichSubnetworksKeepAccess() {
    createNodes(5);
    var smallIsland = link(0, 1, roadWithFootway, roadWithFootway);   // island of two
    link(2, 3, roadWithFootway, roadWithFootway);                     // island of three
    link(3, 4, roadWithFootway, roadWithFootway);

    /* a threshold below which nothing falls leaves both islands alone */
    var lenient = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, 2, Integer.MAX_VALUE, true, Connectivity.STRONG, null);
    assertTrue(lenient.isEmpty(), "the smaller island is not below the threshold");
    assertTrue(layer.getLinkSegments().stream().allMatch(ls -> ls.isModeAllowed(car)));

    /* whereas keeping only the largest withdraws the smaller island's access */
    var strict = restrict(car);
    assertEquals(2, strict.getWithdrawn(), "both segments of the smaller island lose car access");
    assertFalse(smallIsland.getLinkSegmentAb().isModeAllowed(car));
  }

  /**
   * Weak connectivity asks only whether the mode can get there at all, so a one way trap looks healthy to it.
   * This is the same distinction dangling subnetwork removal draws, applied per mode.
   */
  @Test
  public void weakConnectivityDoesNotSeeAOneWayTrap() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(1, 2, roadWithFootway, walkOnly);

    var weak = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, Integer.MAX_VALUE, Integer.MAX_VALUE, true, Connectivity.WEAK, null);
    assertTrue(weak.isEmpty(), "ignoring direction the car network hangs together, so nothing is withdrawn");

    assertEquals(1, restrict(car).getWithdrawn(), "following direction it does not");
  }

  /**
   * A protected segment keeps its access even though it does not qualify, which is the escape hatch for a caller
   * that knows something the layer does not. The cost is a network that much less connected.
   */
  @Test
  public void protectedSegmentKeepsAccessEvenWhenItDoesNotQualify() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    var trap = link(1, 2, roadWithFootway, walkOnly);

    var result = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, Integer.MAX_VALUE, Integer.MAX_VALUE, true, Connectivity.STRONG,
        (MacroscopicLinkSegment ls) -> ls.getParent().idEquals(trap));

    assertEquals(0, result.getWithdrawn(), "nothing withdrawn, the segment was protected");
    assertEquals(1, result.getProtected());
    assertTrue(trap.getLinkSegmentAb().isModeAllowed(car), "access retained on the protected segment");
  }

  /**
   * A network already routable for every mode must come through completely untouched.
   */
  @Test
  public void fullyRoutableNetworkIsLeftAlone() {
    createNodes(4);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(1, 2, roadWithFootway, roadWithFootway);
    link(2, 3, roadWithFootway, roadWithFootway);

    var cleanup = restrictAllAndCleanUp();

    assertTrue(cleanup.isEmpty());
    assertEquals(3, layer.getLinks().size());
    assertEquals(6, layer.getLinkSegments().size());
    assertEquals(4, layer.getNodes().size());
  }

  /**
   * Types are shared, so withdrawing access must never change the type in place: other segments carrying the same
   * type have to keep the mode. This is the failure that would be silent and widespread.
   */
  @Test
  public void withdrawingAccessDoesNotAffectOtherSegmentsSharingTheType() {
    createNodes(3);
    var healthy = link(0, 1, roadWithFootway, roadWithFootway);
    var trap = link(1, 2, roadWithFootway, walkOnly);

    restrict(car);

    assertFalse(trap.getLinkSegmentAb().isModeAllowed(car));
    assertTrue(healthy.getLinkSegmentAb().isModeAllowed(car), "unrelated segment of the same type keeps car");
    assertTrue(healthy.getLinkSegmentBa().isModeAllowed(car), "unrelated segment of the same type keeps car");
    assertTrue(roadWithFootway.isModeAllowed(car), "the original type itself must be unchanged");
  }

  /**
   * Every turn onto the ramp is banned for cars, so cars can never enter it. The ramp is dangling for cars, and so is
   * what lies beyond it, even though it leads back into the street; the street keeps car access
   */
  @Test
  public void rampTrappedByBannedTurnsIsDanglingForCar() {
    var ramp = createStreetWithRamp(carOnly);
    banTurnsOntoRamp(List.of(car));

    assertEquals(3, restrict(car).getWithdrawn());
    assertTrue(ramp.stream().noneMatch(segment -> segment.isModeAllowed(car)));
    assertTrue(segment(0, 1).isModeAllowed(car));
    assertTrue(segment(2, 1).isModeAllowed(car));
  }

  /**
   * The turns onto the ramp are banned for buses only, so cars keep the ramp while buses lose it
   */
  @Test
  public void banForBusesOnlyLeavesRampNotDanglingForCar() {
    var ramp = createStreetWithRamp(roadWithFootway);
    banTurnsOntoRamp(List.of(bus));

    assertEquals(0, restrict(car).getWithdrawn());
    assertTrue(ramp.stream().allMatch(segment -> segment.isModeAllowed(car)));
    assertEquals(3, restrict(bus).getWithdrawn());
  }

  /**
   * A banned turn naming no mode bans every mode, so the ramp is dangling for each of them
   */
  @Test
  public void banNamingNoModeMakesRampDanglingForEveryMode() {
    var ramp = createStreetWithRamp(roadWithFootway);
    banTurnsOntoRamp(List.of());

    for (var mode : List.of(car, bus, pedestrian)) {
      assertEquals(3, restrict(mode).getWithdrawn());
      assertTrue(ramp.stream().noneMatch(segment -> segment.isModeAllowed(mode)));
    }
  }

  /**
   * Cars lose the ramp to banned turns, while pedestrians and buses can still use it, so the final pass keeps it
   */
  @Test
  public void linkDanglingForCarKeepsPedestrians() {
    var ramp = createStreetWithRamp(roadWithFootway).get(0);
    banTurnsOntoRamp(List.of(car));

    var cleanup = restrictAllAndCleanUp();

    assertTrue(cleanup.getRemovedLinkSegments().isEmpty());
    assertFalse(ramp.isModeAllowed(car));
    assertTrue(ramp.isModeAllowed(pedestrian));
  }

  /**
   * Both ends of a two-way street are dead ends, left only by turning back. Turning back is a turn like any other, so
   * nothing is dangling
   */
  @Test
  public void deadEndIsNotDanglingWhenTurningBackIsAllowed() {
    createNodes(3);
    link(0, 1, roadWithFootway, roadWithFootway);
    link(1, 2, roadWithFootway, roadWithFootway);

    assertEquals(0, restrict(car).getWithdrawn());
  }

  /**
   * A two-way dead end hangs off a one-way loop, and turning back at its end is banned for cars. Cars could drive in
   * but never out, and never onto the way back out, so both its link segments are dangling for cars
   * <pre>
   *   0 ----&gt; 1 ===== 3 (dead end)
   *    ^      |
   *     \     v
   *       --- 2
   * </pre>
   */
  @Test
  public void deadEndIsDanglingWhenTurningBackIsBanned() {
    createNodes(4);
    link(0, 1, carOnly, null);
    link(1, 2, carOnly, null);
    link(2, 0, carOnly, null);
    var deadEnd = link(1, 3, carOnly, carOnly);
    MacroscopicNetworkLayerUtils.banTurnForModes(layer, segment(1, 3), segment(3, 1), List.of(car));

    assertEquals(2, restrict(car).getWithdrawn());
    assertFalse(deadEnd.getLinkSegmentAb().isModeAllowed(car));
    assertFalse(deadEnd.getLinkSegmentBa().isModeAllowed(car));
  }

  /**
   * The size of a dangling subnetwork is the number of nodes its link segments start at. The smaller island has 3
   * nodes and 4 link segments: a threshold of 3 keeps it, a threshold of 4 does not
   */
  @Test
  public void danglingSizeThresholdCountsNodes() {
    createNodes(7);
    var smallIsland = link(0, 1, roadWithFootway, roadWithFootway);   // island of three nodes
    link(1, 2, roadWithFootway, roadWithFootway);
    link(3, 4, roadWithFootway, roadWithFootway);                     // island of four nodes
    link(4, 5, roadWithFootway, roadWithFootway);
    link(5, 6, roadWithFootway, roadWithFootway);

    var atThree = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, 3, Integer.MAX_VALUE, true, Connectivity.STRONG, null);
    assertTrue(atThree.isEmpty());

    var atFour = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, 4, Integer.MAX_VALUE, true, Connectivity.STRONG, null);
    assertEquals(4, atFour.getWithdrawn());
    assertFalse(smallIsland.getLinkSegmentAb().isModeAllowed(car));
  }

  /**
   * Ignoring direction, a banned turn has no meaning, so weak connectivity keeps cars on a ramp the banned turns
   * trap. Strong connectivity does not
   */
  @Test
  public void weakConnectivityDoesNotSeeARampTrappedByBannedTurns() {
    createStreetWithRamp(carOnly);
    banTurnsOntoRamp(List.of(car));

    var weak = MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, car, Integer.MAX_VALUE, Integer.MAX_VALUE, true, Connectivity.WEAK, null);
    assertTrue(weak.isEmpty());

    assertEquals(3, restrict(car).getWithdrawn());
  }

  /**
   * The dangling check leaves banned turns as they are: the bans that take cars off the ramp are still registered once
   * the ramp has lost car access, since pedestrians and buses keep both link segments of each ban
   */
  @Test
  public void banTrappingARampIsKeptAfterTheDanglingCheck() {
    var ramp = createStreetWithRamp(roadWithFootway).get(0);
    banTurnsOntoRamp(List.of(car));

    restrictAllAndCleanUp();

    assertFalse(ramp.isModeAllowed(car));
    assertEquals(2, layer.getBannedTurns().getBySegment(ramp).size());
  }
}
