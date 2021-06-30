package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.DirectedGraphBuilder;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities with direction for a physical network simulation model
 * 
 * @author markr
 *
 */
public class DirectedGraphBuilderImpl implements DirectedGraphBuilder<DirectedVertex, DirectedEdge, EdgeSegment> {

  private static final Logger LOGGER = Logger.getLogger(DirectedGraphBuilderImpl.class.getCanonicalName());

  /** use graph builder impl for overlapping functionality */
  private final GraphBuilderImpl graphBuilder;

  /**
   * constructor
   * 
   * @param groupId to use for construction of elements
   */
  public DirectedGraphBuilderImpl(IdGroupingToken groupId) {
    this.graphBuilder = new GraphBuilderImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertexImpl createVertex() {
    return new DirectedVertexImpl(graphBuilder.getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  public DirectedEdgeImpl createEdge(Vertex vertexA, Vertex vertexB) throws PlanItException {
    return new DirectedEdgeImpl(graphBuilder.getIdGroupingToken(), vertexA, vertexB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentImpl createEdgeSegment(DirectedEdge parentEdge, boolean directionAB) throws PlanItException {
    return new EdgeSegmentImpl(graphBuilder.getIdGroupingToken(), parentEdge, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentImpl createEdgeSegment(boolean directionAB) throws PlanItException {
    return new EdgeSegmentImpl(graphBuilder.getIdGroupingToken(), directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupToken) {
    graphBuilder.setIdGroupingToken(groupToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return graphBuilder.getIdGroupingToken();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Edges<? extends DirectedEdge> edges) {
    graphBuilder.recreateIds(edges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<? extends DirectedVertex> vertices) {
    graphBuilder.recreateIds(vertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <X extends EdgeSegment> void recreateIds(EdgeSegments<X> edgeSegments) {
    if (edgeSegments instanceof EdgeSegmentsImpl<?>) {
      /* remove gaps by simply resetting and recreating all edge segment ids */
      IdGenerator.reset(getIdGroupingToken(), EdgeSegment.class);

      for (EdgeSegment edgeSegment : edgeSegments) {
        if (edgeSegments instanceof EdgeSegmentImpl) {
          ((EdgeSegmentImpl) edgeSegment).setId(EdgeSegmentImpl.generateEdgeSegmentId(getIdGroupingToken()));
        } else {
          LOGGER.severe("expected the edge segment implementation to be compatible with graph builder, this is not the case: unable to update ids");
        }
      }

      ((EdgeSegmentsImpl<?>) edgeSegments).updateIdMapping();
    } else {
      LOGGER.severe("expected the edge segments implementation to be compatible with graph builder, this is not the case: unable to update ids");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeImpl createUniqueCopyOf(DirectedEdge edgeToCopy) {
    if (edgeToCopy instanceof DirectedEdgeImpl) {
      return (DirectedEdgeImpl) this.graphBuilder.createUniqueCopyOf(edgeToCopy);
    } else {
      LOGGER.severe("expected the edge to be compatible with directed graph builder, this is not the case: unable to create unique copy");
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentImpl createUniqueCopyOf(EdgeSegment edgeSegmentToCopy, DirectedEdge newParentEdge) {
    if (!(edgeSegmentToCopy instanceof EdgeSegmentImpl)) {
      LOGGER.severe("expected the edge segment to be compatible with directed graph builder, this is not the case: unable to create unique copy");
      return null;
    }
    /* shallow copy as is */
    EdgeSegmentImpl copy = (EdgeSegmentImpl) edgeSegmentToCopy.clone();
    /* make unique copy by updating id */
    copy.setId(EdgeSegmentImpl.generateEdgeSegmentId(getIdGroupingToken()));
    /* update parent edge */
    copy.setParentEdge(newParentEdge);
    return copy;
  }

}
