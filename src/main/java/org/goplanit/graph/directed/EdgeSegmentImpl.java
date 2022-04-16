package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * EdgeSegment represents an edge in a particular (single) direction. Each edge has either one or two edge segments where each edge segment may have a more detailed geography than
 * its parent link (which represents both directions via a centre line)
 *
 * @author markr
 *
 */
public class EdgeSegmentImpl extends GraphEntityImpl implements EdgeSegment {

  /** generated UID */
  private static final long serialVersionUID = -6521489123632246969L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentImpl.class.getCanonicalName());

  /**
   * Store the direction of this edge segment in relation to its parent edge
   */
  private boolean directionAb;

  /**
   * segment's parent edge
   */
  private DirectedEdge parentEdge;

  // Protected

  /**
   * Validate given edge segment on validity
   * 
   * @param edgeSegment to validate
   * @return true when valid, false when not
   */
  protected static final boolean validate(EdgeSegment edgeSegment) {
    if (edgeSegment.getParentEdge() == null) {
      LOGGER.warning(String.format("parent edge missing on edge segment (id:%d externalId:%s)", edgeSegment.getId(), edgeSegment.getExternalId()));
      return false;
    }

    if (edgeSegment.getUpstreamVertex() == null) {
      LOGGER.warning(String.format("upstream vertex missing on edge segment (id:%d externalId:%s)", edgeSegment.getId(), edgeSegment.getExternalId()));
      return false;
    }

    if (edgeSegment.getDownstreamVertex() == null) {
      LOGGER.warning(String.format("downstream vertex missing on edge segment (id:%d externalId:%s)", edgeSegment.getId(), edgeSegment.getExternalId()));
      return false;
    }

    if (edgeSegment.getParentEdge().getVertexA() == edgeSegment.getUpstreamVertex() && edgeSegment.isDirectionAb()) {
      if (edgeSegment.getParentEdge().getEdgeSegmentAb() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", edgeSegment.getId(),
            edgeSegment.getExternalId()));
        return false;
      }
      if (!edgeSegment.getParentEdge().getEdgeSegmentAb().equals(edgeSegment)) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", edgeSegment.getId(),
            edgeSegment.getExternalId()));
        return false;
      }
    }
    if (edgeSegment.getParentEdge().getVertexB() == edgeSegment.getUpstreamVertex() && !edgeSegment.isDirectionAb()) {
      if (edgeSegment.getParentEdge().getEdgeSegmentBa() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", edgeSegment.getId(),
            edgeSegment.getExternalId()));
        return false;
      }

      if (!edgeSegment.getParentEdge().getEdgeSegmentBa().equals(edgeSegment)) {
        LOGGER.warning(String.format("edge segment B->A on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", edgeSegment.getId(),
            edgeSegment.getExternalId()));
        return false;
      }
    } else {
      LOGGER.warning(String.format("edge segment direction inconsistent with its vertices (id:%d externalId:%s) should be the same but it is not", edgeSegment.getId(),
          edgeSegment.getExternalId()));
    }

    if (edgeSegment.getParentEdge().getVertexA().equals(edgeSegment.getUpstreamVertex())) {
      if (!edgeSegment.getParentEdge().getVertexB().equals(edgeSegment.getDownstreamVertex())) {
        LOGGER.warning(String.format("edge segment (id:%d externalId:%s) vertices do not match with parent edge vertices", edgeSegment.getId(), edgeSegment.getExternalId()));
        return false;
      }
    } else {
      if (!edgeSegment.getParentEdge().getVertexB().equals(edgeSegment.getUpstreamVertex())
          || !edgeSegment.getParentEdge().getVertexA().equals(edgeSegment.getDownstreamVertex())) {
        LOGGER.warning(String.format("edge segment (id:%d externalId:%s) vertices do not match with parent edge vertices", edgeSegment.getId(), edgeSegment.getExternalId()));
        return false;
      }
    }

    return true;
  }

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent edge of segment
   * @param directionAb direction of travel
   */
  protected EdgeSegmentImpl(final IdGroupingToken groupId, final DirectedEdge parentEdge, final boolean directionAb) {
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
  protected EdgeSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) {
    super(groupId, EDGE_SEGMENT_ID_CLASS);
  }

  /**
   * Copy constructor
   * 
   * @param edgeSegmentImpl to copy
   */
  protected EdgeSegmentImpl(EdgeSegmentImpl edgeSegmentImpl) {
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
  public DirectedEdge getParentEdge() {
    return this.parentEdge;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setParent(DirectedEdge parentEdge) {
    if (parentEdge == null) {
      LOGGER.warning(String.format("Parent edge is null, unable to set on edge segment (id: %d)", getId()));
      return;
    }
    this.parentEdge = parentEdge;
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
  public EdgeSegmentImpl clone() {
    return new EdgeSegmentImpl(this);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public boolean validate() {
    return EdgeSegmentImpl.validate(this);
  }

}
