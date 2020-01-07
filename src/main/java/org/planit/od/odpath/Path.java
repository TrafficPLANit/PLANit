package org.planit.od.odpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.planit.network.EdgeSegment;
import org.planit.network.Vertex;
import org.planit.network.physical.LinkSegment;
import org.planit.network.virtual.Centroid;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;

/**
 * This object stores a path of LinkSegment objects from a specified origin to a specified destination
 * 
 * @author gman6028
 *
 */
public class Path {
	
	/**
	 * List containing the link segments in the path
	 */
	private List<LinkSegment> path;
	
	/**
	 * Returns the position of a specified vertex in the vertexPathAndCost array, or -1 if the vertex is not present
	 * 
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 * @param vertex the specified vertex
	 * @return the position of a specified vertex in the vertexPathAndCost array, or -1 if the vertex is not present
	 */
	private int getPositionOfVertex(Pair<Double, EdgeSegment>[] vertexPathAndCost, Vertex vertex) {
	    // MARK 7-1 START WITH THIS <--- position of vertex should always be the same as its id. Make sure
	    // this is the case and if not this is a BUG in the construction/population of the array
	    // We shold NOT use this workaround but instead fix the bug (if it is one).
	    //
	    // Once identified REMOVE THIS METHOD AND COLLECT IT DIRECTLY
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
	 * Create the path from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	private void createPath(Zone origin, Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
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
	}
	
	/**
	 * Constructor
	 * 
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public Path(Zone origin, Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		path = new ArrayList<LinkSegment>();
		createPath(origin, destination, vertexPathAndCost);
	}

	/**
	 * Returns the path as a List of LinkSegment objects
	 * 
	 * @return the path as a List of LinkSegment objects
	 */
	public List<LinkSegment> getPathAsList() {
		return path;
	}
}
