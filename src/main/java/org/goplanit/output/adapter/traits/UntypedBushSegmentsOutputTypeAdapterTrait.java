package org.goplanit.output.adapter.traits;

import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.output.adapter.traits.NetworkSegmentsOutputTypeAdapterTrait;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * Interface defining the methods required for a bush-based specific segments output adapter, e.g., per bush allow for
 * persisting of its link information.
 * 
 * @author markr
 *
 */
public interface UntypedBushSegmentsOutputTypeAdapterTrait<ES extends EdgeSegment>
        extends NetworkSegmentsOutputTypeAdapterTrait<ES> {

  /**
   * Provide access to all bushes eligible for persisting. Each bush contains a subset of the physical network used by
   * the traffic assignment
   *
   * @return collection of bushes
   */
  public abstract RootedBush<? extends DirectedVertex, ? extends ES>[] getBushes();
}
