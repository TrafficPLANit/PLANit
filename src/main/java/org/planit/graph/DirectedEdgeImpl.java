package org.planit.graph;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

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

  /**
   * Edge segment A to B direction
   */
  protected EdgeSegment edgeSegmentAB = null;
  /**
   * Edge segment B to A direction
   */
  protected EdgeSegment edgeSegmentBA = null;

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param lengthKm length of the link in km
   * @throws PlanItException thrown if there is an error
   */
  protected DirectedEdgeImpl(final IdGroupingToken groupId, final DirectedVertex vertexA, final DirectedVertex vertexB, final double lengthKm) throws PlanItException {
    super(groupId, vertexA, vertexB, lengthKm);
  }

  /**
   * Copy Constructor
   * 
   * @param directedEdgeImpl top copy
   */
  protected DirectedEdgeImpl(DirectedEdgeImpl directedEdgeImpl) {
    // TODO Auto-generated constructor stub
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment registerEdgeSegment(final EdgeSegment edgeSegment, final boolean directionAB) throws PlanItException {
    PlanItException.throwIf(edgeSegment.getParentEdge().getId() != getId(), "inconsistency between link segment parent link and link it is being registered on");

    final EdgeSegment currentEdgeSegment = directionAB ? edgeSegmentAB : edgeSegmentBA;
    if (directionAB) {
      this.edgeSegmentAB = edgeSegment;
    } else {
      this.edgeSegmentBA = edgeSegment;
    }
    return currentEdgeSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentAb() {
    return edgeSegmentAB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentBa() {
    return edgeSegmentBA;
  }

  /**
   * Same as @{code EdgeImpl} only now we also update the references to the edge segments if indicated
   * 
   * @param vertextoReplace     the vertex to realpce
   * @param vertexToReplaceWith the vertex to replace with
   * @param updateVertexEdges   when true register and unregister the changes on the relevant vertices for both edges and edge segments, when false not
   * @throws PlanItException thrown if error
   */
  @Override
  public boolean replace(Vertex vertexToReplace, Vertex vertexToReplaceWith, boolean updateVertexEdges) throws PlanItException {
    boolean isReplaced = super.replace(vertexToReplace, vertexToReplaceWith, updateVertexEdges);

    if (vertexToReplace instanceof DirectedVertex && vertexToReplaceWith instanceof DirectedVertex) {
      /* replace vertices on edge segments */
      if (hasEdgeSegmentAb()) {
        getEdgeSegmentAb().replace((DirectedVertex) vertexToReplace, (DirectedVertex) vertexToReplaceWith);
      }
      if (hasEdgeSegmentBa()) {
        getEdgeSegmentBa().replace((DirectedVertex) vertexToReplace, (DirectedVertex) vertexToReplaceWith);
        ((DirectedVertex) vertexToReplace).removeEdgeSegment(getEdgeSegmentBa());
      }

      /* replace edge segments on vertex */
      if (updateVertexEdges) {
        if (hasEdgeSegmentAb()) {
          ((DirectedVertex) vertexToReplace).removeEdgeSegment(getEdgeSegmentAb());
          ((DirectedVertex) vertexToReplaceWith).addEdgeSegment(getEdgeSegmentAb());
        }
        if (hasEdgeSegmentBa()) {
          ((DirectedVertex) vertexToReplace).removeEdgeSegment(getEdgeSegmentBa());
          ((DirectedVertex) vertexToReplaceWith).addEdgeSegment(getEdgeSegmentBa());
        }

      }
    } else {
      throw new PlanItException("unable to replace vertex on directed edge, provided vertices are not directed");
    }

    return isReplaced;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdge clone() throws CloneNotSupportedException {
    return new DirectedEdgeImpl(this);
  }

}
