package org.planit.path;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * This object creates a path of LinkSegment objects to a specified destination using the vertexPathAndCost object created by the (Dijkstra) Shortest Path Algorithm
 *
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 *
 * @author gman6028
 *
 */
public class PathImpl implements Path {

  /**
   * Id of the path
   */
  private long id;

  /**
   * List containing the edge segments in the path
   */
  private final List<EdgeSegment> path;

  /**
   * Returns the path as a String of comma-separated node Id or external Id values
   *
   * @param idGetter lambda function to get the required Id value
   * @return the path as a String of comma-separated node Id or external Id values
   */
  private String getNodePathString(final Function<Node, Object> idGetter) {
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
  private String getEdgeSegmentPathString(final Function<EdgeSegment, Object> idGetter) {
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
  private String getPathByEdgeSegmentExternalIdString() {
    return getEdgeSegmentPathString(edgeSegment -> {
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
  private String getPathByEdgeSegmentIdString() {
    return getEdgeSegmentPathString(EdgeSegment::getId);
  }

  /**
   * Output the path as a comma-separated list of node external Id numbers
   *
   * @return string of comma-separated list of node external Id numbers
   */
  private String getPathByNodeExternalIdString() {
    return getNodePathString(Node::getExternalId);
  }

  /**
   * Output the path as a comma-separated list of node Id numbers
   *
   * @return string of comma-separated list of node Id numbers
   */
  private String getPathByNodeIdString() {
    return getNodePathString(Node::getId);
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected PathImpl(final IdGroupingToken groupId) {
    id = IdGenerator.generateId(groupId, Path.class);
    path = new ArrayList<EdgeSegment>();
  }

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param pathEdgeSegments the path to set (not copied)
   */
  protected PathImpl(final IdGroupingToken groupId, final List<EdgeSegment> pathEdgeSegments) {
    id = IdGenerator.generateId(groupId, Path.class);
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
   * Return the path as a List of EdgeSegments
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
  public String toString(final PathOutputIdentificationType pathOutputType) {
    switch (pathOutputType) {
    case LINK_SEGMENT_EXTERNAL_ID:
      return getPathByEdgeSegmentExternalIdString();
    case LINK_SEGMENT_ID:
      return getPathByEdgeSegmentIdString();
    case NODE_EXTERNAL_ID:
      return getPathByNodeExternalIdString();
    case NODE_ID:
      return getPathByNodeIdString();
    }
    return "";
  }

}
