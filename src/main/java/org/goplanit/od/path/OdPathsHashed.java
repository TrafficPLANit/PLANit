package org.goplanit.od.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdHashed;
import org.goplanit.utils.od.OdHashedImpl;
import org.goplanit.utils.od.OdHashedIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.utils.zoning.Zone;

/**
 * This class stores paths by their origin and destination by creating a unique hash for the combined ids of the od zones. This results in a memory efficient implementation
 * requiring only a single hash based container, instead of having as many containers as their are origins. It also means only conducting a single lookup despite the fact we have
 * two keys (o and d).
 *
 * @author markr
 *
 */
public class OdPathsHashed<T extends ManagedDirectedPath> extends OdHashedImpl<T> implements OdPaths<T> {

  /**
   * Wrapper around hashed iterator for od paths
   *
   */
  public class OdPathsHashedIterator<T> extends OdHashedIterator<T> {

    public OdPathsHashedIterator(OdPathsHashed<? extends T> container) {
      super((OdHashed<T>) container, container.zones);
    }

  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param zones   the zones being used
   */
  public OdPathsHashed(final IdGroupingToken groupId, final OdZones zones) {
    super(OdPathsHashed.class, groupId, zones);
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
  public OdPathsHashedIterator<T> iterator() {
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
