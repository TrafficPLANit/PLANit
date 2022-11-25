package org.goplanit.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPaths;

/**
 * Implementation of (managed) DirectedPaths interface
 * 
 * @author markr
 */
public class ManagedDirectedPathsImpl extends ManagedIdEntitiesImpl<ManagedDirectedPath> implements ManagedDirectedPaths {

  /** factory to use */
  private final ContainerisedDirectedPathFactoryImpl directedPathFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ManagedDirectedPathsImpl(final IdGroupingToken groupId) {
    super(ManagedDirectedPath::getId, ManagedDirectedPath.PATH_ID_CLASS);
    this.directedPathFactory = new ContainerisedDirectedPathFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId             to use for creating ids for instances
   * @param directedPathFactory the factory to use
   */
  public ManagedDirectedPathsImpl(final IdGroupingToken groupId, ContainerisedDirectedPathFactoryImpl directedPathFactory) {
    super(ManagedDirectedPath::getId, ManagedDirectedPath.PATH_ID_CLASS);
    this.directedPathFactory = directedPathFactory;
  }

  /**
   * Copy constructor
   * 
   * @param directedPathsImpl to copy
   */
  public ManagedDirectedPathsImpl(ManagedDirectedPathsImpl directedPathsImpl) {
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
  public ManagedDirectedPathsImpl clone() {
    return new ManagedDirectedPathsImpl(this);
  }

}
