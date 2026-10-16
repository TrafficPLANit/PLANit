package org.goplanit.output.adapter.traits;

import org.goplanit.assignment.common.bush.RootedBush;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.Set;

/**
 * Interface defining the methods required for a bush-based specific segments output adapter, e.g., per bush allow for
 * persisting of its link information.
 * 
 * @author markr
 * @param <ES> type of segment
 */
public interface UntypedBushSegmentsOutputTypeAdapterTrait<ES extends EdgeSegment>
        extends NetworkSegmentsOutputTypeAdapterTrait<ES> {

  /**
   * Provide access to all bushes eligible for persisting. Each bush contains a subset of the physical network used by
   * the traffic assignment
   *
   * @return collection of bushes
   */
  public abstract Set<RootedBush<? extends DirectedVertex, ? extends DirectedEdge, ? extends ES>> getBushes();

  /**
   * Allow to check on non zero flow using edge segment rather than concrete bush segment derived version
   *
   * @param bush to use
   * @param edgeSegment to check
   * @return true when flow present, false otherwise
   */
  public abstract boolean hasNonZeroFlow(
      RootedBush<? extends DirectedVertex, ? extends DirectedEdge,? extends EdgeSegment> bush, EdgeSegment edgeSegment);
}
