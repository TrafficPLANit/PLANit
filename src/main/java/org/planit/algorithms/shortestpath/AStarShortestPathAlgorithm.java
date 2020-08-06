package org.planit.algorithms.shortestpath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitGeoUtils;
import org.planit.path.Path;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.Pair;

/**
 * A* shortest path algorithm
 * 
 * A* shortest path is a one-to-one implementation of the shortest path
 * algorithm based on the generalized costs on each link segment (edge). The
 * costs should be provided upon instantiation and are reused whenever a
 * One-To-One execution conditional on the chosen source node is performed.
 * 
 * In its current form, it assumes a macroscopic network and macroscopic link
 * segments to operate on
 * 
 * @author markr
 *
 */
public class AStarShortestPathAlgorithm implements OneToOneShortestPathAlgorithm {

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
   * CRS to interpret the position information of vertices
   */
  protected final CoordinateReferenceSystem crs;
  
  /**
   * conversion multiplier to convert distance (km) to cost
   */
  protected final double heuristicDistanceMultiplier;
    
  /** 
   * Comparator to sort based on the second elements minimum value (ascending order) 
   */
  protected static final Comparator<Pair<Vertex, Double>> pairSecondComparator =
      Comparator.comparing(Pair<Vertex, Double>::getSecond, (f1, f2) -> {
        return f1.compareTo(f2);
      });
  
  /**
   * Constructor for an edge cost based A* algorithm for finding shortest paths.
   * 
   * @param edgeSegmentCosts Edge segment costs
   * @param numberOfVertices number of vertices in the network
   * @param crs the coordinate reference system used in the network, i.e., we can draw upon the geo information of the vertices to compute our heuristic component
   * @param heuristicDistanceMultiplier used to convert the distance between two vertices to a cost, in transport context this would generally be the maximum speed (km/h) 
   *  that is allowed on the network assuming the cost is representing travel time (h). 
   */
  public AStarShortestPathAlgorithm(final double[] edgeSegmentCosts,  int numberOfVertices, CoordinateReferenceSystem crs, double heuristicDistanceMultiplier) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = edgeSegmentCosts.length;
    this.crs = crs;
    this.heuristicDistanceMultiplier = heuristicDistanceMultiplier;    
  }
  
  /**
   * Constructor for an edge cost based A* algorithm for finding shortest paths. In absence of any information regarding the coordinate reference system
   * this approach assumes the vertices have location information but this is cartesian in nature and can be converted to a heuristic cost by utilising the provided
   * heuristicDistanceMultiplier. In this case the multiplier must acocunt for both the conversion to kms for the coordinates as well as the conversion to cost units (likely in hour)
   * 
   * @param edgeSegmentCosts Edge segment costs
   * @param numberOfVertices number of vertices in the network
   * @param heuristicDistanceMultiplier multiplier for coordinates where each unit change in coordinate equates to unit*heuristicDistanceMultiplier cost change
   */
  public AStarShortestPathAlgorithm(final double[] edgeSegmentCosts,  int numberOfVertices, double heuristicDistanceMultiplier) {
    this.edgeSegmentCosts = edgeSegmentCosts;
    this.numberOfVertices = numberOfVertices;
    this.numberOfEdgeSegments = edgeSegmentCosts.length;
    // unknowns
    this.heuristicDistanceMultiplier = -1;
    this.crs = null;
  }  
  
  /**
   * {@inheritDoc}
   * 
   * We create the heuristic costs on-the-fly based on the coordinates of the vertex and computing the as-the-crow-flies lower bound cost.
   * Can only be used when instance was created by providing ${code CRS} and ${codeheuristicDistanceMultiplier} in constructor
   */
  @Override
  public ShortestPathResult executeOneToOne(Vertex origin, Vertex destination) throws PlanItException {
    PlanItException.throwIf(crs==null, "Unknown coordinate reference system, unable to construct heuristic component for A* shortest path");    
    PlanitGeoUtils geoUtils = new PlanitGeoUtils(crs);
    
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

    PriorityQueue<Pair<Vertex, Double>> openVertices = new PriorityQueue<Pair<Vertex, Double>>(numberOfVertices, pairSecondComparator);       
    
    // initialise for origin
    openVertices.add(new Pair<Vertex, Double>(origin, 0.0));
    vertexMeasuredCost[(int)origin.getId()] = 0.0;
    vertexHeuristicCost[(int)origin.getId()] = geoUtils.getDistanceInKilometres(origin.getCentrePointGeometry(), destination.getCentrePointGeometry());
    incomingEdgeSegment[(int)origin.getId()] = null;
    
    Vertex currentVertex =null;
    while(!openVertices.isEmpty()) {
      Pair<Vertex, Double> cheapestNextVertex = openVertices.poll();
      currentVertex = cheapestNextVertex.getFirst();      
      int vertexId = (int)currentVertex.getId();
      // reached destination with lowest cost possible
      if(vertexId == destination.getId()) {
        break;
      }
      
      // when vertex has already been processed in between adding it and it becoming the highest priority ignore...
      if(closedVertex[vertexId]) {
        continue;
      }else {
        // ..otherwise mark as processed (after this)...
        closedVertex[vertexId] = true;
      } //... we use closed vertex array to filter entries that are no longer viable. We cannot remove entries from a priority queue, so this mechanism is in place
        //    to create the same effect with as little as possible computational overhead
      
      // cost to here
      double costToVertex = cheapestNextVertex.getSecond();
      
      // for all exiting edges
      for (EdgeSegment adjacentEdgeSegment : currentVertex.getExitEdgeSegments()) {
        int adjacentVertexId = (int) adjacentEdgeSegment.getDownstreamVertex().getId();
        
        // edge cost
        double exitEdgeCost = edgeSegmentCosts[adjacentVertexId];
        if (exitEdgeCost < Double.POSITIVE_INFINITY) {
          
          //updated actual cost to adjacent node
          double tentativeCost = costToVertex+exitEdgeCost;
          
          Vertex adjacentVertex = adjacentEdgeSegment.getDownstreamVertex();
          double adjacentMeasuredCost = vertexMeasuredCost[adjacentVertexId];
          
          // first visit, compute heuristic on the fly (once)
          if(adjacentMeasuredCost == Double.POSITIVE_INFINITY) {
            vertexHeuristicCost[adjacentVertexId] = this.crs != null ?
                geoUtils.getDistanceInKilometres(adjacentVertex.getCentrePointGeometry(), destination.getCentrePointGeometry())*heuristicDistanceMultiplier : 
                -1; // TODO <-- compute differently
          }
          
          // when tentative cost is more attractive, update path
          if ( adjacentMeasuredCost > tentativeCost) {
            incomingEdgeSegment[adjacentVertexId] = adjacentEdgeSegment;
            vertexMeasuredCost[adjacentVertexId]  = tentativeCost;
            
            // prioritise exploring the new vertex based on f-score (measured + heuristic)
            double priorityCost                   = tentativeCost + vertexHeuristicCost[adjacentVertexId];            
            openVertices.add(new Pair<Vertex, Double>(adjacentVertex, priorityCost)); // place on queue
          }
        }            
      }
    }
    
    PlanItException.throwIf(currentVertex.getId() != destination.getId(),
        String.format("destination %s (id:%d) unreachable from origin %d (id:%d)",
            destination.getExternalId(), destination.getId(), origin.getExternalId(), origin.getId()));
    
    return new ShortestPathResult(vertexMeasuredCost, incomingEdgeSegment);
  }


}
