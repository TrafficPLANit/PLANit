package org.planit.path;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.logging.Logger;

import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Node;

/**
 * This object creates a path of LinkSegment objects to a specified destination using the vertexPathAndCost object created by the (Dijkstra) Shortest Path Algorithm
 *
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 *
 * @author gman6028
 *
 */
public class PathImpl implements Path {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PathImpl.class.getCanonicalName());

  /**
   * Id of the path
   */
  private long id;

  /**
   * deque containing the edge segments in the path (we use a deque for easy insertion at both ends which is generally preferable when cosntructing paths
   * based on shortest path algorithms. Access is less of an issue as we only allow one to iterate
   */
  private final Deque<EdgeSegment> path;

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
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public PathImpl(final IdGroupingToken groupId) {
    id = IdGenerator.generateId(groupId, Path.class);
    path = new LinkedList<EdgeSegment>();
  }

  /**
   * Constructor
   * 
   * @param groupId          contiguous id generation within this group for instances of this class
   * @param pathEdgeSegments the path to set (not copied)
   */
  public PathImpl(final IdGroupingToken groupId, final Deque<EdgeSegment> pathEdgeSegments) {
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
  public Iterator<EdgeSegment> iterator() {
    return path.iterator();
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
      return getEdgeSegmentPathString(EdgeSegment::getExternalId);
    case LINK_SEGMENT_XML_ID:
      return getEdgeSegmentPathString(EdgeSegment::getXmlId);      
    case LINK_SEGMENT_ID:
      return getEdgeSegmentPathString(EdgeSegment::getId);
    case NODE_EXTERNAL_ID:
      return getNodePathString(Node::getExternalId);
    case NODE_XML_ID:
      return getNodePathString(Node::getXmlId);      
    case NODE_ID:
      return getNodePathString(Node::getId);
    }
    return "";
  }



}
