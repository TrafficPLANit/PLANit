package org.planit.path;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.path.DirectedPathFactory;
import org.planit.utils.path.DirectedPaths;

/**
 * Implementation of DirectedPaths interface
 * 
 * @author markr
 */
public class DirectedPathsImpl extends ManagedIdEntitiesImpl<DirectedPath> implements DirectedPaths {

  /** factory to use */
  private final DirectedPathFactory directedPathFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public DirectedPathsImpl(final IdGroupingToken groupId) {
    super(DirectedPath::getId);
    this.directedPathFactory = new DirectedPathFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId             to use for creating ids for instances
   * @param directedPathFactory the factory to use
   */
  public DirectedPathsImpl(final IdGroupingToken groupId, DirectedPathFactory directedPathFactory) {
    super(DirectedPath::getId);
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
  public DirectedPathFactory getFactory() {
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
