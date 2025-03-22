package org.goplanit.algorithms.shortest;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
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

  /** root search vertex used for the underlying search carried out */
  private DirectedVertex rootSearchVertex;

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
   * @param rootSearchVertex             that was used for the search
   * @param minVertexCost               found
   * @param minCostBackwardEdgeSegments found
   * @param maxVertexCost               found
   * @param maxCostBackwardEdgeSegments found
   * @param numEdgeSegments  number of edge segments in network
   */
  protected MinMaxPathResultImpl(
      DirectedVertex rootSearchVertex,
      double[] minVertexCost,
      EdgeSegment[] minCostBackwardEdgeSegments,
      double[] maxVertexCost,
      EdgeSegment[] maxCostBackwardEdgeSegments,
      int numEdgeSegments) {

    this.minPathState = true;
    this.rootSearchVertex = rootSearchVertex;
        this.minPathResult = new ShortestPathResultGeneralised(
        rootSearchVertex, minVertexCost, minCostBackwardEdgeSegments, ShortestSearchType.ONE_TO_ALL, numEdgeSegments);
    this.maxPathResult = new ShortestPathResultGeneralised(
        rootSearchVertex, maxVertexCost, maxCostBackwardEdgeSegments, ShortestSearchType.ONE_TO_ALL, numEdgeSegments);
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
  public <T extends SimpleDirectedPath> T createPath(
          DirectedPathFactory<T> pathFactory, DirectedVertex origin, DirectedVertex destination) {
    return minPathState ?
            minPathResult.createPath(pathFactory, origin, destination):
            maxPathResult.createPath(pathFactory, origin, destination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Deque<EdgeSegment> createRawPath(DirectedVertex origin, DirectedVertex destination) {
    return minPathState ?
            minPathResult.createRawPath(origin, destination) : maxPathResult.createRawPath(origin, destination);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex) {
    return minPathState ?
            minPathResult.getNextEdgeSegmentForVertex(vertex) : maxPathResult.getNextEdgeSegmentForVertex(vertex);
  }

  @Override
  public UntypedACyclicSubGraph<?,?> createAndPopulateDirectedAcyclicSubGraphSpanningTree(IdGroupingToken idToken) {
    throw new PlanItRunTimeException("createDirectedAcyclicSubGraph not yet supported for min/max result");
  }

  @Override
  public <V extends DirectedVertex, E extends EdgeSegment> void  populateDirectedAcyclicSubGraphSpanningTree(
      UntypedACyclicSubGraph<V,E> dagToPopulate) {
    throw new PlanItRunTimeException("createDirectedAcyclicSubGraph not yet supported for min/max result");
  }

  @Override
  public DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment) {
    return minPathState ?
            minPathResult.getNextVertexForEdgeSegment(edgeSegment):
            maxPathResult.getNextVertexForEdgeSegment(edgeSegment);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedVertex getRootSearchVertex() {
    return rootSearchVertex;
  }

}
