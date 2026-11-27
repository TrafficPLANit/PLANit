package org.goplanit.algorithms.parallel.shortest;

import org.goplanit.algorithms.parallel.GenericBatchExecutorService;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Runs Dijkstra's one-to-all search from many origins in parallel, each thread with a search of its own, and keeps of
 * each search only the value a given function makes of its result.
 * <p>
 * A one-to-all result holds a cost and an edge segment per vertex of the network, so on a large network keeping the
 * results of all origins would take far more memory than the work needs. Here a function turns each result into the
 * value wanted, such as a row of a zone to zone skim, in the worker thread straight after its search, so a result is
 * never held beyond its own origin. Each thread has a function of its own, made by a given supplier, so a function
 * may keep working arrays over the searches of its thread. The values come in the origins' order, whatever thread
 * made them, so the outcome is the same on any number of threads.
 * </p>
 *
 * @author markr
 */
public class ParallelShortestPathDijkstra {

  /**
   * The search and the function of one thread
   *
   * @param <T> type of the value made of a result
   */
  private static final class SearchOfThread<T> {

    /** the thread's search */
    private final ShortestPathDijkstra search;

    /** the thread's function, from an origin and its one-to-all result to a value */
    private final BiFunction<DirectedVertex, ShortestPathResult, T> valueOfResult;

    /**
     * Constructor
     *
     * @param search of the thread
     * @param valueOfResult function of the thread
     */
    private SearchOfThread(ShortestPathDijkstra search, BiFunction<DirectedVertex, ShortestPathResult, T> valueOfResult) {
      this.search = search;
      this.valueOfResult = valueOfResult;
    }
  }

  /** origins handed to a thread at a time; each search covers the whole network, so few keep the threads evenly busy */
  private static final int ORIGINS_PER_BATCH = 8;

  /** costs by edge segment id, shared by the searches of all threads */
  private final double[] edgeSegmentCosts;

  /** vertices by id, shared by the searches of all threads */
  private final DirectedVertex[] verticesById;

  /** number of threads the searches run on */
  private final int numberOfThreads;

  /**
   * Constructor
   *
   * @param edgeSegmentCosts costs by edge segment id, both physical and virtual
   * @param verticesById vertices by id, both nodes and centroids
   * @param numberOfThreads to run the searches on, at least 1
   */
  public ParallelShortestPathDijkstra(
      final double[] edgeSegmentCosts, final DirectedVertex[] verticesById, int numberOfThreads) {
    PlanItRunTimeException.throwIf(numberOfThreads < 1, "Number of threads must be at least 1, found %d",
        numberOfThreads);
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.verticesById = verticesById;
    this.numberOfThreads = numberOfThreads;
  }

  /**
   * Run a one-to-all search from each origin and make a value of each result in the thread that ran its search
   *
   * @param <T> type of the value made of a result
   * @param origins to search from
   * @param valueOfResultPerThread makes the function of each thread, from an origin and its one-to-all result to a
   *                               value; the result is only valid while the function runs
   * @return the value of each origin, in the origins' order
   */
  public <T> List<T> executeOneToAll(
      final List<? extends DirectedVertex> origins,
      final Supplier<BiFunction<DirectedVertex, ShortestPathResult, T>> valueOfResultPerThread) {
    final Object[] values = new Object[origins.size()];
    BiFunction<Integer, SearchOfThread<T>, Integer> searchFromOrigin = (index, searchOfThread) -> {
      var origin = origins.get(index);
      values[index] = searchOfThread.valueOfResult.apply(origin, searchOfThread.search.executeOneToAll(origin));
      return index;
    };
    List<Integer> originIndices = IntStream.range(0, origins.size()).boxed().collect(Collectors.toList());

    var executor = new GenericBatchExecutorService<Integer, Integer, SearchOfThread<T>>(originIndices,
        (thread, threadId) -> new SearchOfThread<>(
            new ShortestPathDijkstra(edgeSegmentCosts, verticesById), valueOfResultPerThread.get()),
        searchFromOrigin, null);
    executor.executeOrThrowRunTimeException(numberOfThreads, ORIGINS_PER_BATCH, "One-to-all searches");

    @SuppressWarnings("unchecked")
    List<T> valuesInOriginOrder = (List<T>) Arrays.asList(values);
    return valuesInOriginOrder;
  }
}
