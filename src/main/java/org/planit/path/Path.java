package org.planit.path;

import java.util.Iterator;

import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.utils.graph.EdgeSegment;

/**
 * Path interface representing a path through the network on edge segment level
 * 
 * @author markr
 *
 */
public interface Path {

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
  public String toString(final PathOutputIdentificationType pathOutputType);

  /**
   * Return the Id of this path
   * 
   * @return the Id of this path
   */
  public long getId();

}
