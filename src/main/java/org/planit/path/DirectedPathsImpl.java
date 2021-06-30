package org.planit.path;

import java.util.Deque;
import java.util.TreeMap;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPaths;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * Implementation of DirectedPaths interface
 * 
 * @author markr
 * 
 * @param <p> type of directed path
 */
public class DirectedPathsImpl<P extends DirectedPath> extends LongMapWrapperImpl<P> implements DirectedPaths<P> {

  /**
   * The builder to create paths
   */
  private final DirectedPathBuilder<P> pathBuilder;

  /**
   * Constructor
   * 
   * @param pathBuilder the builder for path instances
   */
  public DirectedPathsImpl(DirectedPathBuilder<P> pathBuilder) {
    super(new TreeMap<Long, P>(), P::getId);
    this.pathBuilder = pathBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public P registerNew() {
    final P newPath = pathBuilder.createPath();
    register(newPath);
    return newPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public P registerNew(Deque<EdgeSegment> edgeSegments) {
    final P newPath = pathBuilder.createPath(edgeSegments);
    register(newPath);
    return newPath;
  }

}
