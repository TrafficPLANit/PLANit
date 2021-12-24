package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntityImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
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

  // Public

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
    if (parentEdge == null) {
      LOGGER.warning(String.format("parent edge missing on edge segment (id:%d externalId:%s)", getId(), getExternalId()));
      return false;
    }

    if (getUpstreamVertex() == null) {
      LOGGER.warning(String.format("upstream vertex missing on edge segment (id:%d externalId:%s)", getId(), getExternalId()));
      return false;
    }

    if (getDownstreamVertex() == null) {
      LOGGER.warning(String.format("downstream vertex missing on edge segment (id:%d externalId:%s)", getId(), getExternalId()));
      return false;
    }

    if (getParentEdge().getVertexA() == getUpstreamVertex() && isDirectionAb()) {
      if (getParentEdge().getEdgeSegmentAb() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", getId(), getExternalId()));
        return false;
      }
      if (!getParentEdge().getEdgeSegmentAb().equals(this)) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", getId(), getExternalId()));
        return false;
      }
    }
    if (getParentEdge().getVertexB() == getUpstreamVertex() && !isDirectionAb()) {
      if (getParentEdge().getEdgeSegmentBa() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", getId(), getExternalId()));
        return false;
      }

      if (!getParentEdge().getEdgeSegmentBa().equals(this)) {
        LOGGER.warning(String.format("edge segment B->A on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", getId(), getExternalId()));
        return false;
      }
    } else {
      LOGGER.warning(String.format("edge segment direction inconsistent with its vertices (id:%d externalId:%s) should be the same but it is not", getId(), getExternalId()));
    }

    if (getParentEdge().getVertexA().equals(getUpstreamVertex())) {
      if (!getParentEdge().getVertexB().equals(getDownstreamVertex())) {
        LOGGER.warning(String.format("edge segment (id:%d externalId:%s) vertices do not match with parent edge vertices", getId(), getExternalId()));
        return false;
      }
    } else {
      if (!getParentEdge().getVertexB().equals(getUpstreamVertex()) || !getParentEdge().getVertexA().equals(getDownstreamVertex())) {
        LOGGER.warning(String.format("edge segment (id:%d externalId:%s) vertices do not match with parent edge vertices", getId(), getExternalId()));
        return false;
      }
    }

    return true;
  }

}
