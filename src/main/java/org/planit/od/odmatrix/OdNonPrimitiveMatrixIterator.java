package org.planit.od.odmatrix;

import org.planit.utils.zoning.OdZones;

/**
 * Iterator of OdMatrix with objects stored as raw two dimensional array
 * 
 * @author markr
 *
 * @param <T> type of values in od matrix
 */
public class OdNonPrimitiveMatrixIterator<T> extends OdMatrixIterator<T, T[][]> {

  /**
   * Constructor
   * 
   * @param matrixContents in preferred container format for values
   * @param zones          zones
   */
  public OdNonPrimitiveMatrixIterator(T[][] matrixContents, OdZones zones) {
    super(matrixContents, zones);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getCurrentValue() {
    return getMatrixContent()[originId][destinationId];
  }

}
