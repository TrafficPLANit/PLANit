package org.planit.path;

import java.util.Deque;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.path.ContainerisedDirectedPathFactory;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPaths;

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

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath registerUniqueCopyOf(ManagedId entityToCopy) {
    DirectedPath newPath = createUniqueCopyOf(entityToCopy);
    directedPaths.register(newPath);
    return newPath;
  }
}
