package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * EdgeSegment represents an edge in a particular (single) direction. Each edge has either one or two edge segments where each edge segment may have a more detailed geography than
 * its parent link (which represents both directions via a centreline)
 *
 * This class is now abstract. It is extended by LinkSegment (physical links) and Connectoid (virtual links).
 *
 * @author markr
 *
 */
public class EdgeSegmentImpl implements EdgeSegment {

  /** generated UID */
  private static final long serialVersionUID = -6521489123632246969L;

  /**
   * unique internal identifier
   */
  private long id;

  /**
   * the upstreamVertex of the edge segment
   */
  private DirectedVertex upstreamVertex;

  /**
   * The downstream vertex of this edge segment
   */
  private DirectedVertex downstreamVertex;

  /**
   * segment's parent edge
   */
  private DirectedEdge parentEdge;

  /**
   * The external Id for this link segment type
   */
  private Object externalId;

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
   * set id of edge segment
   * 
   * @param id to set
   */
  protected void setId(Long id) {
    this.id = id;
  }

  /**
   * set the downstream vertex
   * 
   * @param downstreamVertex to set
   */
  protected void setDownstreamVertex(DirectedVertex downstreamVertex) {
    this.downstreamVertex = downstreamVertex;
  }

  /**
   * set the upstream vertex
   * 
   * @param upstreamVertex to set
   */
  protected void setUpstreamVertex(DirectedVertex upstreamVertex) {
    this.upstreamVertex = upstreamVertex;
  }

  /**
   * set the parent edge
   * 
   * @param parentEdge to set
   */
  protected void setParentEdge(DirectedEdge parentEdge) {
    this.parentEdge = parentEdge;
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
    setId(generateEdgeSegmentId(groupId));
    setParentEdge(parentEdge);

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
    setId(edgeSegmentImpl.getId());
    setExternalId(edgeSegmentImpl.getExternalId());
    setParentEdge(edgeSegmentImpl.getParentEdge());
    setUpstreamVertex(edgeSegmentImpl.getUpstreamVertex());
    setDownstreamVertex(edgeSegmentImpl.getDownstreamVertex());
  }

  // Public

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
  public long getId() {
    return this.id;
  }

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
  public void removeParentEdge() {
    this.parentEdge = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getExternalId() {
    return externalId;
  }

  /**
   * compare based on edge segment id
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final EdgeSegment o) {
    return (int) (id - o.getId());
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
   */
  @Override
  public boolean replace(DirectedVertex vertexToReplace, DirectedVertex vertexToReplaceWith) {
    boolean vertexReplaced = false;

    /* replace vertices on edge segment */
    if (vertexToReplaceWith != null) {
      if (getUpstreamVertex() != null && vertexToReplace.getId() == getUpstreamVertex().getId()) {
        remove(vertexToReplace);
        setUpstreamVertex(vertexToReplaceWith);
        vertexReplaced = true;
      } else if (getDownstreamVertex() != null && vertexToReplace.getId() == getDownstreamVertex().getId()) {
        remove(vertexToReplace);
        setDownstreamVertex(vertexToReplaceWith);
        vertexReplaced = true;
      }
    }
    return vertexReplaced;
  }

}
