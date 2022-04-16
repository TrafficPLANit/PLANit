package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityImpl;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Conjugate EdgeSegment represents an edge in a particular (single) direction in a conjugate directed graph.
 *
 * @author markr
 *
 */
public class ConjugateEdgeSegmentImpl extends GraphEntityImpl implements ConjugateEdgeSegment {

  /** UID */
  private static final long serialVersionUID = 8906736183855154599L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConjugateEdgeSegmentImpl.class.getCanonicalName());

  /**
   * Store the direction of this edge segment in relation to its parent edge
   */
  private boolean directionAb;

  /**
   * segment's parent edge
   */
  private ConjugateDirectedEdge parentEdge;

  // Public

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent edge of segment
   * @param directionAb direction of travel
   */
  protected ConjugateEdgeSegmentImpl(final IdGroupingToken groupId, final ConjugateDirectedEdge parentEdge, final boolean directionAb) {
    this(groupId, directionAb);
    setParent(parentEdge);
    this.directionAb = directionAb;
  }

  /**
   * Constructor (without setting parent edge)
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   */
  protected ConjugateEdgeSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    super(groupId, CONJUGATE_EDGE_SEGMENT_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param edgeSegmentImpl to copy
   */
  protected ConjugateEdgeSegmentImpl(ConjugateEdgeSegmentImpl edgeSegmentImpl) {
    super(edgeSegmentImpl);
    setParent(edgeSegmentImpl.getParentEdge());
    this.directionAb = edgeSegmentImpl.directionAb;
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectionAb() {
    return this.directionAb;
  }

  // Getter - Setters

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdge getParentEdge() {
    return this.parentEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setParent(DirectedEdge parentEdge) {
    if (parentEdge == null) {
      LOGGER.warning(String.format("Parent edge is null, unable to set on conjugate edge segment (id: %d)", getId()));
      return;
    }
    this.parentEdge = (ConjugateDirectedEdge) parentEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeParentEdge() {
    this.parentEdge = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeSegmentImpl clone() {
    return new ConjugateEdgeSegmentImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean validate() {
    return EdgeSegmentImpl.validate(this);
  }

}
