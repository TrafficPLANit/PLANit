package org.goplanit.algorithms.parallel.shortest;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Wrapper around shortest path Dijkstra algorithm to perform parallel batch execution
 *
 * @author markr
 */
public class ParallelShortestPathDijkstra {

  public void runParallelDijkstra(
      List<DirectedVertex> targetOrigins,
      double[] sharedCosts,
      DirectedVertex[] verticesById) throws ExecutionException, InterruptedException {

    // Define instantiating Dijkstra context for thread
    // Each thread gets its own instance, ensuring distinct tracking arrays
    var algoProvider = (BiFunction<Thread, Integer, ShortestPathDijkstra>) (thread, id) -> {
      // Cloning cost reference or passing local state copy if necessary
      return new ShortestPathDijkstra(sharedCosts, verticesById);
    };

    // Define the isolated path execution logic
    var taskProcessor = (BiFunction<DirectedVertex, ShortestPathDijkstra, ShortestPathResult>) (origin, dijkstra) -> {
      return dijkstra.executeOneToAll(origin, java.util.Collections.emptySet());
    };

    // Define what to do with results once a chunk finishes
    var resultConsumer = (Consumer<List<ShortestPathResult>>) (batchOutputs) -> {
      for (ShortestPathResult res : batchOutputs) {
        processPathResultData(res);
      }
    };

    // Wrap everything up and trigger execution
    GenericBatchExecutorService<DirectedVertex, ShortestPathResult, ShortestPathDijkstra> executor =
        new GenericBatchExecutorService<>(targetOrigins, algoProvider, taskProcessor, resultConsumer);

    int threads = Runtime.getRuntime().availableProcessors();
    int batchSize = 128; // todo: revisit this

    executor.execute(threads, batchSize);
  }

  private void processPathResultData(ShortestPathResult result) {
    // Domain specific handling code goes here...
  }
}
