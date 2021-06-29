package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * EdgeSegment represents an edge in a particular (single) direction. Each edge has either one or two edge segments where each edge segment may have a more detailed geography than
 * its parent link (which represents both directions via a centre line)
 *
 * @author markr
 *
 */
public class EdgeSegmentImpl extends ExternalIdAbleImpl implements EdgeSegment {

  /** generated UID */
  private static final long serialVersionUID = -6521489123632246969L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentImpl.class.getCanonicalName());

  /**
   * the upstreamVertex of the edge segment TODO: remove instead store direction and derive vertex from parent if needed
   */
  private DirectedVertex upstreamVertex;

  /**
   * The downstream vertex of this edge segment TODO: remove instead store direction and derive vertex from parent if needed
   */
  private DirectedVertex downstreamVertex;

  /**
   * segment's parent edge
   */
  private DirectedEdge parentEdge;

  /**
   * Generate unique edge segment id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id id of this EdgeSegment
   */
  protected static long generateEdgeSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, EdgeSegment.class);
  }

  /**
   * set id of edge segment and expose method to package
   * 
   * @param id to set
   */
  protected void setId(Long id) {
    super.setId(id);
  }

  // Public

  /**
   * Constructor
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentEdge  parent edge of segment
   * @param directionAB direction of travel
   * @throws PlanItException thrown when parent edge's vertices are incompatible with directional edge segments
   */
  protected EdgeSegmentImpl(final IdGroupingToken groupId, final DirectedEdge parentEdge, final boolean directionAB) throws PlanItException {
    this(groupId, directionAB);
    setParentEdge(parentEdge);
  }

  /**
   * Constructor (without setting parent edge)
   *
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param directionAB direction of travel
   * @throws PlanItException thrown when parent edge's vertices are incompatible with directional edge segments
   */
  protected EdgeSegmentImpl(final IdGroupingToken groupId, final boolean directionAB) throws PlanItException {
    super(generateEdgeSegmentId(groupId));

    if (!(parentEdge.getVertexA() instanceof DirectedVertex && parentEdge.getVertexB() instanceof DirectedVertex)) {
      throw new PlanItException(String.format("parent edges (id:%d) vertices do not support directed edge segments, they must be of type DirectedVertex", parentEdge.getId()));
    }
    setUpstreamVertex((DirectedVertex) (directionAB ? parentEdge.getVertexA() : parentEdge.getVertexB()));
    setDownstreamVertex((DirectedVertex) (directionAB ? parentEdge.getVertexB() : parentEdge.getVertexA()));
  }

  /**
   * Copy constructor
   * 
   * @param edgeSegmentImpl to copy
   */
  protected EdgeSegmentImpl(EdgeSegmentImpl edgeSegmentImpl) {
    super(edgeSegmentImpl);
    setParentEdge(edgeSegmentImpl.getParentEdge());
    setUpstreamVertex(edgeSegmentImpl.getUpstreamVertex());
    setDownstreamVertex(edgeSegmentImpl.getDownstreamVertex());
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return idHashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    return idEquals(obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remove(DirectedVertex vertex) {
    if (vertex != null) {
      if (getUpstreamVertex() != null && getUpstreamVertex().getId() == vertex.getId()) {
        this.upstreamVertex = null;
        return true;
      } else if (getDownstreamVertex() != null && getDownstreamVertex().getId() == vertex.getId()) {
        this.downstreamVertex = null;
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDownstreamVertex(DirectedVertex downstreamVertex) {
    this.downstreamVertex = downstreamVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUpstreamVertex(DirectedVertex upstreamVertex) {
    this.upstreamVertex = upstreamVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getUpstreamVertex() {
    return upstreamVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getDownstreamVertex() {
    return downstreamVertex;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectionAb() {
    return getParentEdge().hasEdgeSegmentAb() && getParentEdge().getEdgeSegmentAb().getId() == this.getId();
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
  public void setParentEdge(DirectedEdge parentEdge) {
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
  public EdgeSegment clone() {
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

    if (getParentEdge().getVertexA() == getUpstreamVertex()) {
      if (getParentEdge().getEdgeSegmentAb() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", getId(), getExternalId()));
        return false;
      }
      if (!getParentEdge().getEdgeSegmentAb().equals(this)) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", getId(), getExternalId()));
        return false;
      }
    }
    if (getParentEdge().getVertexB() == getUpstreamVertex()) {
      if (getParentEdge().getEdgeSegmentBa() == null) {
        LOGGER.warning(String.format("edge segment A->B on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is null", getId(), getExternalId()));
        return false;
      }

      if (!getParentEdge().getEdgeSegmentBa().equals(this)) {
        LOGGER.warning(String.format("edge segment B->A on parent edge of this edge segment (id:%d externalId:%s) should be the same but it is not", getId(), getExternalId()));
        return false;
      }
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
