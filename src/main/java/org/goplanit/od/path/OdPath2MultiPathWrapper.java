package org.goplanit.od.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdDataImpl;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.reflection.ReflectionUtils;
import org.goplanit.utils.zoning.Zone;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class allows an OdPath instance to be wrapped such that it can be processed as if it is an OD multi-path instance. Not to be used to add
 * paths, only to read existing paths.
 * <p>
 *   This should not be used unless there is significant benefit of using the OdPath approach during computatinal costly exercises, e.g.,
 *   assignment, and the conversion to multi-path only occurs very infrequently, since the conversion requires on-the-fly creation
 *   of container instances which is computationally costly. It may be worth the trade-off as it may save memory during simulation since
 *   single paths per od require no container within the odPath instances.
 * </p>
 *
 * @author markr
 *
 */
public class OdPath2MultiPathWrapper<T extends ManagedDirectedPath, U extends List<T>> extends OdDataImpl<U> implements OdMultiPaths<T, U> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(OdPath2MultiPathWrapper.class.getCanonicalName());

  /** wrapping this single path per od instance */
  private final OdPaths<T> odSinglePaths;

  private final Class<U> containerClazz;

  /** re-use this container if allowed, otherwise create them on the fly for each call */
  private final U dummyContainer;

  private boolean allowReuseContainer = false;

  /**
   * Create a container of desired type
   * @return container
   */
  static <W extends ManagedDirectedPath, V extends Collection<W>> V  createContainer(Class<V> containerClazz){
    return ReflectionUtils.createTypedInstance(containerClazz.getCanonicalName());
  }

  /**
   * Given configuration get access to container to use
   *
   * @return container to use for wrapping path
   */
  static <W extends ManagedDirectedPath, V extends List<W>> V getContainer(boolean allowReuseContainer, final V dummyContainer, final Class<V> containerClazz){
    if(allowReuseContainer){
      dummyContainer.clear();
      return dummyContainer;
    }else{
      return createContainer(containerClazz);
    }
  }

  /**
   * Given configuration get access to container to use
   *
   * @return container to use for wrapping path
   */
  private U getContainer(){
    return getContainer(isAllowReuseContainer(), dummyContainer, containerClazz);
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param odSinglePaths the odSinglePaths we are wrapping
   * @param containerClazz class of container
   */
  public OdPath2MultiPathWrapper(
          final IdGroupingToken groupId, OdPaths<T> odSinglePaths, Class<U> containerClazz) {
    super(OdPath2MultiPathWrapper.class, groupId, containerClazz, null /* zones are already in the od single paths we wrap */);
    this.odSinglePaths = odSinglePaths;
    this.containerClazz = containerClazz;
    this.dummyContainer =createContainer(containerClazz);
  }

  /**
   * Copy constructor
   *
   * @param other to copy from
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public OdPath2MultiPathWrapper(final OdPath2MultiPathWrapper<T, U> other, boolean deepCopy) {
    super(other);
    this.odSinglePaths = deepCopy ? other.odSinglePaths.deepClone() : other.odSinglePaths.shallowClone();
    this.containerClazz = other.containerClazz;
    this.dummyContainer = createContainer(containerClazz);
    this.allowReuseContainer = other.allowReuseContainer;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public U getValue(Zone origin, Zone destination) {
    return getValue(origin.getId(), destination.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public U getValue(long originId, long destinationId) {
    U container = getContainer();
    container.add(odSinglePaths.getValue(originId,destinationId));
    return container;
  }

  /**
   * Not allowed, read-only
   *
   * @param origin      specified origin
   * @param destination specified destination
   * @param value       value at the specified cell
   */
  @Override
  public void setValue(Zone origin, Zone destination, U value) {
    LOGGER.severe("singlePath2MultiPathWrappr is read-only, attempt to add to it is ignored");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfOdZones() {
    return odSinglePaths.getNumberOfOdZones();
  }

  @Override
  public OdMultiPathIterator<T, U> iterator() {
    return new OdPath2MultiPathWrapperIterator<T,U>(containerClazz, isAllowReuseContainer(), odSinglePaths.iterator());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long determineTotalPaths() {
    return odSinglePaths.determineTotalPaths();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPath2MultiPathWrapper<T, U> shallowClone() {
    return new OdPath2MultiPathWrapper<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPath2MultiPathWrapper<T, U> deepClone() {
    return new OdPath2MultiPathWrapper<>(this, true);
  }

  // getters - setters

  /** flag indicating f we can reuse the same dummy container when getting values. Default is false
   *
   * @return flag
   */
  public boolean isAllowReuseContainer() {
    return allowReuseContainer;
  }

  /** flag indicating f we can reuse the same dummy container when getting values.
   *
   * @param flag the flag to set
   */
  public void setAllowReuseContainer(boolean flag) {
    this.allowReuseContainer = flag;
  }


}
