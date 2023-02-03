package org.goplanit.graph.directed.acyclic;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
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
public class ACyclicSubGraphImpl extends UntypedACyclicSubGraphImpl<DirectedVertex, EdgeSegment> implements ACyclicSubGraph {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ACyclicSubGraphImpl.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param groupId                    generate id based on the group it resides in
   * @param rootVertex                 of the dag
   * @param invertedDirection          when true dag ends at root and all other vertices precede it, when false the root is the starting point and all other vertices succeed it
   * @param numberOfParentEdgeSegments number of directed edge segments of the parent this subgraph is a subset from
   */
  public ACyclicSubGraphImpl(final IdGroupingToken groupId, DirectedVertex rootVertex, boolean invertedDirection, int numberOfParentEdgeSegments) {
    super(groupId, rootVertex, invertedDirection, numberOfParentEdgeSegments);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ACyclicSubGraphImpl(ACyclicSubGraphImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraphImpl shallowClone() {
    return new ACyclicSubGraphImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ACyclicSubGraphImpl deepClone() {
    return new ACyclicSubGraphImpl(this, true);
  }

}
