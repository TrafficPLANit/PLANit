package org.goplanit.od.path;

import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.Zone;

import java.util.Collection;
import java.util.List;

import static org.goplanit.od.path.OdPath2MultiPathWrapper.getContainer;

/**
 * Iterator that wraps an iterator for single paths per od as if it has multiple-paths per od.
 *
 * @param <T> type of path
 * @param <U> type of container used wrap single-path per od in
 */
public class OdPath2MultiPathWrapperIterator <T extends ManagedDirectedPath, U extends List<T>> implements OdMultiPathIterator<T, U>{

  private OdPathIterator<T> pathIteratorToWrap;

  private final Class<U> containerClazz;

  /** re-use this container if allowed, otherwise create them on the fly for each call */
  private final U dummyContainer;

  private U currentValue = null;

  private boolean allowReuseOfContainer = false;

  /**
   * Constructor
   *
   * @param containerClazz container to use
   * @param allowReuseOfContainer flag indicating if we can reuse the container to wrap path per od in
   * @param pathIteratorToWrap iterator of wrapped single path per od container
   */
  public OdPath2MultiPathWrapperIterator(Class<U> containerClazz, boolean allowReuseOfContainer, OdPathIterator<T> pathIteratorToWrap) {
    this.pathIteratorToWrap = pathIteratorToWrap;
    this.containerClazz = containerClazz;
    this.dummyContainer = OdPath2MultiPathWrapper.createContainer(containerClazz);
    this.allowReuseOfContainer = allowReuseOfContainer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getCurrentOrigin() {
    return pathIteratorToWrap.getCurrentOrigin();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zone getCurrentDestination() {
    return pathIteratorToWrap.getCurrentDestination();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public U getCurrentValue() {
    return currentValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNext() {
    return pathIteratorToWrap.hasNext();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public U next() {
    this.currentValue = getContainer(allowReuseOfContainer, dummyContainer, containerClazz);
    var wrappedValue =  pathIteratorToWrap.next();
    if(wrappedValue != null) {
      currentValue.add(wrappedValue);
    }
    return currentValue;
  }
}
