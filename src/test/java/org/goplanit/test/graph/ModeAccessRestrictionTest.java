package org.goplanit.test.graph;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
public class ModeAccessRestrictionTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer layer;
  private List<Node> nodes;

  private Mode car;
  private Mode bus;
  private Mode pedestrian;

  private MacroscopicLinkSegmentType roadWithFootway;
  private MacroscopicLinkSegmentType busAndWalk;
  private MacroscopicLinkSegmentType walkOnly;
  private MacroscopicLinkSegmentType carOnly;

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
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

    nodes = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  private void createNodes(int count) {
    for (int i = 0; i < count; ++i) {
      nodes.add(layer.getNodes().getFactory().registerNew());
    }
  }

  /** create a link with a segment in each direction, each with its own type */
  private MacroscopicLink link(int a, int b, MacroscopicLinkSegmentType typeAb, MacroscopicLinkSegmentType typeBa) {
    var theLink = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    if (typeAb != null) {
      layer.getLinkSegments().getFactory().registerNew(theLink, true, true).setLinkSegmentType(typeAb);
    }
    if (typeBa != null) {
      layer.getLinkSegments().getFactory().registerNew(theLink, false, true).setLinkSegmentType(typeBa);
    }
    return theLink;
  }

  /** the safest configuration, a single subnetwork the mode can route across in full */
  private MacroscopicNetworkLayerUtils.Result restrict(Mode mode) {
    return MacroscopicNetworkLayerUtils.restrictModeAccessToConnectedSubNetworks(
        layer, mode, Integer.MAX_VALUE, Integer.MAX_VALUE, true, Connectivity.STRONG, null);
  }

  /** run every mode then clean up, i.e. what a caller does */
  private MacroscopicNetworkLayerUtils.CleanupResult restrictAllAndCleanUp() {
    for (var mode : List.of(car, bus, pedestrian)) {
      restrict(mode);
    }
    return MacroscopicNetworkLayerUtils.removeInfrastructureWithoutModeAccess(layer);
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

    var cleanup = restrictAllAndCleanUp();

    assertEquals(1, cleanup.getRemovedLinkSegments(), "nothing could use the segment any more");
    assertEquals(1, cleanup.getRemovedLinks(), "the link was left with no segments");
    assertEquals(1, cleanup.getRemovedNodes(), "and node 2 was left with no edges");
    assertEquals(1, cleanup.getRemovedLinkSegmentTypes(), "the emptied type has no purpose");

    assertEquals(1, layer.getLinks().size());
    assertEquals(2, layer.getLinkSegments().size());
    assertEquals(2, layer.getNodes().size());
    assertNull(layer.getLinks().get(stub.getId()));
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
}
