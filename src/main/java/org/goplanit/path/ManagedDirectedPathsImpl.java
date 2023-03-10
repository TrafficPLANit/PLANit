package org.goplanit.path;

import org.goplanit.network.layers.MacroscopicNetworkLayersImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.path.ManagedDirectedPaths;

import java.util.function.BiConsumer;

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
   * @param mapper to use for tracking mapping between original and copied entities (may be null)
   */
  public ManagedDirectedPathsImpl(ManagedDirectedPathsImpl directedPathsImpl, boolean deepCopy, BiConsumer<ManagedDirectedPath, ManagedDirectedPath> mapper) {
    super(directedPathsImpl, deepCopy, mapper);
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
  public ManagedDirectedPathsImpl shallowClone() {
    return new ManagedDirectedPathsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPathsImpl deepClone() {
    return new ManagedDirectedPathsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ManagedDirectedPathsImpl deepCloneWithMapping(BiConsumer<ManagedDirectedPath, ManagedDirectedPath> mapper) {
    return new ManagedDirectedPathsImpl(this, true, mapper);
  }

}
