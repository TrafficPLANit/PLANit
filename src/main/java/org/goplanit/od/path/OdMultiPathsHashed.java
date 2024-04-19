package org.goplanit.od.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdHashedImpl;
import org.goplanit.utils.od.OdHashedIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.reflection.ReflectionUtils;
import org.goplanit.utils.zoning.OdZones;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class stores (multiple) paths per origin and destination by creating a unique hash for the combined ids of the od zones. This results in a memory efficient implementation
 * on the Od aspect of the data, requiring only a single hash based container, instead of having containers for each origin (and or destination).
 * It also means only conducting a single lookup despite the fact we have two keys (o and d).
 *
 * @author markr
 *
 */
public class OdMultiPathsHashed<T extends ManagedDirectedPath, U extends Collection<T>> extends OdHashedImpl<U> implements OdMultiPaths<T, U> {

  /**
   * Wrapper around hashed iterator for od paths
   *
   */
  public static class OdPathsHashedIterator<V extends ManagedDirectedPath, W extends Collection<V>> extends OdHashedIterator<W> implements OdMultiPathIterator<V,W> {

    public OdPathsHashedIterator(OdMultiPathsHashed<V,W> container) {
      super(container, container.zones);
    }

  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param zones   the zones being used
   */
  public OdMultiPathsHashed(final IdGroupingToken groupId, Class<U> multiPathContainerClass, final OdZones zones) {
    super(OdMultiPathsHashed.class, groupId, multiPathContainerClass, zones);
  }

  /**
   * Copy constructor
   *
   * @param other to copy from
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public OdMultiPathsHashed(final OdMultiPathsHashed<T,U> other, boolean deepCopy) {
    super(other);
    this.odHashed.clear();
    Function<ManagedDirectedPath, ManagedDirectedPath> cloneFunc =
            deepCopy ? ManagedDirectedPath::deepClone : ManagedDirectedPath::shallowClone;
    other.zones.forEach(
            origin -> other.zones.forEach(
                    destination -> other.odHashed.values().forEach(original ->
                            setValue(origin, destination,
                                    original.stream().map(e -> (T) cloneFunc.apply(e)).collect(Collectors.toList())))));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathsHashedIterator<T> iterator() {
    return new OdPathsHashedIterator<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdMultiPathsHashed<T> shallowClone() {
    return new OdMultiPathsHashed<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdMultiPathsHashed<T> deepClone() {
    return new OdMultiPathsHashed<>(this, true);
  }

  // getters - setters

}
