package org.planit.od.odmatrix;

import org.ojalgo.array.Array2D;
import org.planit.utils.zoning.OdZones;

/**
 * Iterator of OdMatrix with primitives stored as Array2D
 * 
 * @author markr
 *
 * @param <T> type of primitive used which must be a Number derived type
 */
public class OdPrimitiveMatrixIterator<T extends Number> extends OdMatrixIterator<T, Array2D<T>> {

  /**
   * Constructor
   * 
   * @param matrixContents in preferred container format for primitives
   * @param zones          zones
   */
  public OdPrimitiveMatrixIterator(Array2D<T> matrixContents, OdZones zones) {
    super(matrixContents, zones);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getCurrentValue() {
    return getMatrixContent().get(originId, destinationId);
  }

}
