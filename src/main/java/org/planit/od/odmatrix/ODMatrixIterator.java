package org.planit.od.odmatrix;

import org.ojalgo.array.Array2D;
import org.planit.network.virtual.Zoning;
import org.planit.od.ODDataIteratorImpl;

/**
 * Iterator which runs through rows and columns of an OD matrix, making the value, row and column of each cell available
 * 
 * @author gman6028
 *
 */
public class ODMatrixIterator extends ODDataIteratorImpl<Double> {

	/**
	 * the trips of this matrix
	 */
	private final Array2D<Double> matrixContents;

  /**
   * Constructor
   * 
   * @param matrixContents matrix object containing the data to be iterated through
   * @param zones Zones object defining the zones in the network
   */
	public ODMatrixIterator(Array2D<Double> matrixContents, Zoning.Zones zones) {
		super(zones);
		this.matrixContents = matrixContents;
	}

    /**
     * Returns the value in the current cells and increments the current position
     * 
     * @return the value of the next cell
     */
	@Override
	public Double next() {
		updateCurrentLocation();
		return matrixContents.get(originId, destinationId);
	}

    /**
     * Returns the value at the current cell
     * 
     * @return the value at the current cell
     */
	@Override
	public Double getCurrentValue() {
		return matrixContents.get(originId, destinationId);
	}

}