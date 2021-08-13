package org.planit.od.odmatrix;

import org.planit.od.OdData;

/**
 * This class contains common methods for handling origin-demand matrices of a certain type where the entire matrix is modelled even if some combinations of ODs do not contain any
 * data.
 * 
 * @author gman6028, markr
 *
 */
public interface OdMatrix<T, U> extends OdData<T> {

  /**
   * Returns an iterator which can iterate through all the origin-destination cells in the matrix
   * 
   * @return iterator through all the origin-destination cells
   */
  @Override
  public abstract OdMatrixIterator<T, U> iterator();
}
