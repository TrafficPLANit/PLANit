package org.planit.algorithms.shortestpath;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.planit.path.Path;
import org.planit.path.PathImpl;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGroupingToken;

/**
 * Class that stores the result of a shortest path execution allowing
 * one to extract paths or cost information
 * 
 * Note that we must traverse a path from an origin to a destination in reversed order to extract the path
 * 
 * @author markr
 *
 */
public class ShortestPathResult {
  
  private static final Logger LOGGER = Logger.getLogger(ShortestPathResult.class.getCanonicalName());
  
  /**
   * the costs found by a shortest path run
   */
  protected final double[] vertexMeasuredCost;     

  /**
   * the preceding vertex to reach the vertex with the given measured cost
   */
  protected final EdgeSegment[] incomingEdgeSegment;
  
  /** Constructor only to be used by shortest path algorithms
   * 
   * @param vertexMeasuredCost measured costs to get to the vertex (by id)
   * @param incomingEdgeSegment the incoming edge segment for each vertex (by id)
   */
  protected ShortestPathResult(double[] vertexMeasuredCost, EdgeSegment[] incomingEdgeSegment) {
    this.vertexMeasuredCost = vertexMeasuredCost;
    this.incomingEdgeSegment = incomingEdgeSegment;    
  }

  /**
   * Create the path from the provided origin to a specified destination vertex, using the results available.
   *
   *
   * @param groupId           contiguous id generation within this group for instances for paths
   * @param origin            the specified origin vertex
   * @param destination       the specified destination vertex
   * @return the path that is created, when no path could be extracted null is returned
   * 
   */
  public Path createPath(IdGroupingToken groupId, Vertex origin, Vertex destination){
    // path edge segment container
    final Deque<EdgeSegment> pathEdgeSegments = new LinkedList<EdgeSegment>();
    
    // prep
    int vertexId  = (int)destination.getId();
    EdgeSegment previousEdgeSegmentOnPath = incomingEdgeSegment[vertexId];
    final int originVertexId = (int)origin.getId();
    
    // extract path
    while (originVertexId != vertexId) {
      if(previousEdgeSegmentOnPath==null) {
        LOGGER.warning(String.format("unable to extract path from origin %s (id:%d) to destination %s (id:%d)",
                origin.getExternalId(), origin.getId(), destination.getExternalId(), destination.getId()));
        return null;
      }      
      pathEdgeSegments.addFirst(previousEdgeSegmentOnPath);
      vertexId = (int)previousEdgeSegmentOnPath.getUpstreamVertex().getId();
      previousEdgeSegmentOnPath = incomingEdgeSegment[vertexId];
    }
    
    // create path
    return new PathImpl(groupId, pathEdgeSegments);
  }

  /** Find the incoming edge segment for a given vertex
   * @param vertex
   * @return incoming edge segment
   */
  public EdgeSegment getIncomingEdgeSegmentForVertex(Vertex vertex) {
    return incomingEdgeSegment[(int)vertex.getId()];
  }

  /** Collect the cost to reach the given vertex
   * @param vertex to collect cost for
   * @return cost found
   */
  public double getCostToReach(Vertex vertex) {
    return vertexMeasuredCost[(int) vertex.getId()];
  }
  
  

}
