package org.goplanit.test.shortestpath;

import org.goplanit.algorithms.parallel.shortest.ParallelShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the parallel one-to-all search against the single search run one origin at a time
 *
 * @author markr
 */
public class ParallelShortestPathDijkstraTest {

  /**
   * Cost to reach every vertex in a one-to-all result
   *
   * @param result of the search
   * @param verticesById to report for
   * @return cost per vertex id
   */
  private static double[] costsToAll(ShortestPathResult result, DirectedVertex[] verticesById) {
    double[] costsToAll = new double[verticesById.length];
    for (var vertex : verticesById) {
      costsToAll[(int) vertex.getId()] = result.getCostToReach(vertex);
    }
    return costsToAll;
  }

  /** costs by edge segment id */
  private double[] costs;

  /** the grid's nodes by id */
  private DirectedVertex[] verticesById;

  /** every node of the grid as origin, in an order other than their ids */
  private List<DirectedVertex> origins;

  /**
   * A grid of 6 by 6 nodes, 1 km apart, with every link two way, and costs that differ from segment to segment so
   * the paths are not all alike
   */
  @BeforeEach
  public void initialise() {
    var network = MacroscopicNetworkUtils.createSimpleGrid(
        IdGenerator.createIdGroupingToken(ParallelShortestPathDijkstraTest.class.getCanonicalName()), 6, 6);
    var layer = network.getTransportLayers().getFirst();

    costs = new double[layer.getLinkSegments().size()];
    layer.getLinkSegments().forEach(segment -> costs[(int) segment.getId()] = 1 + (segment.getId() * 37) % 11);

    verticesById = new DirectedVertex[layer.getNodes().size()];
    layer.getNodes().forEach(node -> verticesById[(int) node.getId()] = node);

    origins = new ArrayList<>(List.of(verticesById));
    Collections.reverse(origins);
    Collections.swap(origins, 3, 17);
  }

  /**
   * The values come in the origins' order and equal those of one search per origin, on one thread and on four
   */
  @Test
  public void valuesInTheOriginsOrderEqualThoseOfOneSearchAtATime() {
    var single = new ShortestPathDijkstra(costs, verticesById);
    var expected = new ArrayList<double[]>();
    for (var origin : origins) {
      expected.add(costsToAll(single.executeOneToAll(origin), verticesById));
    }

    for (int threads : new int[]{1, 4}) {
      var parallel = new ParallelShortestPathDijkstra(costs, verticesById, threads);
      List<double[]> values = parallel.executeOneToAll(
          origins, () -> (origin, result) -> costsToAll(result, verticesById));

      assertEquals(origins.size(), values.size());
      for (int index = 0; index < origins.size(); ++index) {
        assertArrayEquals(expected.get(index), values.get(index), "origin at " + index + ", threads " + threads);
      }
    }
  }

  /**
   * Each thread has a function of its own, so a function may keep a working array over the searches of its thread
   */
  @Test
  public void eachThreadHasAFunctionOfItsOwn() {
    int threads = 4;
    var parallel = new ParallelShortestPathDijkstra(costs, verticesById, threads);
    List<int[]> workingArrays = parallel.executeOneToAll(origins, () -> {
      int[] searches = new int[1];
      return (origin, result) -> {
        ++searches[0];
        return searches;
      };
    });

    var distinctArrays = new IdentityHashMap<int[], Boolean>();
    workingArrays.forEach(array -> distinctArrays.put(array, Boolean.TRUE));
    assertTrue(distinctArrays.size() <= threads);
    assertEquals(origins.size(), distinctArrays.keySet().stream().mapToInt(array -> array[0]).sum());
  }
}
