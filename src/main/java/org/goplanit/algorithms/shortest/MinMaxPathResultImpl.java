package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

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
  private ShortestPathResultGeneralised minPathResult;

  /**
   * tracking max path results
   */
  private ShortestPathResultGeneralised maxPathResult;

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
    this.minPathResult = new ShortestPathResultGeneralised(minVertexCost, minCostBackwardEdgeSegments, ShortestSearchType.ONE_TO_ALL);
    this.maxPathResult = new ShortestPathResultGeneralised(maxVertexCost, maxCostBackwardEdgeSegments, ShortestSearchType.ONE_TO_ALL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMinPathState(boolean flag) {
    this.minPathState = flag;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends SimpleDirectedPath> T createPath(DirectedPathFactory<T> pathFactory, DirectedVertex origin, DirectedVertex destination) {
    return minPathState ? minPathResult.createPath(pathFactory, origin, destination) : maxPathResult.createPath(pathFactory, origin, destination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex) {
    return minPathState ? minPathResult.getNextEdgeSegmentForVertex(vertex) : maxPathResult.getNextEdgeSegmentForVertex(vertex);
  }

  @Override
  public DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment) {
    return minPathState ? minPathResult.getNextVertexForEdgeSegment(edgeSegment) : maxPathResult.getNextVertexForEdgeSegment(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCostOf(Vertex vertex) {
    return minPathState ? minPathResult.getCostOf(vertex) : maxPathResult.getCostOf(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getSearchType() {
    return minPathResult.searchType;
  }

}
