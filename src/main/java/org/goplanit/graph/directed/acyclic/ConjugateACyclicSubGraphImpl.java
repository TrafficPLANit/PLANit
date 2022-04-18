package org.goplanit.graph.directed.acyclic;

import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ConjugateACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * An acyclic sub graph contains a subset of the full graph without cycles. The active subset of the graph is tracked by explicitly registering edge segments. Edge segments are by
 * definition directed.
 * 
 * Whenever edge segments are added it is verified that no cycles are created. Also each edge segment that is added must connect to the existing subgraph's contents
 * 
 * 
 * @author markr
 *
 */
public class ConjugateACyclicSubGraphImpl extends UntypedACyclicSubGraphImpl<ConjugateDirectedVertex, ConjugateEdgeSegment> implements ConjugateACyclicSubGraph {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateACyclicSubGraphImpl.class.getCanonicalName());

  /**
   * Constructor. It is expected that all provided root vertices represent edges in the orignal network leading to a single root.
   * 
   * @param groupId                       generate id based on the group it resides in
   * @param rootVertices                  the root vertices of the conjugate bush which can be the end or starting point depending whether or not direction is inverted.
   * @param invertedDirection             when true dag ends at root and all other vertices precede it, when false the root is the starting point and all other vertices succeed it
   * @param numberOfConjugateEdgeSegments number of conjugate directed edge segments of the parent this subgraph is a subset from
   */
  public ConjugateACyclicSubGraphImpl(final IdGroupingToken groupId, Set<ConjugateDirectedVertex> rootVertices, boolean invertedDirection, int numberOfConjugateEdgeSegments) {
    super(groupId, rootVertices, invertedDirection, numberOfConjugateEdgeSegments);
  }

  /**
   * Copy constructor
   * 
   * @param aCyclicSubGraphImpl to copy
   */
  public ConjugateACyclicSubGraphImpl(ConjugateACyclicSubGraphImpl aCyclicSubGraphImpl) {
    super(aCyclicSubGraphImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateACyclicSubGraphImpl clone() {
    return new ConjugateACyclicSubGraphImpl(this);
  }

}
