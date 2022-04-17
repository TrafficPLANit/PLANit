package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.EdgeImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Edge class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry additional information for each
 * particular direction of the edge.
 *
 * @author markr
 *
 */
public class DirectedEdgeImpl<V extends DirectedVertex, ES extends EdgeSegment> extends EdgeImpl<V> implements DirectedEdge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(DirectedEdgeImpl.class.getCanonicalName());

  /**
   * Edge segment A to B direction
   */
  private ES edgeSegmentAb = null;
  /**
   * Edge segment B to A direction
   */
  private ES edgeSegmentBa = null;

  /**
   * set edge segment from B to A
   * 
   * @param edgeSegmentBa to set
   */
  protected void setEdgeSegmentBa(ES edgeSegmentBa) {
    this.edgeSegmentBa = edgeSegmentBa;
  }

  /**
   * set edge segment from A to B
   * 
   * @param edgeSegmentAb to set
   */
  protected void setEdgeSegmentAb(ES edgeSegmentAb) {
    this.edgeSegmentAb = edgeSegmentAb;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected DirectedEdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB) {
    super(groupId, vertexA, vertexB);
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param lengthKm length of the link in km
   */
  protected DirectedEdgeImpl(final IdGroupingToken groupId, final V vertexA, final V vertexB, final double lengthKm) {
    super(groupId, vertexA, vertexB, lengthKm);
  }

  /**
   * Copy Constructor. Edge segments are shallow copied and point to the passed in edge as their parent So additional effort is needed to make the new edge usable
   * 
   * @param directedEdgeImpl to copy
   */
  protected DirectedEdgeImpl(DirectedEdgeImpl<V, ES> directedEdgeImpl) {
    super(directedEdgeImpl);
    setEdgeSegmentAb(directedEdgeImpl.getEdgeSegmentAb());
    setEdgeSegmentBa(directedEdgeImpl.getEdgeSegmentBa());
  }

  // Public

  // Protected

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public ES registerEdgeSegment(final EdgeSegment edgeSegment, final boolean directionAB) {
    if (edgeSegment.getParent() == null) {
      edgeSegment.setParent(this);
    }

    if (edgeSegment.getParent() != this) {
      LOGGER.warning("Inconsistency between link segment's parent link and link it is being registered on");
      return null;
    }

    final ES overwrittenEdgeSegment = directionAB ? getEdgeSegmentAb() : getEdgeSegmentBa();
    if (directionAB) {
      setEdgeSegmentAb((ES) edgeSegment);
    } else {
      setEdgeSegmentBa((ES) edgeSegment);
    }
    return overwrittenEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES getEdgeSegmentAb() {
    return edgeSegmentAb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES getEdgeSegmentBa() {
    return edgeSegmentBa;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeImpl<V, ES> clone() {
    return new DirectedEdgeImpl<V, ES>(this);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void replace(EdgeSegment edgeSegmentToReplace, EdgeSegment edgeSegmentToReplaceWith) {
    if (edgeSegmentToReplace != null) {
      if (hasEdgeSegmentAb() && getEdgeSegmentAb().getId() == edgeSegmentToReplace.getId()) {
        setEdgeSegmentAb((ES) edgeSegmentToReplaceWith);
      } else if (hasEdgeSegmentBa() && getEdgeSegmentBa().getId() == edgeSegmentToReplace.getId()) {
        setEdgeSegmentBa((ES) edgeSegmentToReplaceWith);
      } else {
        LOGGER.warning("provided edge segment to replace is not known on the directed edge");
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES removeEdgeSegmentAb() {
    var removedEdgeSegment = edgeSegmentAb;
    setEdgeSegmentAb(null);
    return removedEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ES removeEdgeSegmentBa() {
    var removedEdgeSegment = edgeSegmentBa;
    setEdgeSegmentBa(null);
    return removedEdgeSegment;
  }

}
