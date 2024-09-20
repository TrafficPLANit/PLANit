package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.util.Deque;

/**
 * Implementation of the MinMaxPathResult interface
 * 
 * @author markr
 *
 */
public class MinMaxPathResultImpl implements MinMaxPathResult {

  /**
   * Track the state regarding whether to return min or max path information
   */
  private boolean minPathState;

  /**
   * tracking min path results
   */
  private final ShortestPathResultGeneralised minPathResult;

  /**
   * tracking max path results
   */
  private final ShortestPathResultGeneralised maxPathResult;

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
  public boolean isMinPathState() {
    return this.minPathState;
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
  public Deque<EdgeSegment> createRawPath(DirectedVertex origin, DirectedVertex destination) {
    return minPathState ? minPathResult.createRawPath(origin, destination) : maxPathResult.createRawPath(origin, destination);
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
  public double getCostToReach(Vertex vertex) {
    return minPathState ? minPathResult.getCostToReach(vertex) : maxPathResult.getCostToReach(vertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getSearchType() {
    return minPathResult.searchType;
  }

}
