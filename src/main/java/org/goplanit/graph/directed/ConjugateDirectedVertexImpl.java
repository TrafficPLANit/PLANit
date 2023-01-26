package org.goplanit.graph.directed;

import java.util.Collection;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Conjugate directed vertex representation connected to one or more entry and exit conjugate edges
 *
 * @author markr
 *
 */
public class ConjugateDirectedVertexImpl extends DirectedVertexImpl<ConjugateEdgeSegment> implements ConjugateDirectedVertex {

  /** UID */
  private static final long serialVersionUID = 3357507383489421626L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateDirectedVertexImpl.class.getCanonicalName());

  // Protected

  /** original edge this conjugate vertex represents */
  protected final DirectedEdge originalEdge;

  /**
   * Constructor
   * 
   * @param groupId,          contiguous id generation within this group for instances of this class
   * @param originalEdge this conjugate represents in the conjugate graph
   */
  protected ConjugateDirectedVertexImpl(final IdGroupingToken groupId, final DirectedEdge originalEdge) {
    super(groupId);
    this.originalEdge = originalEdge;
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateDirectedVertexImpl(ConjugateDirectedVertexImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.originalEdge = other.originalEdge;
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<ConjugateEdgeSegment> getEntryEdgeSegments() {
    return entryEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<ConjugateEdgeSegment> getExitEdgeSegments() {
    return exitEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends ConjugateDirectedEdge> getEdges() {
    return (Collection<? extends ConjugateDirectedEdge>) super.getEdges();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertexImpl clone() {
    return new ConjugateDirectedVertexImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedVertexImpl deepClone() {
    return new ConjugateDirectedVertexImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdge getOriginalEdge() {
    return originalEdge;
  }

}
