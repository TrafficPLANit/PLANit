package org.goplanit.path;

import java.util.Deque;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ContainerisedDirectedPathFactory;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.path.DirectedPaths;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class ContainerisedDirectedPathFactoryImpl extends DirectedPathFactoryImpl implements ContainerisedDirectedPathFactory {

  /** container to use */
  private final DirectedPaths directedPaths;

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   * @param directedPaths to register the created instances on
   */
  public ContainerisedDirectedPathFactoryImpl(IdGroupingToken groupIdToken, DirectedPaths directedPaths) {
    super(groupIdToken);
    this.directedPaths = directedPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath registerNew() {
    DirectedPath newPath = createNew();
    directedPaths.register(newPath);
    return newPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath registerNew(Deque<? extends EdgeSegment> edgeSegments) {
    DirectedPath newPath = createNew(edgeSegments);
    directedPaths.register(newPath);
    return newPath;
  }
}
