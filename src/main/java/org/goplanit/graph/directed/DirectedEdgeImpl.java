package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.EdgeImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Edge class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry additional information for each
 * particular direction of the edge.
 *
 * @author markr
 *
 */
public class DirectedEdgeImpl extends EdgeImpl implements DirectedEdge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(DirectedEdgeImpl.class.getCanonicalName());

  /**
   * Edge segment A to B direction
   */
  private EdgeSegment edgeSegmentAb = null;
  /**
   * Edge segment B to A direction
   */
  private EdgeSegment edgeSegmentBa = null;

  /**
   * set edge segment from B to A
   * 
   * @param edgeSegmentBa to set
   */
  protected void setEdgeSegmentBa(EdgeSegment edgeSegmentBa) {
    this.edgeSegmentBa = edgeSegmentBa;
  }

  /**
   * set edge segment from A to B
   * 
   * @param edgeSegmentAb to set
   */
  protected void setEdgeSegmentAb(EdgeSegment edgeSegmentAb) {
    this.edgeSegmentAb = edgeSegmentAb;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   */
  protected DirectedEdgeImpl(final IdGroupingToken groupId, final DirectedVertex vertexA, final DirectedVertex vertexB) {
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
  protected DirectedEdgeImpl(final IdGroupingToken groupId, final DirectedVertex vertexA, final DirectedVertex vertexB, final double lengthKm) {
    super(groupId, vertexA, vertexB, lengthKm);
  }

  /**
   * Copy Constructor. Edge segments are shallow copied and point to the passed in edge as their parent So additional effort is needed to make the new edge usable
   * 
   * @param directedEdgeImpl to copy
   */
  protected DirectedEdgeImpl(DirectedEdgeImpl directedEdgeImpl) {
    super(directedEdgeImpl);
    setEdgeSegmentAb(directedEdgeImpl.getEdgeSegmentAb());
    setEdgeSegmentBa(directedEdgeImpl.getEdgeSegmentBa());
  }

  // Public

  // Protected

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getVertexA() {
    return (DirectedVertex) super.getVertexA();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getVertexB() {
    return (DirectedVertex) super.getVertexB();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment registerEdgeSegment(final EdgeSegment edgeSegment, final boolean directionAB) {
    if (edgeSegment.getParentEdge() == null) {
      edgeSegment.setParent(this);
    }

    if (edgeSegment.getParentEdge() != this) {
      LOGGER.warning("Inconsistency between link segment's parent link and link it is being registered on");
      return null;
    }

    final EdgeSegment overwrittenEdgeSegment = directionAB ? getEdgeSegmentAb() : getEdgeSegmentBa();
    if (directionAB) {
      setEdgeSegmentAb(edgeSegment);
    } else {
      setEdgeSegmentBa(edgeSegment);
    }
    return overwrittenEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentAb() {
    return edgeSegmentAb;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentBa() {
    return edgeSegmentBa;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeImpl clone() {
    return new DirectedEdgeImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void replace(EdgeSegment edgeSegmentToReplace, EdgeSegment edgeSegmentToReplaceWith) {
    if (edgeSegmentToReplace != null) {
      if (hasEdgeSegmentAb() && getEdgeSegmentAb().getId() == edgeSegmentToReplace.getId()) {
        setEdgeSegmentAb(edgeSegmentToReplaceWith);
      } else if (hasEdgeSegmentBa() && getEdgeSegmentBa().getId() == edgeSegmentToReplace.getId()) {
        setEdgeSegmentBa(edgeSegmentToReplaceWith);
      } else {
        LOGGER.warning("provided edge segment to replace is not known on the directed edge");
      }
    }
  }

}
