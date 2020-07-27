package org.planit.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.planit.output.enums.RouteIdType;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.virtual.Centroid;

/**
 * Route interface representing a route through the network on edge segment level
 * 
 * @author markr
 *
 */
public interface Route {

  /**
   * add an edge segment to the path by appending it
   * 
   * @param edgeSegment the edge segment to add
   * @return true as per Collection.add
   */
  Boolean addEdgeSegment(EdgeSegment edgeSegment);

  /**
   * Iterator over the available edge segments
   * 
   * @return edgseSegmentIterator
   */
  Iterator<EdgeSegment> getIterator();

  /**
   * Outputs this path as a String, appropriate to a specified path output type
   *
   * @param pathOutputType the specified path output type
   * @return String describing the path
   */
  public String toString(final RouteIdType pathOutputType);

  /**
   * Return the route as a List of EdgeSegments
   * 
   * @return the path as a List of EdgeSegments
   */
  public List<EdgeSegment> getPath();

  /**
   * Return the Id of this path
   * 
   * @return the Id of this path
   */
  public long getId();

  /**
   * Create the route from an implicit origin to a specified destination, using the vertexPathAndCost array as input coming from a shortest path algorithm output
   *
   * This method makes use of the fact that the origin pair in the vertexPathAndCost array has a null EdgeSegment. It searches through vertexPathAndCost from the destination
   * centroid until it finds a null EdgeSegment, which must represent the origin centroid. This is quicker than doing an instanceof test to determine whether the upstream vertex is
   * a physical node.
   *
   * @param groupId           contiguous id generation within this group for instances of this class
   * @param destination       the specified destination zone
   * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
   * @return the route that is created
   * 
   */
  public static Route createRoute(final IdGroupingToken groupId, final Centroid destination, final Pair<Double, EdgeSegment>[] vertexPathAndCost) {
    long downstreamVertexId = destination.getId();
    EdgeSegment edgeSegment = vertexPathAndCost[(int) downstreamVertexId].getSecond();
    final List<EdgeSegment> pathEdgeSegments = new ArrayList<EdgeSegment>();
    while (edgeSegment != null) {
      pathEdgeSegments.add(edgeSegment);
      downstreamVertexId = edgeSegment.getUpstreamVertex().getId();
      edgeSegment = vertexPathAndCost[(int) downstreamVertexId].getSecond();
    }
    Collections.reverse(pathEdgeSegments);
    return new RouteImpl(groupId, pathEdgeSegments);
  }

}
