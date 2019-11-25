package org.planit.od.odpath;

import java.util.List;

import org.planit.network.physical.Node;
import org.planit.od.ODDataIteratorImpl;
import org.planit.zoning.Zoning;

/**
 * Iterator which runs through rows and columns of an OD path object, making the value, row and column of each cell available
 * 
 * @author gman6028
 *
 */
public class ODPathIterator extends ODDataIteratorImpl<List<Node>> {

	/**
	 * the path for each origin-destination cell
	 */
	private List<Node>[][] matrixContents;

	/**
	 * Constructor
	 * 
	 * @param matrixContents the path for each origin-destination cell
	 * @param zones the zones in the current zoning
	 */
	public ODPathIterator(List<Node>[][] matrixContents, Zoning.Zones zones) {
		super(zones);
		this.matrixContents = matrixContents;
	}

    /**
     * Returns the path in the current cell
     * 
     * @return the path the current cell
     */
	@Override
	public List<Node> getCurrentValue() {
		return matrixContents[originId][destinationId];
	}

    /**
     * Returns the path in the next cell and increments the current position
     * 
     * @return the path the next cell
     */
	@Override
	public List<Node> next() {
		updateCurrentLocation();
		return matrixContents[originId][destinationId];
	}

}