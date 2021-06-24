package org.planit.path;

import java.util.Deque;
import java.util.logging.Logger;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.path.DirectedPath;

/**
 * Default implementation of directed path builder to create directed paths where the internal ids are generated based on the grouping token
 * used by this builder
 * 
 * @author markr
 *
 */
public class DirectedPathBuilderImpl implements DirectedPathBuilder<DirectedPath>{

  /** the logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DirectedPathBuilderImpl.class.getCanonicalName());

  /** the id group token */
  protected IdGroupingToken groupIdToken;
  
  /**
   * Constructor
   * 
   * @param groupIdToken to use for creating element ids
   */
  public DirectedPathBuilderImpl(IdGroupingToken groupIdToken) {
    this.groupIdToken = groupIdToken;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath createPath(Deque<EdgeSegment> pathEdgeSegments) {
    return new DirectedPathImpl(groupIdToken, pathEdgeSegments);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public DirectedPath createPath() {
    return new DirectedPathImpl(groupIdToken);
  }

}
