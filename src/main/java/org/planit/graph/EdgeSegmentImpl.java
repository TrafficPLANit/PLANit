package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.IdSetter;

/**
 * EdgeSegment represents an edge in a particular (single) direction. Each edge has either one or two edge segments where each edge segment may have a more detailed geography than
 * its parent link (which represents both directions via a centreline)
 *
 * This class is now abstract. It is extended by LinkSegment (physical links) and Connectoid (virtual links).
 *
 * @author markr
 *
 */
public abstract class EdgeSegmentImpl implements EdgeSegment, IdSetter<Long> {

  /** generated UID */
  private static final long serialVersionUID = -6521489123632246969L;

  /**
   * unique internal identifier
   */
  protected long id;

  /**
   * segment's parent edge
   */
  protected final Edge parentEdge;

  /**
   * the upstreamVertex of the edge segment
   */
  protected DirectedVertex upstreamVertex;

  /**
   * The downstream vertex of this edge segment
   */
  protected DirectedVertex downstreamVertex;

  /**
   * The external Id for this link segment type
   */
  protected Object externalId;

  /**
   * Generate unique edge segment id
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @return id id of this EdgeSegment
   */
  protected static long generateEdgeSegmentId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, EdgeSegment.class);
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
  protected EdgeSegmentImpl(final IdGroupingToken groupId, final Edge parentEdge, final boolean directionAB) throws PlanItException {
    this.id = generateEdgeSegmentId(groupId);
    this.parentEdge = parentEdge;
    if (!(parentEdge.getVertexA() instanceof DirectedVertex && parentEdge.getVertexB() instanceof DirectedVertex)) {
      throw new PlanItException(String.format("parent edges (id:%d) vertices do not support directed edge segments, they must be of type DirectedVertex", parentEdge.getId()));
    }
    this.upstreamVertex = (DirectedVertex) (directionAB ? parentEdge.getVertexA() : parentEdge.getVertexB());
    this.downstreamVertex = (DirectedVertex) (directionAB ? parentEdge.getVertexB() : parentEdge.getVertexA());
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeVertex(Vertex vertex) {
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
   * Unique id of the edge segment
   *
   * @return id
   */
  @Override
  public long getId() {
    return this.id;
  }
  
  @Override
  public void overwriteId(Long id) {
      this.id = id;
  }  

  /**
   * parent edge of the segment
   *
   * @return parentEdge
   */
  @Override
  public Edge getParentEdge() {
    return this.parentEdge;
  }

  /**
   * set external id of the instance. Note that this id need not be unique (unlike regular id)
   * 
   * @param externalId for the edge segment
   */
  @Override
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  /**
   * Does the instance have an external id
   * 
   * @return true when available, false otherwise
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * Get external id of the instance. Note that this id need not be unique (unlike regular id)
   * 
   * @return externalId
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

}
