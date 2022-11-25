package org.goplanit.od.path;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.od.OdNonPrimitiveMatrix;
import org.goplanit.utils.od.OdNonPrimitiveMatrixIterator;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.zoning.OdZones;

/**
 * This class stores the path objects from each origin to each destination in a full matrix form
 *
 * @author gman6028, markr
 *
 */
public class OdPathMatrix extends OdNonPrimitiveMatrix<ManagedDirectedPath> implements OdPaths {

  /**
   * Wrapper around primitive matrix iterator
   * 
   * @author markr
   */
  public class OdPathMatrixIterator extends OdNonPrimitiveMatrixIterator<ManagedDirectedPath> implements OdPathIterator {

    public OdPathMatrixIterator(final OdPathMatrix matrix) {
      super(matrix.matrixContents, matrix.zones);
    }
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param zones   the zones being used
   */
  public OdPathMatrix(final IdGroupingToken groupId, final OdZones zones) {
    super(OdPathMatrix.class, groupId, zones, new ManagedDirectedPath[zones.size()][zones.size()]);
  }

  /**
   * Copy constructor (shallow copy of matrix contents)
   * 
   * @param odPathMatrix to copy from
   */
  public OdPathMatrix(final OdPathMatrix odPathMatrix) {
    super(odPathMatrix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathMatrixIterator iterator() {
    return new OdPathMatrixIterator(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdPathMatrix clone() {
    return new OdPathMatrix(this);
  }

  // getters - setters

}
