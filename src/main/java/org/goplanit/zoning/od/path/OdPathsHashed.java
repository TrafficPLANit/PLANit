package org.goplanit.zoning.od.path;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.zonetozone.ZoneToZoneHashedImpl;
import org.goplanit.utils.zoning.zonetozone.ZoneToZoneHashedIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.OdZones;

/**
 * Stores paths by their origin and destination, by a unique hash of the combined ids of the od zones. This is memory
 * efficient: a single hash based container instead of as many containers as there are origins. It also takes only a
 * single lookup despite the two keys (o and d).
 *
 * @author markr
 * @param <T> type of path
 */
public class OdPathsHashed<T extends ManagedDirectedPath> extends ZoneToZoneHashedImpl<T> implements OdPaths<T> {

  /**
   * Wrapper around hashed iterator for od paths
   *
   * @param <U> type of path
   */
  public static class OdPathsHashedIterator<U extends ManagedDirectedPath> extends ZoneToZoneHashedIterator<U>
      implements OdPathIterator<U> {

    public OdPathsHashedIterator(OdPathsHashed<U> container) {
      super(container, container.zones);
    }

  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param pathClass type of the paths in container
   * @param zones   the zones being used
   */
  public OdPathsHashed(final IdGroupingToken groupId, Class<T> pathClass, final OdZones zones) {
    super(OdPathsHashed.class, groupId, pathClass, zones);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy from
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public OdPathsHashed(final OdPathsHashed<? extends T> other, boolean deepCopy) {
    super(other);
    if(deepCopy){
      this.odHashed.clear();
      other.zones.forEach(
              origin -> other.zones.forEach(
                      destination -> other.odHashed.values().forEach(
                              original -> setValue( origin, destination, ((T)original.deepClone()))
                      )
              )
      );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long determineTotalPaths() {
    return odHashed.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OdPathsHashedIterator<T> iterator() {
    return new OdPathsHashedIterator<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathsHashed<T> shallowClone() {
    return new OdPathsHashed<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathsHashed<T> deepClone() {
    return new OdPathsHashed<>(this, true);
  }

  // getters - setters

}
