package org.goplanit.algorithms.shortest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Shortest bush algorithm.
 * 
 * Shortest bush algorithm is a one-to-all implementation of all (equal cost) implicit shortest bush comprising of all equal cost paths based on the generalized costs on each link
 * segment (edge). It is identical to Dijkstra's shortest path algorithm except that it creates a bush rooted at the origin towards each node, where each node stores all its equal
 * cheapest predecessor nodes from which the bush can be extracted when traversing it in reverse order (from node back to origin)
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class OneToAllShortestBushAlgorithmImpl extends OneToAllShortestGeneralisedAlgorithm implements OneToAllShortestBushAlgorithm {

  /**
   * Track incoming edge segment(s) that are shortest for each vertex in this array. In case there is only a single shortest option, the object is an edge segment, otherwise it is
   * a collection of edge segments
   */
  protected Object[] incomingEdgeSegments;

  /** track if most recent tested costs were equal, if so, any processed edge segment is to be added to existing shortest edge segments to complement the bush */
  protected boolean currEqualShortestCosts;

  /**
   * predicate for shortest bush means less or equal cost compared to existing cost, so cheaper and equal cost paths are considered
   */
  protected static final BiPredicate<Double, Double> isShorterOrEqualPredicate = (currCost, computedCost) -> {
    return Precision.greaterEqual(currCost, computedCost, Precision.EPSILON_15);
  };

  /**
   * Test whether shortest bush means less or equal cost compared to existing cost, so cheaper and equal cost paths are considered. also store the costs so we can test if they were
   * equal or shorter in {@link #processShorterOrEqualIncomginEdgeSegment(EdgeSegment)}
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
   * @param incomingEdgeSegment that is considered shorter or equal shortest for its downstream vertex
   */
  @SuppressWarnings("unchecked")
  private void processShorterOrEqualIncomginEdgeSegment(EdgeSegment incomingEdgeSegment) {

    if (currEqualShortestCosts) {
      /* equal costs */

      var incomingEdgeSegmentsVertex = incomingEdgeSegments[(int) incomingEdgeSegment.getDownstreamVertex().getId()];
      int downstreamVertexId = (int) incomingEdgeSegment.getDownstreamVertex().getId();
      if (incomingEdgeSegmentsVertex instanceof EdgeSegment) {
        /* change from single segment to collection */
        var list = new ArrayList<EdgeSegment>(2);
        list.add((EdgeSegment) incomingEdgeSegmentsVertex);
        list.add(incomingEdgeSegment);
        incomingEdgeSegments[downstreamVertexId] = list;
      } else {
        /* add to existing collection */
        ((List<EdgeSegment>) incomingEdgeSegments[downstreamVertexId]).add(incomingEdgeSegment);
      }

    } else {
      /* cheapest */

      incomingEdgeSegments[(int) incomingEdgeSegment.getDownstreamVertex().getId()] = incomingEdgeSegment;
    }
  }

  /**
   * Constructor for an edge cost based algorithm for finding shortest bushes.
   * 
   * @param edgeSegmentCosts     Edge segment costs
   * @param numberOfEdgeSegments Edge segments, both physical and connectoid
   * @param numberOfVertices     Vertices, both nodes and centroids
   */
  public OneToAllShortestBushAlgorithmImpl(final double[] edgeSegmentCosts, int numberOfEdgeSegments, int numberOfVertices) {
    super(edgeSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Construct shortest bush result from source node to all other nodes in the network based on directed LinkSegment edges
   * 
   * @param currentOrigin origin vertex of source node
   * @return shortest bush result that can be used to extract bushes
   * @throws PlanItException thrown if an error occurs
   */
  @Override
  public ShortestBushResult executeOneToAll(DirectedVertex currentOrigin) throws PlanItException {

    this.currentOrigin = currentOrigin;

    /* see #processShorterOrEqualIncomginEdgeSegment on how it is populated */
    this.incomingEdgeSegments = new Object[numberOfVertices];

    /*
     * found shortest bush costs to each vertex for current origin. When deemed shortest, all incoming edge segments are stored on the array as a list, unless only a single edge
     * segment is shortest in which case the entry is just the edge segment
     */
    var vertexMeasuredCost = super.executeOneToAll(this::isShorterOrEqual, this::processShorterOrEqualIncomginEdgeSegment);
    return new ShortestBushResultImpl(currentOrigin, vertexMeasuredCost, incomingEdgeSegments, numberOfEdgeSegments);
  }
}
