package org.goplanit.test.graph;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.graph.algorithms.ConnectedComponents;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.algorithms.ConnectivityAssessment;
import org.goplanit.utils.graph.directed.algorithms.DepthFirstSearch;
import org.goplanit.utils.graph.directed.algorithms.StronglyConnectedComponents;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the graph algorithms, on graphs small enough that the expected answer can be stated by hand.
 *
 * @author markr
 */
public class GraphAlgorithmsTest {

  private MacroscopicNetworkLayer layer;
  private List<Node> nodes;

  /** all edge segments qualify */
  private static final Predicate<EdgeSegment> ANY = es -> true;

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

  /** create the given number of nodes, addressable by index */
  private void createNodes(int count) {
    for (int i = 0; i < count; ++i) {
      nodes.add(layer.getNodes().getFactory().registerNew());
    }
  }

  /** connect a to b, with a segment in the a-&gt;b direction only */
  private void oneWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
  }

  /** connect a and b with segments in both directions */
  private void twoWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    layer.getLinkSegments().getFactory().registerNew(link, false, true);
  }

  private ConnectivityAssessment.Result<Node> assess() {
    return ConnectivityAssessment.assess(layer.getNodes(), ANY);
  }

  /**
   * A single directed cycle is strongly connected in its entirety: every vertex reaches every other by going
   * round.
   */
  @Test
  public void singleDirectedCycleIsOneComponent() {
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    oneWay(3, 0);

    var result = StronglyConnectedComponents.execute(layer.getNodes(), ANY);
    assertEquals(1, result.size());
    assertEquals(4, result.getLargest().size());
    assertTrue(result.areMutuallyReachable(nodes.get(0), nodes.get(2)));
  }

  /**
   * A directed path is the opposite extreme: nothing can get back, so every vertex is its own component.
   */
  @Test
  public void directedPathIsAllSingletons() {
    createNodes(4);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);

    var result = StronglyConnectedComponents.execute(layer.getNodes(), ANY);
    assertEquals(4, result.size());
    assertEquals(1, result.getLargest().size());
    assertFalse(result.areMutuallyReachable(nodes.get(0), nodes.get(1)));
  }

  /** main cycle 0,1,2,3 and pocket cycle 4,5,6, deliberately of unequal size, see note below */
  private void createMainAndPocket() {
    createNodes(7);
    /* main cycle, four nodes so that it is unambiguously the largest. Equal sized components would leave which
     * one counts as "main" down to iteration order, and the assessment would be describing the wrong half */
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(2, 3);
    oneWay(3, 0);
    /* pocket cycle, three nodes */
    oneWay(4, 5);
    oneWay(5, 6);
    oneWay(6, 4);
  }

  /**
   * Two cycles joined by a single one-way link. Weakly this is one network, strongly it is two, which is exactly
   * the discrepancy the assessment exists to expose.
   */
  @Test
  public void twoCyclesJoinedOneWaySplitStronglyButNotWeakly() {
    createMainAndPocket();
    /* single bridge, main into pocket only */
    oneWay(3, 4);

    var weak = ConnectedComponents.execute(layer.getNodes(), edge -> true);
    assertEquals(1, weak.size());
    assertEquals(7, weak.get(0).size());

    var strong = StronglyConnectedComponents.execute(layer.getNodes(), ANY);
    assertEquals(2, strong.size());
    assertEquals(4, strong.getLargest().size());

    /* the pocket can be entered but never left, so it is unusable as an origin */
    var assessment = assess();
    assertEquals(4, assessment.getLargestStronglyConnected().size());
    assertEquals(3, assessment.getNumberOfTrapped());
    assertEquals(3, assessment.getEntryOnly().size());
    assertEquals(0, assessment.getExitOnly().size());
    assertEquals(0, assessment.getSevered().size());
    assertEquals(0, assessment.getPartiallyConnected().size());
    assertEquals(1, assessment.getNumberOfPockets());
  }

  /**
   * The mirror image: a pocket that can only be left. This is the car park case observed in Sydney, where the
   * single connection to the network is a one-way service road pointing outwards.
   */
  @Test
  public void pocketThatCanOnlyBeLeftIsExitOnly() {
    createMainAndPocket();
    /* single bridge, pocket into main only */
    oneWay(4, 0);

    var assessment = assess();
    assertEquals(4, assessment.getLargestStronglyConnected().size());
    assertEquals(3, assessment.getExitOnly().size());
    assertEquals(0, assessment.getEntryOnly().size());
    assertTrue(assessment.getExitOnly().contains(nodes.get(5)));
  }

  /**
   * A pocket can be cut off in both directions while still being weakly connected, but only when it hangs off
   * another pocket rather than off the main network directly.
   * <p>
   * Reached directly, a pocket always has at least one traversable segment to or from the main network, so it is
   * necessarily one-directional. Severing requires a second hop: here the main network cannot reach P, and Q sits
   * downstream of P, so Q can neither be reached from the main network nor return to it.
   * </p>
   */
  @Test
  public void pocketBehindAnotherPocketIsSevered() {
    createMainAndPocket();
    createNodes(3);          // Q, nodes 7,8,9
    oneWay(7, 8);
    oneWay(8, 9);
    oneWay(9, 7);

    oneWay(4, 0);            // P can leave into the main network, nothing leads back into P
    oneWay(4, 7);            // P leads into Q, nothing leads back from Q

    var assessment = assess();
    assertEquals(10, assessment.getLargestWeaklyConnected().size());
    assertEquals(4, assessment.getLargestStronglyConnected().size());
    assertEquals(6, assessment.getNumberOfTrapped());
    assertEquals(3, assessment.getExitOnly().size());
    assertEquals(3, assessment.getSevered().size());
    assertEquals(0, assessment.getEntryOnly().size());
    assertTrue(assessment.getSevered().contains(nodes.get(8)));
    assertEquals(2, assessment.getNumberOfPockets());
  }

  /**
   * A two-way network has no directional traps at all, which is why walking never shows this pathology.
   */
  @Test
  public void fullyBidirectionalNetworkHasNoTrappedVertices() {
    createNodes(5);
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);
    twoWay(3, 4);

    var assessment = assess();
    assertEquals(5, assessment.getLargestWeaklyConnected().size());
    assertEquals(5, assessment.getLargestStronglyConnected().size());
    assertEquals(0, assessment.getNumberOfTrapped());
    assertEquals(0, assessment.getNumberOfPockets());
  }

  /**
   * Assessment is confined to the largest weak component. A separate island must not be counted as trapped,
   * since it is a fragmentation problem rather than a directional one.
   */
  @Test
  public void separateIslandIsNotReportedAsTrapped() {
    createNodes(6);
    /* main, four nodes, bidirectional */
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);
    /* island, two nodes, unconnected to the main */
    twoWay(4, 5);

    var assessment = assess();
    assertEquals(4, assessment.getLargestWeaklyConnected().size());
    assertEquals(4, assessment.getLargestStronglyConnected().size());
    assertEquals(0, assessment.getNumberOfTrapped());
  }

  /**
   * The predicate is the only route by which selection reaches the algorithm. Excluding the bridge must isolate
   * the two halves.
   */
  @Test
  public void predicateRestrictsTraversal() {
    createNodes(4);
    twoWay(0, 1);
    twoWay(1, 2);
    twoWay(2, 3);

    /* exclude every segment attached to the middle link, severing the graph in two */
    var middleLink = layer.getLinks().get(1);
    Predicate<EdgeSegment> notMiddle = es -> es.getParent() == null || !es.getParent().idEquals(middleLink);

    var components = StronglyConnectedComponents.execute(layer.getNodes(), notMiddle);
    assertEquals(2, components.size());
    assertEquals(2, components.getLargest().size());
  }

  /**
   * An isolated vertex still forms a component of its own rather than disappearing.
   */
  @Test
  public void isolatedVertexFormsItsOwnComponent() {
    createNodes(3);
    twoWay(0, 1);

    var components = StronglyConnectedComponents.execute(layer.getNodes(), ANY);
    assertEquals(2, components.size());
    assertEquals(2, components.getLargest().size());
    assertEquals(1, components.getComponents().get(1).size());
  }

  /**
   * Depth first traversal must finish every vertex exactly once, and must not recurse, which is what allows it to
   * run on networks far deeper than the stack would allow.
   */
  @Test
  public void depthFirstFinishesEveryVertexOnce() {
    createNodes(200);
    for (int i = 0; i < 199; ++i) {
      oneWay(i, i + 1);
    }

    var order = DepthFirstSearch.finishOrder(layer.getNodes(), ANY, false);
    assertEquals(200, order.size());
    assertEquals(200, order.stream().distinct().count());
    /* the end of the chain has nothing downstream, so it finishes before the vertex that leads into it */
    assertTrue(order.indexOf(nodes.get(199)) < order.indexOf(nodes.get(0)));
  }

  /**
   * A long chain would overflow the stack under a recursive formulation. 100k vertices is well beyond the default
   * stack depth, so completing at all is the assertion.
   */
  @Test
  public void deepChainDoesNotOverflowTheStack() {
    final int count = 100_000;
    createNodes(count);
    for (int i = 0; i < count - 1; ++i) {
      oneWay(i, i + 1);
    }

    var components = StronglyConnectedComponents.execute(layer.getNodes(), ANY);
    assertEquals(count, components.size());
  }
}
