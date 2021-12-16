package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.path.DirectedPathFactory;

/**
 * Implementation of the MinMaxPathResult interface
 * 
 * @author markr
 *
 */
public class MinMaxPathResultImpl implements MinMaxPathResult {

  /**
   * Track the state regarding whether or not to return min or max path information
   */
  private boolean minPathState;

  /**
   * tracking min path results
   */
  private ShortestPathResult minPathResult;

  /**
   * tracking max path results
   */
  private ShortestPathResult maxPathResult;

  /**
   * Constructor
   * 
   * @param minVertexCost               found
   * @param minCostBackwardEdgeSegments found
   * @param maxVertexCost               found
   * @param maxCostBackwardEdgeSegments found
   */
  protected MinMaxPathResultImpl(double[] minVertexCost, EdgeSegment[] minCostBackwardEdgeSegments, double[] maxVertexCost, EdgeSegment[] maxCostBackwardEdgeSegments) {
    this.minPathState = true;
    this.minPathResult = new ShortestPathResultImpl(minVertexCost, minCostBackwardEdgeSegments);
    this.maxPathResult = new ShortestPathResultImpl(maxVertexCost, maxCostBackwardEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(DirectedPathFactory pathFactory, DirectedVertex origin, DirectedVertex destination) {
    return minPathState ? minPathResult.createPath(pathFactory, origin, destination) : maxPathResult.createPath(pathFactory, origin, destination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getIncomingEdgeSegmentForVertex(Vertex vertex) {
    return minPathState ? minPathResult.getIncomingEdgeSegmentForVertex(vertex) : maxPathResult.getIncomingEdgeSegmentForVertex(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostToReach(Vertex vertex) {
    return minPathState ? minPathResult.getCostToReach(vertex) : maxPathResult.getCostToReach(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMinPathState(boolean flag) {
    this.minPathState = flag;
  }

}
