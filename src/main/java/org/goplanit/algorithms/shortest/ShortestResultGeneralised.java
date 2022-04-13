package org.goplanit.algorithms.shortest;

import java.util.function.Function;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;

/**
 * Base class for shortestXResult classes
 * 
 * @author markr
 *
 */
public abstract class ShortestResultGeneralised implements ShortestResult{

  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;

  /** reflects the active type */
  protected final ShortestSearchType searchType;

  /** depending on configuration this function collects vertex at desired edge segment extremity */
  protected Function<EdgeSegment, DirectedVertex> getVertexAtExtreme;
  
  /**
   * Determine the start and end vertex to use for constructing the path depending on the search type used in the preceding shortest path search
   * 
   * @param origin      of to be constructed path
   * @param destination of to be constructed path
   * @return order in which origin and destination are to be encountered when traversing search results
   */
  protected Pair<DirectedVertex, DirectedVertex> getStartEndVertexForResultTraversal(DirectedVertex origin, DirectedVertex destination) {
    if (isInverted()) {
      return Pair.of(origin, destination);
    } else {
      /* regular direction where results are traversed from destination back to origin */
      return Pair.of(destination, origin);
    }
  }
  
  /**
   * Constructor only to be used by shortest X algorithms
   * 
   * @param vertexMeasuredCost      measured costs to get to the vertex (by id)
   * @param searchType              used (one-to-all, all-to-one, etc)
   */
  protected ShortestResultGeneralised(double[] vertexMeasuredCost, ShortestSearchType searchType) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.searchType = searchType;

    /* search direction for creating paths in opposite direction as compared to shortest path search itself */
    this.getVertexAtExtreme = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(searchType, true /* invert */ );
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override  
  public DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment) {
    return this.getVertexAtExtreme.apply(edgeSegment);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getSearchType() {
    return searchType;
  }  
}
