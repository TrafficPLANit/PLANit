package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraphBuilder;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities with direction for a physical network simulation model
 * 
 * @author markr
 *
 */
public class DirectedGraphBuilderImpl implements DirectedGraphBuilder<DirectedVertexImpl, DirectedEdgeImpl, EdgeSegmentImpl> {

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
  @Override
  public DirectedEdgeImpl createEdge(DirectedVertexImpl vertexA, DirectedVertexImpl vertexB) throws PlanItException {
    return new DirectedEdgeImpl(graphBuilder.getIdGroupingToken(), vertexA, vertexB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentImpl createEdgeSegment(DirectedEdgeImpl parentEdge, boolean directionAB) throws PlanItException {
    return new EdgeSegmentImpl(graphBuilder.getIdGroupingToken(), parentEdge, directionAB);
  }

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
  public void recreateIds(Edges<DirectedEdgeImpl> edges) {
    graphBuilder.recreateIds(edges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<DirectedVertexImpl> vertices) {
    graphBuilder.recreateIds(vertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(EdgeSegments<EdgeSegmentImpl> edgeSegments) {
    if (edgeSegments instanceof EdgeSegmentsImpl<?>) {
      /* remove gaps by simply resetting and recreating all edge segment ids */
      IdGenerator.reset(getIdGroupingToken(), EdgeSegment.class);

      for (EdgeSegmentImpl edgeSegment : edgeSegments) {
        edgeSegment.setId(EdgeSegmentImpl.generateEdgeSegmentId(getIdGroupingToken()));
      }

      ((EdgeSegmentsImpl<?>) edgeSegments).updateIdMapping();
    } else {
      LOGGER.severe("expected the Edge segment implementation to be compatible with graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedEdgeImpl createUniqueCopyOf(DirectedEdgeImpl edgeToCopy) {
    return (DirectedEdgeImpl) this.graphBuilder.createUniqueCopyOf(edgeToCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentImpl createUniqueCopyOf(EdgeSegmentImpl edgeSegmentToCopy, DirectedEdgeImpl newParentEdge) {
    /* shallow copy as is */
    EdgeSegmentImpl copy = (EdgeSegmentImpl) edgeSegmentToCopy.clone();
    /* make unique copy by updating id */
    copy.setId(EdgeSegmentImpl.generateEdgeSegmentId(getIdGroupingToken()));
    /* update parent edge */
    copy.setParentEdge(newParentEdge);
    return copy;
  }

}
