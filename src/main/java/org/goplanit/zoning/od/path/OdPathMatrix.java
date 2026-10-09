package org.goplanit.zoning.od.path;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.zonetozone.ZoneToZoneNonPrimitiveMatrix;
import org.goplanit.utils.zoning.zonetozone.ZoneToZoneNonPrimitiveMatrixIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.OdZones;

/**
 * This class stores the path objects from each origin to each destination in a full matrix form
 *
 * @author gman6028, markr
 *
 */
public class OdPathMatrix extends ZoneToZoneNonPrimitiveMatrix<ManagedDirectedPath> implements OdPaths<ManagedDirectedPath> {

  /**
   * Wrapper around primitive matrix iterator
   * 
   * @author markr
   */
  public static class OdPathMatrixIterator extends ZoneToZoneNonPrimitiveMatrixIterator<ManagedDirectedPath>
      implements OdPathIterator<ManagedDirectedPath> {

    public OdPathMatrixIterator(final OdPathMatrix matrix) {
      super(matrix.matrixContainer, matrix.zones);
    }
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param zones   the zones being used
   */
  public OdPathMatrix(final IdGroupingToken groupId, final OdZones zones) {
    super(OdPathMatrix.class,
        groupId,
        ManagedDirectedPath.class,
        zones,
        new ManagedDirectedPath[zones.size()][zones.size()]);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy from
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public OdPathMatrix(final OdPathMatrix other, boolean deepCopy) {
    super(other);

    this.matrixContainer = new ManagedDirectedPath[other.zones.size()][other.zones.size()];
    for (var origin : other.zones) {
      for (var destination : other.zones) {
        var currValue = other.getValue(origin, destination);
        setValue(origin, destination, deepCopy ? currValue.deepClone() : currValue);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long determineTotalPaths() {
    return determineNonNullCells();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OdPathMatrixIterator iterator() {
    return new OdPathMatrixIterator(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathMatrix shallowClone() {
    return new OdPathMatrix(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathMatrix deepClone() {
    return new OdPathMatrix(this, true);
  }

  // getters - setters

}
