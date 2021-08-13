package org.planit.od.odmatrix;

import java.util.logging.Logger;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZones;
import org.planit.utils.zoning.Zone;

/**
 * This class contains common methods for handling origin-demand matrices where values are non primitive types, i.e. Objects of some type. Data container used is a raw array of a
 * fixed size based on the Od number of zones. Cells that have no value entry will still take up space in this implementation. If the matrix is dense and contains very little empty
 * cells, this is likely the most computationally efficient implementation, if not, consider other implementations that deal better with sparse matrices in terms of memory usage.
 * 
 * @author gman6028m, markr
 *
 */
public abstract class OdNonPrimitiveMatrix<T> extends OdMatrixImpl<T, T[][]> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OdNonPrimitiveMatrix.class.getCanonicalName());

  /**
   * Constructor for full Od matrix containing non-primitives, i.e. object based
   * 
   * @param idTokenClass   to use for id generation
   * @param idToken        to use for the matrix id
   * @param zones          holder for zones considered in the matrix
   * @param matrixContents container for the matrix contents
   */
  public OdNonPrimitiveMatrix(final Class<?> idTokenClass, final IdGroupingToken idToken, final OdZones zones, final T[][] matrixContents) {
    super(idTokenClass, idToken, zones, matrixContents);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy from
   */
  public OdNonPrimitiveMatrix(OdNonPrimitiveMatrix<T> other) {
    super(other);
  }

  /**
   * Sets the value for a specified origin and destination
   * 
   * @param origin      specified origin
   * @param destination specified destination
   * @param value       value at the specified cell
   */
  @Override
  public void setValue(Zone origin, Zone destination, T value) {
    if (origin.getId() == destination.getId()) {
      // demand or cost from any origin to itself must be zero
      // matrixContents.set(originId, destinationId, 0.0);
      LOGGER.warning(String.format("BEFORE WE WOULD SET THIS TO ZERO --> NOT GENERIC, SO COMMENTED OUT AND WE NOW SET %s", value));
    }

    matrixContents[(int) origin.getId()][(int) destination.getId()] = value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue(Zone origin, Zone destination) {
    return matrixContents[(int) origin.getId()][(int) destination.getId()];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue(long originId, long destinationId) {
    return matrixContents[(int) originId][(int) destinationId];
  }

  /**
   * Returns an iterator which can iterate through all the origin-destination cells in the matrix
   * 
   * @return iterator through all the origin-destination cells
   */
  @Override
  public abstract OdMatrixIterator<T, T[][]> iterator();
}
