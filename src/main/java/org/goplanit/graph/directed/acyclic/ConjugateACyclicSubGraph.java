package org.goplanit.graph.directed.acyclic;

import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;

/**
 * 
 * An conjugate acyclic sub graph contains a subset of a graph without cycles. The active subset of the graph is tracked by explicitly registering conjugate edge segments.
 * Conjugate Edge segments are by definition directed.
 * <p>
 * A topological sort on the current state of the conjugate graph allows for fast traversal of the conjugate graph for various algorithms (shortest path). It also reveals if the
 * graph is still acyclic.
 * 
 * @author markr
 *
 */
public interface ConjugateACyclicSubGraph extends UntypedACyclicSubGraph<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ConjugateACyclicSubGraph clone();

}
