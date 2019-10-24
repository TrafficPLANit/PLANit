package org.planit.od.odmatrix;

import org.ojalgo.array.Array2D;
import org.planit.od.ODDataIterator;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

public class ODMatrixIterator implements ODDataIterator<Double> {

	private int originId;
	private int destinationId;
	private int numberOfTravelAnalysisZones;
	private int currentLocation;
	private Zoning.Zones zones;

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
		this.matrixContents = matrixContents;
		this.zones = zones;
		this.numberOfTravelAnalysisZones = zones.getNumberOfZones();
		currentLocation = 0;
	}

   /**
    * Tests whether there are any more cells to iterate through
    * 
    * @return true if there are more cells to iterate through, false otherwise
    */
	@Override
	public boolean hasNext() {
		return currentLocation < numberOfTravelAnalysisZones * numberOfTravelAnalysisZones;
	}

    /**
     * Returns the value in the current cells and increments the current position
     * 
     * @return the value in the current cell
     */
	@Override
	public Double next() {
		originId = currentLocation / numberOfTravelAnalysisZones;
		destinationId = currentLocation % numberOfTravelAnalysisZones;
		currentLocation++;
		return matrixContents.get(originId, destinationId);
	}

	/**
	 * Returns the origin zone object for the current cell
	 * 
	 * @return the origin zone object at the current cell
	 */
	@Override
	public Zone getCurrentOrigin() {
		return zones.getZone(originId);
	}

    /**
     * Returns the destination zone object for the current cell
     * 
     * @return the destination zone object for the current cell
     */
	@Override
	public Zone getCurrentDestination() {
		return zones.getZone(destinationId);
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
