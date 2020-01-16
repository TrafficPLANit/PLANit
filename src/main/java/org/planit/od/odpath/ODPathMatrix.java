package org.planit.od.odpath;

import org.planit.network.EdgeSegment;
import org.planit.od.ODDataImpl;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * This class stores the Path objects from each origin to each destination.
 * 
 * @author gman6028
 *
 */
public class ODPathMatrix extends ODDataImpl<Path> {
	
	/**
	 * Array storing path for each origin-destination pair
	*/
	private Path[][] matrixContents;
	
	/**
	 * Constructor
	 * 
	 * @param zones the zones being used
	 */
    public ODPathMatrix(Zoning.Zones zones) {
        super(zones);
        int numberOfTravelAnalysisZones = zones.getNumberOfZones();
        matrixContents = new Path[numberOfTravelAnalysisZones][numberOfTravelAnalysisZones];
    }
    
    /**
     *  Returns the path for a specified origin and destination
     *  
     *  @param origin the specified origin zone
     *  @param destination the specified destination zone
     *  @return the path from the origin to the destination
     */
	@Override
	public Path getValue(Zone origin, Zone destination) {
		int originId = (int) origin.getId();
		int destinationId = (int) destination.getId();
		return matrixContents[originId][destinationId];
	}

	/**
	 * Create and save the Path object from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public void createAndSavePath(Zone origin, Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		Path path = new Path(destination, vertexPathAndCost);
		setValue(origin, destination, path);
	}

	/**
	 * Set the path from a specified origin to a specified destination
	 * 
	 * @param the specified origin zone
	 * @param the specified destination zone
	 * @param path the Path object from the origin to the destination
	 * 
	 */
	@Override
	public void setValue(Zone origin, Zone destination, Path path) {
		int originId = (int) origin.getId();
		int destinationId = (int) destination.getId();
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