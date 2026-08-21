package org.goplanit.test.graph;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for removing dangling subnetworks under both notions of connectivity.
 * <p>
 * The fixture throughout is the situation the strong notion exists for: a pocket that is attached to the network
 * yet cannot be both entered and left, which weak connectivity considers a perfectly ordinary part of the network.
 * </p>
 *
 * @author markr
 */
public class DanglingSubNetworkRemovalTest {

  private MacroscopicNetworkLayer layer;
  private List<Node> nodes;

  /** all link segments belong to the network being pruned */
  private static final Predicate<MacroscopicLinkSegment> ANY = ls -> true;

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
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

  /** connect a to b with a segment in the a-&gt;b direction only */
  private MacroscopicLink oneWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    return link;
  }

  /** connect a and b with segments in both directions */
  private MacroscopicLink twoWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    layer.getLinkSegments().getFactory().registerNew(link, false, true);
    return link;
  }

  /**
   * Main cycle 0,1,2,3 and pocket cycle 4,5,6 joined by a single one way link into the pocket, so the pocket can
   * be entered but never left. Sizes are unequal so that the main cycle is unambiguously the largest.
   */
  private void createMainAndPocketJoinedIntoPocket() {
    createNodes(7);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    oneWay(3, 0);
    oneWay(4, 5);
    oneWay(5, 6);
    oneWay(6, 4);
    oneWay(3, 4);
  }

  private void removeDangling(Connectivity connectivity, Predicate<MacroscopicLinkSegment> eligible) {
    layer.getLayerModifier().removeDanglingSubnetworks(
        Integer.MAX_VALUE,          // below size, i.e. anything that is not the largest qualifies
        Integer.MAX_VALUE,          // above size, effectively no upper bound
        true,                       // always keep the largest
        false,                      // leave managed ids alone so the assertions address the same entities
        eligible,
        connectivity);
  }

  /**
   * The established behaviour, and the reason the strong notion is wanted: ignoring direction there is a single
   * subnetwork here, so nothing at all is dangling and nothing is removed.
   */
  @Test
  public void weakConnectivityLeavesTheOneWayPocketInPlace() {
    createMainAndPocketJoinedIntoPocket();

    removeDangling(Connectivity.WEAK, ANY);

    assertEquals(7, layer.getNodes().size());
    assertEquals(8, layer.getLinks().size());
    assertEquals(8, layer.getLinkSegments().size());
  }

  /**
   * Following direction the pocket is a subnetwork of its own, so it is removed. Critically the link joining it to
   * the main network goes with it: it belongs to neither component, and leaving it behind would strand the node it
   * attaches to.
   */
  @Test
  public void strongConnectivityRemovesTheOneWayPocketWithoutLeavingAStub() {
    createMainAndPocketJoinedIntoPocket();

    removeDangling(Connectivity.STRONG, ANY);

    /* only the main cycle survives, in full */
    assertEquals(4, layer.getNodes().size());
    assertEquals(4, layer.getLinks().size());
    assertEquals(4, layer.getLinkSegments().size());

    for (int i = 0; i < 4; ++i) {
      assertNotNull(layer.getNodes().get(nodes.get(i).getId()), "main network node should have survived");
    }
    for (int i = 4; i < 7; ++i) {
      assertNull(layer.getNodes().get(nodes.get(i).getId()), "pocket node should have been removed");
    }
  }

  /**
   * The mirror image, a pocket that can only be left, which is the car park case observed in Sydney where the only
   * car connection is a one way service road pointing outwards.
   */
  @Test
  public void strongConnectivityRemovesAPocketThatCanOnlyBeLeft() {
    createNodes(7);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    oneWay(3, 0);
    oneWay(4, 5);
    oneWay(5, 6);
    oneWay(6, 4);
    oneWay(4, 0);   // out of the pocket only

    removeDangling(Connectivity.STRONG, ANY);

    assertEquals(4, layer.getNodes().size());
    assertEquals(4, layer.getLinks().size());
  }

  /**
   * The rule that protects shared infrastructure must survive the move to strong connectivity, since it is the
   * only thing stopping a pass over one network from deleting nodes another network still hangs off.
   * <p>
   * Node 5 sits in the pocket but also carries a link outside the network being pruned, standing in for the rail
   * line embedded in a street. The pocket is removed around it, yet node 5 and its outside link must remain. Note
   * this is precisely the case that distinguishes the two reasons an edge can be excluded: node 4 is also attached
   * to an excluded link, the one joining it to the main network, but that one is excluded merely for crossing to
   * another component of the <i>same</i> network and so does not protect it.
   * </p>
   */
  @Test
  public void strongConnectivityStillProtectsNodesSharedWithOtherInfrastructure() {
    createMainAndPocketJoinedIntoPocket();
    createNodes(1);                            // node 7, the far end of the outside link
    var outsideLink = twoWay(5, 7);

    /* stands in for a track type or mode test, restricted to identity so the test does not depend on mode setup */
    Predicate<MacroscopicLinkSegment> notOutside =
        ls -> ls.getParent() == null || !ls.getParent().idEquals(outsideLink);

    removeDangling(Connectivity.STRONG, notOutside);

    /* four main network nodes, plus node 5 and node 7 held in place by the outside link */
    assertEquals(6, layer.getNodes().size());
    assertNotNull(layer.getNodes().get(nodes.get(5).getId()), "node shared with outside infrastructure kept");
    assertNotNull(layer.getNodes().get(nodes.get(7).getId()), "far end of outside link kept");
    assertNull(layer.getNodes().get(nodes.get(4).getId()), "pocket node only bordering its own network removed");
    assertNull(layer.getNodes().get(nodes.get(6).getId()), "pocket node removed");

    /* the outside link survives intact, which is the whole point of the protection */
    assertNotNull(layer.getLinks().get(outsideLink.getId()));
    assertEquals(5, layer.getLinks().size());
  }

  /**
   * A network that is already strongly connected must come through untouched, so that opting in to the stricter
   * notion is not a licence to remove anything else.
   */
  @Test
  public void strongConnectivityLeavesAStronglyConnectedNetworkAlone() {
    createNodes(5);
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);
    twoWay(3, 4);

    removeDangling(Connectivity.STRONG, ANY);

    assertEquals(5, layer.getNodes().size());
    assertEquals(4, layer.getLinks().size());
    assertEquals(8, layer.getLinkSegments().size());
  }
}
