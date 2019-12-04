package org.planit.od.odpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.network.physical.LinkSegment;
import org.planit.network.virtual.Centroid;
import org.planit.od.ODDataImpl;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * This class stores the path from each origin to each destination.
 * 
 * @author gman6028
 *
 */
public class ODPathMatrix extends ODDataImpl<List<LinkSegment>> {
	
	/**
	 * Array storing path for each origin-destination pair
	*/
	private List<LinkSegment>[][] matrixContents;
	
	/**
	 * Returns the position of a specified vertex in the vertexPathAndCost array, or -1 if the vertex is not present
	 * 
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 * @param vertex the specified vertex
	 * @return the position of a specified vertex in the vertexPathAndCost array, or -1 if the vertex is not present
	 */
	private int getPositionOfVertex(Pair<Double, EdgeSegment>[] vertexPathAndCost, Vertex vertex) {
		for (int i=0; i<vertexPathAndCost.length; i++) {
			if (vertexPathAndCost[i] != null) {
				if (vertexPathAndCost[i].getSecond() != null) {
					if (vertexPathAndCost[i].getSecond().getDownstreamVertex().getId() == vertex.getId()) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * Constructor
	 * 
	 * @param zones the zones being used
	 */
    public ODPathMatrix(Zoning.Zones zones) {
        super(zones);
        int numberOfTravelAnalysisZones = zones.getNumberOfZones();
        matrixContents = new List[numberOfTravelAnalysisZones][numberOfTravelAnalysisZones];
    }
    
    /**
     *  Returns the path for a specified origin and destination
     *  
     *  @param origin the specified origin zone
     *  @param destination the specified destination zone
     *  @return the path from the origin to the destination
     */
	@Override
	public List<LinkSegment> getValue(Zone origin, Zone destination) {
		int originId = (int) origin.getId();
		int destinationId = (int) destination.getId();
		return matrixContents[originId][destinationId];
	}

	/**
	 * Create and save the path from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public void createAndSavePath(Zone origin, Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		List<LinkSegment> path = new ArrayList<LinkSegment>();
		Centroid destinationCentroid = destination.getCentroid();
		int position = getPositionOfVertex(vertexPathAndCost, destinationCentroid);
		for (Vertex vertex = destinationCentroid; position != -1; position = getPositionOfVertex(vertexPathAndCost, vertex)) {
			EdgeSegment edgeSegment = vertexPathAndCost[position].getSecond();
			vertex = edgeSegment.getUpstreamVertex();
			if (edgeSegment instanceof LinkSegment) {
				LinkSegment linkSegment = (LinkSegment) edgeSegment;
				path.add(linkSegment);
			}
		}
		//need to reverse the order of the path since the assignment works from the destination to the origin
		Collections.reverse(path);
		setValue(origin, destination, path);
	}

	/**
	 * Set the path from a specified origin to a specified destination
	 * 
	 * @param the specified origin zone
	 * @param the specified destination zone
	 * @param odPath the path from the origin to the destination
	 * 
	 */
	@Override
	public void setValue(Zone origin, Zone destination, List<LinkSegment> path) {
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