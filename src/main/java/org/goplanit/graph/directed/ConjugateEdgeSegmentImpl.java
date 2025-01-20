package org.goplanit.graph.directed;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Logger;

import org.goplanit.utils.graph.ConjugateEdge;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.ConjugateDirectedEdge;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Conjugate EdgeSegment represents an edge in a particular (single) direction in a conjugate directed graph.
 *
 * @author markr
 *
 */
public class ConjugateEdgeSegmentImpl extends EdgeSegmentImpl implements ConjugateEdgeSegment {

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
   */
  protected ConjugateEdgeSegmentImpl(
      final IdGroupingToken groupId, final ConjugateDirectedEdge parentEdge, final boolean directionAb) {
    super(groupId, parentEdge, directionAb);
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
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ConjugateEdgeSegmentImpl(ConjugateEdgeSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * Static implementation of to be overwritten method that due to inheritance structure otherwise would
   * require code duplication.
   *
   * @param conjugateEdgeSegment to extract geometry from
   * @return result of check
   */
  public static boolean hasGeometry(ConjugateEdgeSegment conjugateEdgeSegment) {
    return conjugateEdgeSegment.getParent().hasGeometry();
  }

  /**
   * Static implementation of to be overwritten method that due to inheritance structure otherwise would
   * require code duplication.
   *
   * @param conjugateEdgeSegment to extract length in km from
   * @return length in km
   */
  public static double getLengthKm(ConjugateEdgeSegment conjugateEdgeSegment){
    return conjugateEdgeSegment.getParent().getLengthKm();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateDirectedEdge getParent(){
    return (ConjugateDirectedEdge) super.getParent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeSegmentImpl shallowClone() {
    return new ConjugateEdgeSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateEdgeSegmentImpl deepClone() {
    return new ConjugateEdgeSegmentImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean validate() {
    return EdgeSegmentImpl.validate(this);
  }

  /**
   * Geometry is to be derived from underlying non-conjugate counterpart. Currently, this entails simply
   * requiring an up and downstream node geometry.
   *
   * @return true when up and downstream conjugate node geometry is available to construct on-the-fly geometry,
   * false otherwise
   */
  @Override
  public boolean hasGeometry() {
    return hasGeometry(this);
  }

  /**
   * Length is sum of length of its underlying two edge segments. Computed on-the-fly. If any edge is null, it is assumed
   * length may be set to 0km for that edge.
   *
   * @return on-the-fly length calculation
   */
  @Override
  public double getLengthKm() {
    return getLengthKm(this);
  }

}
