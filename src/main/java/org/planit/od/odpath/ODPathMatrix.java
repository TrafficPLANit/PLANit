package org.planit.od.odpath;

import org.planit.network.virtual.Zoning;
import org.planit.od.ODDataImpl;
import org.planit.route.Route;
import org.planit.route.RouteImpl;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.virtual.Zone;

/**
 * This class stores the Path objects from each origin to each destination.
 *
 * @author gman6028
 *
 */
public class ODPathMatrix extends ODDataImpl<Route> {

	/**
	 * Array storing path for each origin-destination pair
	*/
	private final Route[][] matrixContents;

	/**
	 * Constructor
	 *
	 * @param zones the zones being used
	 */
    public ODPathMatrix(final Zoning.Zones zones) {
        super(zones);
        final int numberOfTravelAnalysisZones = zones.getNumberOfZones();
        matrixContents = new Route[numberOfTravelAnalysisZones][numberOfTravelAnalysisZones];
    }

    /**
     *  Returns the path for a specified origin and destination
     *
     *  @param origin the specified origin zone
     *  @param destination the specified destination zone
     *  @return the path from the origin to the destination
     */
	@Override
	public Route getValue(final Zone origin, final Zone destination) {
		final int originId = (int) origin.getId();
		final int destinationId = (int) destination.getId();
		return matrixContents[originId][destinationId];
	}

	/**
	 * Create and save the Path object from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 *
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public void createAndSavePath(final Zone origin, final Zone destination, final Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		setValue(origin, destination, RouteImpl.createODRoute(destination.getCentroid(), vertexPathAndCost));
	}

	/**
	 * Set the path from a specified origin to a specified destination
	 *
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param path the Path object from the origin to the destination
	 *
	 */
	@Override
	public void setValue(final Zone origin, final Zone destination, final Route path) {
		final int originId = (int) origin.getId();
		final int destinationId = (int) destination.getId();
        matrixContents[originId][destinationId] = path;
	}

	/**
	 * Returns an iterator which can iterate through all the origin-destination cells in the matrix
	 *
	 * @return iterator through all the origin-destination cells
	 */
	@Override
	public ODPathIterator iterator() {
    	return new ODPathIterator(matrixContents, zones);
	}

}