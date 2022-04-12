package org.goplanit.algorithms.shortest;

import java.util.ArrayList;
import java.util.List;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Shortest bush algorithm.
 * 
 * Shortest bush algorithm is a one-to-all/all-to-one implementation (depending on configuration) of all (equal cost) implicit shortest bush comprising of all equal cost paths
 * based on the generalised costs on each link segment (edge). It is identical to Dijkstra's shortest path algorithm except that it creates a bush rooted at the start vertex
 * towards the destination vertex, where each node stores all its equal cheapest predecessor nodes from which the bush can be extracted when traversing it in reverse order.
 * <p>
 * when configured as one-to-all, the result is to be traversed in reverse direction (destination to origin) to obtian apth information, whereas in all-to-one form the result is
 * traversed from origin to destination (as the search itself is reversed in that case). In its current form, it assumes a macroscopic network and macroscopic link segments to
 * operate on
 * 
 * @author markr
 *
 */
public class ShortestBushGeneralised extends ShortestPathGeneralised implements ShortestBushOneToAll, ShortestBushAllToOne {

  /**
   * Track incoming edge segment(s) that are shortest for each vertex in this array. In case there is only a single shortest option, the object is an edge segment, otherwise it is
   * a collection of edge segments
   */
  protected Object[] nextEdgeSegments;

  /** track if most recent tested costs were equal, if so, any processed edge segment is to be added to existing shortest edge segments to complement the bush */
  protected boolean currEqualShortestCosts;

  /**
   * Test whether shortest bush means less or equal cost compared to existing cost, so cheaper and equal cost paths are considered. also store the costs so we can test if they were
   * equal or shorter in {@link #processShorterOrEqualIncomingEdgeSegment(EdgeSegment)}
   */
  private boolean isShorterOrEqual(double currShortestCostToVertex, double currComputedCostToVertex) {
    currEqualShortestCosts = false;
    if (Precision.smaller(currShortestCostToVertex, currComputedCostToVertex)) {
      return false;
    } else if (Precision.equal(currShortestCostToVertex, currComputedCostToVertex, Precision.EPSILON_15)) {
      currEqualShortestCosts = true;
    }
    return true;
  }

  /**
   * Whenever an incoming edge segment is considered shorter or equal, it should either be marked as the new incoming edge segment for the vertex (shorter), or supplement the
   * existing shortest segments present (equal). This is what this method does and it is supplied as consumer to the shortestAlgorithm base function
   * 
   * @param addNextEdgeSegment that is considered shorter or equal shortest for its reference vertex
   */
  @SuppressWarnings("unchecked")
  private void processShorterOrEqualIncomingEdgeSegment(EdgeSegment addNextEdgeSegment) {

    DirectedVertex nextSegmentVertex = this.getVertexAtExtreme.apply(addNextEdgeSegment);
    int nextSegmentRefVertexId = (int) nextSegmentVertex.getId();

    if (currEqualShortestCosts) {
      /* equal costs */
      var currentEligibleEdgeSegmentsVertex = nextEdgeSegments[(int) nextSegmentRefVertexId];

      if (currentEligibleEdgeSegmentsVertex instanceof EdgeSegment) {
        /* change from single segment to collection */
        var list = new ArrayList<EdgeSegment>(2);
        list.add((EdgeSegment) currentEligibleEdgeSegmentsVertex);
        list.add(addNextEdgeSegment);
        nextEdgeSegments[nextSegmentRefVertexId] = list;
      } else {
        /* add to existing collection */
        ((List<EdgeSegment>) nextEdgeSegments[nextSegmentRefVertexId]).add(addNextEdgeSegment);
      }

    } else {
      /* cheapest */

      nextEdgeSegments[nextSegmentRefVertexId] = addNextEdgeSegment;
    }
  }

  /**
   * Constructor for an edge cost based algorithm for finding shortest bushes.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public ShortestBushGeneralised(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Construct shortest bush result from origin node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest bush result that can be used to extract bushes
   */
  @Override
  public ShortestBushResult executeOneToAll(DirectedVertex currentOrigin) {

    this.currentSource = currentOrigin;

    /* see #processShorterOrEqualIncomginEdgeSegment on how it is populated */
    this.nextEdgeSegments = new Object[numberOfVertices];

    /*
     * found shortest bush costs to each vertex for current origin. When deemed shortest, all incoming edge segments are stored on the array as a list, unless only a single edge
     * segment is shortest in which case the entry is just the edge segment
     */
    var vertexMeasuredCost = super.executeOneToAll(this::isShorterOrEqual, this::processShorterOrEqualIncomingEdgeSegment);
    return new ShortestBushResultGeneralised(vertexMeasuredCost, nextEdgeSegments, numberOfEdgeSegments, ShortestSearchType.ONE_TO_ALL);
  }

  /**
   * Construct shortest bush result from all nodes to destination node in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest bush result that can be used to extract bushes
   */
  @Override
  public ShortestBushResult executeAllToOne(DirectedVertex currentDestination) {
    this.currentSource = currentDestination;

    /* see #processShorterOrEqualIncomginEdgeSegment on how it is populated */
    this.nextEdgeSegments = new Object[numberOfVertices];

    /*
     * found shortest bush costs from each vertex to current destination. When deemed shortest, all outgoing edge segments are stored on the array as a list, unless only a single
     * edge segment is shortest in which case the entry is just the edge segment
     */
    var vertexMeasuredCost = super.executeAllToOne(this::isShorterOrEqual, this::processShorterOrEqualIncomingEdgeSegment);
    return new ShortestBushResultGeneralised(vertexMeasuredCost, nextEdgeSegments, numberOfEdgeSegments, ShortestSearchType.ALL_TO_ONE);
  }
}
