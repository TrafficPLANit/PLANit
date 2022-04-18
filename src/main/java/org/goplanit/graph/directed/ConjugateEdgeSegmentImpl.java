package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Conjugate EdgeSegment represents an edge in a particular (single) direction in a conjugate directed graph.
 *
 * @author markr
 *
 */
public class ConjugateEdgeSegmentImpl extends EdgeSegmentImpl<ConjugateDirectedEdge> implements ConjugateEdgeSegment {

  /** UID */
  private static final long serialVersionUID = 8906736183855154599L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateEdgeSegmentImpl.class.getCanonicalName());

  // Public

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent edge of segment
   * @param directionAb direction of travel
   * @param idClazz     to use
   */
  protected ConjugateEdgeSegmentImpl(final IdGroupingToken groupId, final ConjugateDirectedEdge parentEdge, final boolean directionAb, final Class<ConjugateEdgeSegment> idClazz) {
    super(groupId, parentEdge, directionAb, idClazz);
  }

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent edge of segment
   * @param directionAb direction of travel
   */
  protected ConjugateEdgeSegmentImpl(final IdGroupingToken groupId, final ConjugateDirectedEdge parentEdge, final boolean directionAb) {
    this(groupId, parentEdge, directionAb, CONJUGATE_EDGE_SEGMENT_ID_CLASS);
  }

  /**
   * Constructor (without setting parent edge)
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   */
  protected ConjugateEdgeSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    this(groupId, null, directionAB);
  }

  /**
   * Copy constructor
   * 
   * @param edgeSegmentImpl to copy
   */
  protected ConjugateEdgeSegmentImpl(ConjugateEdgeSegmentImpl edgeSegmentImpl) {
    super(edgeSegmentImpl);
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
