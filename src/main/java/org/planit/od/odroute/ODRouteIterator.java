package org.planit.od.odroute;

import org.planit.network.virtual.Zoning;
import org.planit.od.ODDataIteratorImpl;
import org.planit.route.Route;

/**
 * Iterator which runs through rows and columns of a matrix of Path objects, making the Path, row and column of each cell available
 * 
 * @author gman6028
 *
 */
public class ODRouteIterator extends ODDataIteratorImpl<Route> {

	/**
	 * array containing the Path object for each OD cell
	 */
	private Route[][] matrixContents;

	/**
	 * Constructor
	 * 
	 * @param matrixContents matrix of Path objects for each origin-destination cell
	 * @param zones the zones in the current zoning
	 */
	public ODRouteIterator(Route[][] matrixContents, Zoning.Zones zones) {
		super(zones);
		this.matrixContents = matrixContents;
	}

    /**
     * Returns the path in the current cell
     * 
     * @return the Path in the current cell
     */
	@Override
	public Route getCurrentValue() {
		return matrixContents[originId][destinationId];
	}

    /**
     * Returns the path in the next cell and increments the current position
     * 
     * @return the path the next cell
     */
	@Override
	public Route next() {
		updateCurrentLocation();
		return matrixContents[originId][destinationId];
	}

}