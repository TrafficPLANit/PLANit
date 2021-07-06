package org.planit.path;

import java.util.Deque;

import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedId;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;
import org.planit.utils.path.DirectedPaths;

/**
 * Factory for creating directed paths on container
 * 
 * @author markr
 */
public class DirectedPathFactoryImpl extends ManagedIdEntityFactoryImpl<DirectedPath> implements DirectedPathFactory {

  /** container to use */
  private final DirectedPaths directedPaths;

  /**
   * Create a new instance
   * 
   * @return created path
   */
  protected DirectedPath create() {
    return new DirectedPathImpl(groupIdToken);
  }

  /**
   * Constructor
   * 
   * @param groupIdToken  to use for creating element ids
   * @param directedPaths to register the created instances on
   */
  public DirectedPathFactoryImpl(IdGroupingToken groupIdToken, DirectedPaths directedPaths) {
    super(groupIdToken);
    this.directedPaths = directedPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath registerNew() {
    DirectedPath newPath = create();
    directedPaths.register(newPath);
    return newPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPath registerNew(Deque<? extends EdgeSegment> edgeSegments) {
    DirectedPath newPath = new DirectedPathImpl(groupIdToken, edgeSegments);
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
