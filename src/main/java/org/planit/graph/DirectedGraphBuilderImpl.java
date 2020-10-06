package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;

/**
 * Create network entities with direction for a physical network simulation model
 * 
 * @author markr
 *
 */
public class DirectedGraphBuilderImpl implements DirectedGraphBuilder<DirectedVertex, Edge, EdgeSegment> {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedGraphBuilderImpl.class.getCanonicalName());

  /** use graph builder impl for overlapping functionality */
  private final GraphBuilderImpl graphBuilder = new GraphBuilderImpl();

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex createVertex() {
    return new DirectedVertexImpl(graphBuilder.getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Edge createEdge(DirectedVertex vertexA, DirectedVertex vertexB, final double length) throws PlanItException {
    return graphBuilder.createEdge(vertexA, vertexB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment createEdgeSegment(Edge parentEdge, boolean directionAB) throws PlanItException {
    return new EdgeSegmentImpl(graphBuilder.getIdGroupingToken(), parentEdge, directionAB);
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

}
