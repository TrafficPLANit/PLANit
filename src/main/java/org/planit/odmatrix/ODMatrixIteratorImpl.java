package org.planit.odmatrix;

import org.ojalgo.array.Array2D;
import org.planit.utils.Pair;

/**
 * Implementation of ODMatrixIterator over entries masking its matrix based
 * container
 * 
 * @author markr
 */
public class ODMatrixIteratorImpl implements ODMatrixIterator {

	private int originId = 0;
	private int destinationId = 0;
	private int currentOriginId = 1;
	private int currentDestinationId = 1;
	private int beyondEndID;
	boolean hasNext;
	double currentValue = -1.0;

	/**
	 * the trips of this matrix
	 */
	protected final Array2D<Double> matrixContents;

	private double get(long originZone, long destinationZone) {
		return matrixContents.get(originZone, destinationZone);
	}

	public ODMatrixIteratorImpl(Array2D<Double> demandMatrixContents, int numberOfTravelAnalysisZones) {
		this.matrixContents = demandMatrixContents;
		beyondEndID = numberOfTravelAnalysisZones - 1;
		hasNext = numberOfTravelAnalysisZones > 0;
	}

	/**
	 * Tests whether this iterator has more object to find
	 * 
	 * @return true if more object available, false otherwise
	 */
	@Override
	public boolean hasNext() {
		return hasNext;
	}

	/**
	 * Returns the value in the next cell of the origin-demand matrix
	 * 
	 * @return the value of the demand in the next cell
	 */
	@Override
	public Double next() {
		currentDestinationId = destinationId + 1;
		currentOriginId = originId + 1;
		currentValue = get(originId, destinationId);
		if (destinationId < beyondEndID) {
			++destinationId;
		} else if (originId < beyondEndID) {
			++originId;
			destinationId = 0;
		} else {
			hasNext = false;
		}
		return currentValue;
	}

	/**
	 * Returns the value of the current cell without advancing the iteration
	 * 
	 * @return the value of the demand in the current cell
	 */
	@Override
	public double getCurrentValue() {
		return currentValue;
	}

	/**
	 * Get the id of the origin of the current cell
	 * 
	 * @return id of the origin of the current cell
	 */
	@Override
	public long getCurrentOriginId() {
		return currentOriginId - 1;
	}

	/**
	 * Get the external id of the origin of the current cell
	 * 
	 * @return external id of the origin of the current cell
	 */
	@Override
	public long getCurrentOriginExternalId() {
		return currentOriginId;
	}

	/**
	 * Get the id of the destination of the current cell
	 * 
	 * @return id of the destination of the current cell
	 */
	@Override
	public long getCurrentDestinationId() {
		return currentDestinationId - 1;
	}

	/**
	 * Get the external id of the destination of the current cell
	 * 
	 * @return external id of the destination of the current cell
	 */
	@Override
	public long getCurrentDestinationExternalId() {
		return currentDestinationId;
	}

	/**
	 * Returns the origin and destination of the current cell as a Pair
	 * 
	 * @return Pair containing the origin row and destination column of the current
	 *         cell
	 */
	@Override
	public Pair<Integer, Integer> getCurrentODPair() {
		return new Pair<Integer, Integer>(currentOriginId, currentDestinationId);
	}
}
