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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param directedPathsImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ManagedDirectedPathsImpl(ManagedDirectedPathsImpl directedPathsImpl, boolean deepCopy) {
    super(directedPathsImpl, deepCopy);
    this.directedPathFactory =
            new ContainerisedDirectedPathFactoryImpl(directedPathsImpl.directedPathFactory.getIdGroupingToken(), this);
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
    return new ManagedDirectedPathsImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPathsImpl deepClone() {
    return new ManagedDirectedPathsImpl(this, true);
  }

}
