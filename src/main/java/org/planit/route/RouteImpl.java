package org.planit.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.planit.output.enums.RouteIdType;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * This object creates a route of LinkSegment objects to a specified destination using the
 * vertexPathAndCost object created by the (Dijkstra) Shortest Path Algorithm
 *
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so
 * there is no need to specify the origin.
 *
 * @author gman6028
 *
 */
public class RouteImpl implements Route {

  /**
   * Id of the path
   */
  private long id;

  /**
   * List containing the edge segments in the path
   */
  private final List<EdgeSegment> path;

  /**
   * Create the route from an implicit origin to a specified destination, using the
   * vertexPathAndCost array as input
   * coming from a shortest path algorithm output
   *
   * This method makes use of the fact that the origin pair in the vertexPathAndCost array has a
   * null EdgeSegment.
   * It searches through vertexPathAndCost from the destination centroid until it finds a null
   * EdgeSegment, which must
   * represent the origin centroid. This is quicker than doing an instanceof test to determine
   * whether the upstream vertex is a physical node.
   *
   * @param destination the specified destination zone
   * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic
   *          assignment)
   * @return the route that is created
   */
  public static RouteImpl createODRoute(final Centroid destination,
      final Pair<Double, EdgeSegment>[] vertexPathAndCost) {
    long downstreamVertexId = destination.getId();
    EdgeSegment edgeSegment = vertexPathAndCost[(int) downstreamVertexId].getSecond();
    final List<EdgeSegment> pathEdgeSegments = new ArrayList<EdgeSegment>();
    while (edgeSegment != null) {
      pathEdgeSegments.add(edgeSegment);
      downstreamVertexId = edgeSegment.getUpstreamVertex().getId();
      edgeSegment = vertexPathAndCost[(int) downstreamVertexId].getSecond();
    }
    Collections.reverse(pathEdgeSegments);
    return new RouteImpl(pathEdgeSegments);
  }

  /**
   * Returns the path as a String of comma-separated node Id or external Id values
   *
   * @param idGetter lambda function to get the required Id value
   * @return the path as a String of comma-separated node Id or external Id values
   */
  private String getNodeRouteString(final Function<Node, Object> idGetter) {
    final StringBuilder builder = new StringBuilder("[");
    for (final EdgeSegment edgeSegment : path) {
      final Vertex vertex = edgeSegment.getUpstreamVertex();
      if (vertex instanceof Node) {
        final Node node = (Node) vertex;
        builder.append(idGetter.apply(node));
        if (edgeSegment.getDownstreamVertex() instanceof Node) {
          builder.append(",");
        }
      }
    }
    builder.append("]");
    return new String(builder);
  }

  /**
   * Returns the path as a String of comma-separated edge segment Id or external Id values
   *
   * @param idGetter lambda function to get the required Id value
   * @return the path as a String of comma-separated link segment Id or external Id values
   */
  private String getEdgeSegmentRouteString(final Function<EdgeSegment, Object> idGetter) {
    final StringBuilder builder = new StringBuilder("[");
    for (final EdgeSegment edgeSegment : path) {
      builder.append(idGetter.apply(edgeSegment));
      builder.append(",");
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append("]");
    return new String(builder);
  }

  /**
   * Output the path as a comma-separated list of edge segment external Id numbers
   *
   * @return string of comma-separated list of edge segment external Id numbers
   */
  private String getRouteByEdgeSegmentExternalIdString() {
    return getEdgeSegmentRouteString(edgeSegment -> {
      if ((edgeSegment instanceof ConnectoidSegment) && !(((ConnectoidSegment) edgeSegment).hasExternalId())) {
        return "Connectoid Undefined";
      }
      return edgeSegment.getExternalId();
    });
  }

  /**
   * Output the path as a comma-separated list of edge segment Id numbers
   *
   * @return string of comma-separated list of edge segment Id numbers
   */
  private String getRouteByEdgeSegmentIdString() {
    return getEdgeSegmentRouteString(EdgeSegment::getId);
  }

  /**
   * Output the path as a comma-separated list of node external Id numbers
   *
   * @return string of comma-separated list of node external Id numbers
   */
  private String getRouteByNodeExternalIdString() {
    return getNodeRouteString(Node::getExternalId);
  }

  /**
   * Output the path as a comma-separated list of node Id numbers
   *
   * @return string of comma-separated list of node Id numbers
   */
  private String getRouteByNodeIdString() {
    return getNodeRouteString(Node::getId);
  }

  /**
   * Constructor
   */
  public RouteImpl() {
    id = IdGenerator.generateId(RouteImpl.class);
    path = new ArrayList<EdgeSegment>();
  }

  /**
   * Constructor
   * 
   * @param pathEdgeSegments the path to set (not copied)
   */
  public RouteImpl(final List<EdgeSegment> pathEdgeSegments) {
    id = IdGenerator.generateId(RouteImpl.class);
    path = pathEdgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Boolean addEdgeSegment(final EdgeSegment edgeSegment) {
    return path.add(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<EdgeSegment> getIterator() {
    return path.iterator();
  }

  /**
   * Return the route as a List of EdgeSegments
   * 
   * @return the path as a List of EdgeSegments
   */
  @Override
  public List<EdgeSegment> getPath() {
    return path;
  }

  /**
   * Return the id of this path
   * 
   * @return the id of this path
   */
  @Override
  public long getId() {
    return id;
  }

  /**
   * Outputs this path as a String, appropriate to a specified path output type
   *
   * @param pathOutputType the specified path output type
   * @return String describing the path
   */
  @Override
  public String toString(final RouteIdType pathOutputType) {
    switch (pathOutputType) {
      case LINK_SEGMENT_EXTERNAL_ID:
        return getRouteByEdgeSegmentExternalIdString();
      case LINK_SEGMENT_ID:
        return getRouteByEdgeSegmentIdString();
      case NODE_EXTERNAL_ID:
        return getRouteByNodeExternalIdString();
      case NODE_ID:
        return getRouteByNodeIdString();
    }
    return "";
  }

}
