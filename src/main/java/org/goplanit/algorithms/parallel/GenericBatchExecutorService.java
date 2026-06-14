package org.goplanit.algorithms.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A generic multi-threaded batch executor.
 * Separates data batching and parallel execution mechanics from specific domain algorithms.
 *
 * @param <I> The input element type to be processed
 * @param <O> The output element type generated per input element
 * @param <A> The thread-local execution context/algorithm context type (e.g., ShortestPathDijkstra)
 */
public class GenericBatchExecutorService<I, O, A> {

  private static final Logger LOGGER = Logger.getLogger(GenericBatchExecutorService.class.getCanonicalName());

  private final Collection<I> inputs;
  private final BiFunction<Thread, Integer, A> algoContextProvider;
  private final BiFunction<I, A, O> taskProcessor;
  private final Consumer<List<O>> batchResultConsumer;

  /**
   * Aggregated metrics for a single executed chunk
   */
  public static final class BatchSummary {
    private final int processedCount;

    public BatchSummary(int processedCount) {
      this.processedCount = processedCount;
    }

    public int getProcessedCount() {
      return processedCount;
    }
  }

  /**
   * Constructor
   * @param inputs               The flat collection of all independent inputs to process.
   * @param algoContextProvider  Factory to create/retrieve a thread-isolated algorithm instance.
   *                             Accepts (Thread, ThreadId) as parameters.
   * @param taskProcessor        The execution logic processing an Input given a thread's isolated Algorithm context.
   * @param batchResultConsumer  Optional callback hook executed inside the worker thread immediately after a
   *                             complete batch chunk finishes processing. Can be null.
   */
  public GenericBatchExecutorService(
      Collection<I> inputs,
      BiFunction<Thread, Integer, A> algoContextProvider,
      BiFunction<I, A, O> taskProcessor,
      Consumer<List<O>> batchResultConsumer) {
    this.inputs = inputs;
    this.algoContextProvider = algoContextProvider;
    this.taskProcessor = taskProcessor;
    this.batchResultConsumer = batchResultConsumer;
  }

  /**
   * Executes processing over the inputs across a thread pool.
   *
   * @param threadsToUse number of threads
   * @param batchSize the batch size to chunk the problem into
   * @throws InterruptedException throw if error
   * @throws ExecutionException thrown if error
   */
  public void execute(int threadsToUse, int batchSize) throws InterruptedException, ExecutionException {
    final ExecutorService exec = Executors.newFixedThreadPool(threadsToUse);
    final CompletionService<BatchSummary> cs = new ExecutorCompletionService<>(exec);

    // Use ThreadLocal to bind an independent algorithm worker instance per thread context
    final AtomicInteger threadIdCounter = new AtomicInteger(0);
    final ThreadLocal<A> threadIsolatedAlgo = ThreadLocal.withInitial(() ->
        algoContextProvider.apply(Thread.currentThread(), threadIdCounter.getAndIncrement())
    );

    final AtomicInteger submittedBatches = new AtomicInteger(0);

    try {
      // Partition tasks and submit
      List<I> currentBatch = new ArrayList<>(batchSize);
      for (I input : inputs) {
        currentBatch.add(input);
        if (currentBatch.size() >= batchSize) {
          submitBatch(cs, List.copyOf(currentBatch) /* to be processed inputs */, threadIsolatedAlgo, submittedBatches);
          currentBatch.clear();
        }
      }
      // Deal with leftovers
      if (!currentBatch.isEmpty()) {
        submitBatch(cs, List.copyOf(currentBatch), threadIsolatedAlgo, submittedBatches);
      }

      // Consume batch completion
      awaitAndReport(cs, submittedBatches.get());

    } finally {
      exec.shutdown();
      threadIsolatedAlgo.remove();
    }
  }

  /**
   * Run a single batch
   * @param completionService to apply
   * @param batchInputData the batch input data
   * @param threadIsolatedAlgo the algorithm to apply
   * @param submittedBatches track number of submitted batches
   */
  private void submitBatch(
      CompletionService<BatchSummary> completionService,
      List<I> batchInputData,
      ThreadLocal<A> threadIsolatedAlgo,
      AtomicInteger submittedBatches) {

    completionService.submit(() -> {

      A algoInstance = threadIsolatedAlgo.get();
      List<O> outputs = new ArrayList<>(batchInputData.size());

      // do the actual work within the thread
      for (I input : batchInputData) {
        O output = taskProcessor.apply(input, algoInstance);
        outputs.add(output);
      }

      if (batchResultConsumer != null) {
        batchResultConsumer.accept(outputs);
      }

      return new BatchSummary(batchInputData.size());
    });

    submittedBatches.incrementAndGet();
  }

  /**
   * Deal with completed batches and summarize
   * @param completionService the completion service with the summary
   * @param totalBatches total run
   * @throws InterruptedException if error
   * @throws ExecutionException if error
   */
  private void awaitAndReport(CompletionService<BatchSummary> completionService, int totalBatches)
      throws InterruptedException, ExecutionException {
    int totalProcessed = 0;
    for (int i = 0; i < totalBatches; i++) {
      BatchSummary summary = completionService.take().get();
      totalProcessed += summary.getProcessedCount();

      if (totalProcessed % 5000 == 0 || i == totalBatches - 1) {
        LOGGER.info(String.format("Parallel Execution Progress: %d tasks processed.", totalProcessed));
      }
    }
  }
}
