package org.planit.od.path;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.od.OdHashedImpl;
import org.planit.utils.od.OdHashedIterator;
import org.planit.utils.path.DirectedPath;
import org.planit.utils.zoning.OdZones;

/**
 * This class stores paths by their origin and destination by creating a unique hash for the combined ids of the od zones. This results in a memory efficient implementation
 * requiring only a single hash based container, instead of having as many containers as their are origins. It also means only conducting a single lookup despite the fact we have
 * two keys (o and d).
 *
 * @author markr
 *
 */
public class OdPathsHashed extends OdHashedImpl<DirectedPath> implements OdPaths {

  /**
   * Wrapper around hashed iterator for od paths
   * 
   */
  public class OdPathsHashedIterator extends OdHashedIterator<DirectedPath> {

    public OdPathsHashedIterator(OdPathsHashed container) {
      super(container, container.zones);
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
   * Copy constructor (shallow copy of contents)
   * 
   * @param other to copy from
   */
  public OdPathsHashed(final OdPathsHashed other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathsHashedIterator iterator() {
    return new OdPathsHashedIterator(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathsHashed clone() {
    return new OdPathsHashed(this);
  }

  // getters - setters

}
