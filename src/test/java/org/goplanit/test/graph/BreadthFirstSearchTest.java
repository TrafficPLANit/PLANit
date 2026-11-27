package org.goplanit.test.graph;

import java.util.List;
import java.util.Set;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.graph.directed.algorithms.BreadthFirstSearch;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the breadth-first search from several start vertices at once
 *
 * @author markr
 */
public class BreadthFirstSearchTest extends LayerTestBase {

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
  }

  /**
   * Searching from 0 and 3 at once reaches what either reaches, over the segments allowed. The segment 4-5 is not
   * allowed, so 5 is not reached; the starts have no back link
   * <pre>
   *   0 ---> 1 ---> 2
   *   3 ---> 4 -x-> 5
   * </pre>
   */
  @Test
  public void searchFromSeveralStartsReachesWhatEitherReaches() {
    createNodes(6);
    oneWay(0, 1);
    oneWay(1, 2);
    oneWay(3, 4);
    oneWay(4, 5);
    var excluded = segment(4, 5);

    var result = BreadthFirstSearch.<Node, MacroscopicLinkSegment>execute(List.of(nodes.get(0), nodes.get(3)), false,
        segment -> segment != excluded, (previous, segment) -> segment != excluded, (node, segment) -> false);

    assertNull(result.first());
    var backLinks = result.second();
    assertEquals(Set.of(nodes.get(0), nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4)), backLinks.keySet());
    assertNull(backLinks.get(nodes.get(0)));
    assertNull(backLinks.get(nodes.get(3)));
    assertSame(segment(1, 2), backLinks.get(nodes.get(2)));
  }
}
