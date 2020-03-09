package org.planit.od;

import org.planit.network.virtual.Zoning;
import org.planit.utils.network.virtual.Zone;

/**
 * Base class containing common methods required by all classes which implement ODDataIterator
 * 
 * @author gman6028
 *
 * @param <T> the type of data to be stored for each origin-destination cell
 */
public abstract class ODDataIteratorImpl<T> implements ODDataIterator<T> {

	/**
	 * Id of the origin zone
	 */
	protected int originId;
	
	/**
	 * Id of the destination zone
	 */
	protected int destinationId;
	
	/**
	 * Number of travel analysis zones in the OD matrix (used internally, not accessible from other classes)
	 */
	protected int numberOfTravelAnalysisZones;
	
	/**
	 * Marker used to store the current position in the OD matrix (used internally, not accessible from other classes)
	 */
	protected int currentLocation;
	
	/**
	 * Zones object to store travel analysis zones (from Zoning object)
	 */
	protected Zoning.Zones zones;
	
	/**
	 * Increment the location cursor for the next iteration
	 */
	protected void updateCurrentLocation() {
		originId = currentLocation / numberOfTravelAnalysisZones;
		destinationId = currentLocation % numberOfTravelAnalysisZones;
		currentLocation++;
	}

	/**
	 * Constructor
	 * 
     * @param zones zones considered in the matrix
	 */
	public ODDataIteratorImpl(Zoning.Zones zones) {
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
	 * Returns the origin zone object for the current cell
	 * 
	 * @return the origin zone object at the current cell
	 */
	@Override
	public Zone getCurrentOrigin() {
		return zones.getZoneById(originId);
	}

	/**
	  * Returns the destination zone object for the current cell
	  * 
	  * @return the destination zone object for the current cell
	  */
	@Override
	public Zone getCurrentDestination() {
		return zones.getZoneById(destinationId);
	}

}