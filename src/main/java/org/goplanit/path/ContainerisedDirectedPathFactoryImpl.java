package org.goplanit.path;

import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.ContainerisedDirectedPathFactory;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPaths;

import java.util.Deque;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class ContainerisedDirectedPathFactoryImpl extends ManagedDirectedPathFactoryImpl implements ContainerisedDirectedPathFactory {

  /** container for directed paths to use */
  private final ManagedDirectedPaths directedPaths;

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   * @param directedPaths to register the created instances on
   */
  public ContainerisedDirectedPathFactoryImpl(IdGroupingToken groupIdToken, ManagedDirectedPaths directedPaths) {
    super(groupIdToken);
    this.directedPaths = directedPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPath registerNew() {
    ManagedDirectedPath newPath = createNew();
    directedPaths.register(newPath);
    return newPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPath registerNew(Deque<? extends EdgeSegment> edgeSegments) {
    ManagedDirectedPath newPath = createNew(edgeSegments);
    directedPaths.register(newPath);
    return newPath;
  }
}
