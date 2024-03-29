package org.goplanit.algorithms.shortest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A* shortest path algorithm
 * 
 * A* shortest path is a one-to-one implementation of the shortest path algorithm based on the generalized costs on each link segment (edge). The costs should be provided upon
 * instantiation and are reused whenever a One-To-One execution conditional on the chosen source node is performed.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link segments to operate on
 * 
 * @author markr
 *
 */
public class ShortestPathAStar implements ShortestPathOneToOne {

  /**
   * The cost for each edge to determine shortest paths
   */
  protected final double[] edgeSegmentCosts;

  /**
   * The number of edge segments considered
   */
  protected final int numberOfEdgeSegments;

  /**
   * The number of vertices in the network
   */
  protected final int numberOfVertices;

  /**
   * CRS based utility class to interpret the position information of vertices
   */
  protected final PlanitJtsCrsUtils geoUtils;

  /**
   * conversion multiplier to convert distance (km) to cost
   */
  protected final double heuristicDistanceMultiplier;

  /**
   * Comparator to sort based on the second elements minimum value (ascending order)
   */
  protected static final Comparator<Pair<DirectedVertex, Double>> pairSecondComparator = Comparator.comparing(Pair::second, Comparator.naturalOrder());

  /**
   * Constructor for an edge cost based A* algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts            Edge segment costs
   * @param numberOfVertices            number of vertices in the network
   * @param crs                         the coordinate reference system used in the network, i.e., we can draw upon the geo information of the vertices to compute our heuristic
   *                                    component
   * @param heuristicDistanceMultiplier used to convert the distance between two vertices to a cost, in transport context this would generally be the 1/(maximum speed (km/h)), e.g. pace to convert
   *                                    a heuristic distance (km) into travel time (h) since (km* h/km = h).
   */
  public ShortestPathAStar(final double[] edgeSegmentCosts, int numberOfVertices, CoordinateReferenceSystem crs, double heuristicDistanceMultiplier) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = edgeSegmentCosts.length;
    geoUtils = new PlanitJtsCrsUtils(crs);
    this.heuristicDistanceMultiplier = heuristicDistanceMultiplier;
  }

  /**
   * {@inheritDoc}
   * 
   * We create the heuristic costs on-the-fly based on the coordinates of the vertex and computing the as-the-crow-flies lower bound cost. Can only be used when instance was
   * created by providing ${code CRS} and ${codeheuristicDistanceMultiplier} in constructor. Also, all network entities should hold geo positions otherwise the execution will fail
   * with a nullpointer.
   *
   */
  @Override
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination, Set<? extends EdgeSegment> bannedSegments) {
    if (origin.getPosition() == null || destination.getPosition() == null) {
      throw new PlanItRunTimeException(
          "aStar shortest path must compute distances between vertices on-the-fly. One or more vertices do not have location information available making this impossible");
    }

    // g-score (actual measured cost to destination)
    double[] vertexMeasuredCost = new double[numberOfVertices];
    Arrays.fill(vertexMeasuredCost, Double.POSITIVE_INFINITY);
    // h-score (fixed heuristic cost to destination)
    double[] vertexHeuristicCost = new double[numberOfVertices];
    Arrays.fill(vertexHeuristicCost, Double.POSITIVE_INFINITY);
    // precedingVertex for each vertex (used to reconstruct path)
    EdgeSegment[] incomingEdgeSegment = new EdgeSegment[numberOfVertices];
    // closed set used to filter out old entries in immutable priority queue (so we do not have to remove them)
    boolean[] closedVertex = new boolean[numberOfVertices];
    Arrays.fill(closedVertex, Boolean.FALSE);

    PriorityQueue<Pair<DirectedVertex, Double>> openVertices = new PriorityQueue<>(numberOfVertices, pairSecondComparator);

    // initialise for origin
    openVertices.add(Pair.of(origin, 0.0));
    vertexMeasuredCost[(int) origin.getId()] = 0.0;
    vertexHeuristicCost[(int) origin.getId()] = geoUtils.getDistanceInKilometres(origin.getPosition(), destination.getPosition()) * heuristicDistanceMultiplier;
    incomingEdgeSegment[(int) origin.getId()] = null;

    DirectedVertex currentVertex = null;
    while (!openVertices.isEmpty()) {
      Pair<DirectedVertex, Double> cheapestNextVertex = openVertices.poll();
      currentVertex = cheapestNextVertex.first();
      int vertexId = (int) currentVertex.getId();
      // reached destination with lowest cost possible
      if (vertexId == destination.getId()) {
        break;
      }

      // when vertex has already been processed in between adding it and it becoming the highest priority ignore...
      if (closedVertex[vertexId]) {
        continue;
      } else {
        // ..otherwise mark as processed (after this)...
        closedVertex[vertexId] = true;
      } // ... we use closed vertex array to filter entries that are no longer viable. We cannot remove entries from a priority queue, so this mechanism is in place
        // to create the same effect with as little as possible computational overhead

      // cost to here
      double costToVertex = vertexMeasuredCost[vertexId];

      // for all exiting edges
      for (var adjacentEdgeSegment : currentVertex.getExitEdgeSegments()) {
        if(bannedSegments!= null && !bannedSegments.isEmpty() && bannedSegments.contains(adjacentEdgeSegment)){
          continue;
        }

        int adjacentVertexId = (int) adjacentEdgeSegment.getDownstreamVertex().getId();

        // edge cost
        double exitEdgeCost = edgeSegmentCosts[(int) adjacentEdgeSegment.getId()];
        if (exitEdgeCost < Double.MAX_VALUE) {

          // updated actual cost to adjacent node
          double tentativeCost = costToVertex + exitEdgeCost;

          var adjacentVertex = adjacentEdgeSegment.getDownstreamVertex();
          double adjacentMeasuredCost = vertexMeasuredCost[adjacentVertexId];

          // first visit, compute heuristic on the fly (once)
          if (adjacentMeasuredCost == Double.POSITIVE_INFINITY) {
            vertexHeuristicCost[adjacentVertexId] = geoUtils.getDistanceInKilometres(adjacentVertex.getPosition(), destination.getPosition()) * heuristicDistanceMultiplier;
          }

          // when tentative cost is more attractive, update path
          if (adjacentMeasuredCost > tentativeCost) {
            incomingEdgeSegment[adjacentVertexId] = adjacentEdgeSegment;
            vertexMeasuredCost[adjacentVertexId] = tentativeCost;

            // prioritise exploring the new vertex based on f-score (measured + heuristic)
            double priorityCost = tentativeCost + vertexHeuristicCost[adjacentVertexId];
            openVertices.add(Pair.of(adjacentVertex, priorityCost)); // place on queue
          }
        }
      }
    }

    if(currentVertex.getId() != destination.getId()) {
      throw new PlanItRunTimeException("Destination %s (id:%d) unreachable from origin %S (id:%d)", destination.getXmlId(), destination.getId(), origin.getXmlId(), origin.getId());
    }

    return new ShortestPathResultGeneralised(vertexMeasuredCost, incomingEdgeSegment, ShortestSearchType.ONE_TO_ONE);
  }

  @Override
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination) {
    return executeOneToOne(origin, destination, null);
  }

}
