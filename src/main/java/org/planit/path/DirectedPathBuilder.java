package org.planit.path;

import java.util.Deque;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.path.DirectedPath;

/**
 * A path builder to use for creating paths
 * 
 * @author markr
 *
 * @param <P> type of path to create
 */
public interface DirectedPathBuilder<P extends DirectedPath> {

  /** Create a new emtpy directed path 
   * 
   * @return created directed path
   */
  public P createPath();
  
  
  /** Create a new directed path based on provided edge segments. Provided edge segments container is to be used directly to avoid
   * copying overhead.
   * 
   * @param pathEdgeSegments that comprise the path
   * @return created directed path
   */
  public P createPath(final Deque<EdgeSegment> pathEdgeSegments);

}
