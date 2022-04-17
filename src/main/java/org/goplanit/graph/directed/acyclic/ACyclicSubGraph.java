package org.goplanit.graph.directed.acyclic;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * <p>
 * A topological sort on the current state of the graph allows for fast traversal of the graph for various algorithms (shortest path). It also reveals if the graph is still
 * acyclic.
 * 
 * @author markr
 *
 */
public interface ACyclicSubGraph extends UntypedACyclicSubGraph<DirectedVertex, EdgeSegment> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ACyclicSubGraph clone();

}
