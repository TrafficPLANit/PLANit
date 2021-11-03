package org.goplanit.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.path.DirectedPath;
import org.goplanit.utils.path.DirectedPaths;

/**
 * Implementation of DirectedPaths interface
 * 
 * @author markr
 */
public class DirectedPathsImpl extends ManagedIdEntitiesImpl<DirectedPath> implements DirectedPaths {

  /** factory to use */
  private final ContainerisedDirectedPathFactoryImpl directedPathFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public DirectedPathsImpl(final IdGroupingToken groupId) {
    super(DirectedPath::getId, DirectedPath.PATH_ID_CLASS);
    this.directedPathFactory = new ContainerisedDirectedPathFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId             to use for creating ids for instances
   * @param directedPathFactory the factory to use
   */
  public DirectedPathsImpl(final IdGroupingToken groupId, ContainerisedDirectedPathFactoryImpl directedPathFactory) {
    super(DirectedPath::getId, DirectedPath.PATH_ID_CLASS);
    this.directedPathFactory = directedPathFactory;
  }

  /**
   * Copy constructor
   * 
   * @param directedPathsImpl to copy
   */
  public DirectedPathsImpl(DirectedPathsImpl directedPathsImpl) {
    super(directedPathsImpl);
    this.directedPathFactory = directedPathsImpl.directedPathFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ContainerisedDirectedPathFactoryImpl getFactory() {
    return directedPathFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedPathsImpl clone() {
    return new DirectedPathsImpl(this);
  }

}
