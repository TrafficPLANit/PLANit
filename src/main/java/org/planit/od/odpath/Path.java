package org.planit.od.odpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.planit.network.EdgeSegment;
import org.planit.utils.Pair;
import org.planit.zoning.Zone;

/**
 * This object creates a path of LinkSegment objects to a specified destination using the vertexPathAndCost object created by the (Dijkstra) Shortest Path Algorithm
 * 
 * The path creation makes use of the fact that the origin pair will have a null EdgeSegment, so there is no need to specify the origin.
 * 
 * @author gman6028
 *
 */
public class Path {
	
	/**
	 * List containing the edge segments in the path
	 */
	
	private List<EdgeSegment> path;
	/**
	 * Create the path from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * This method makes use of the fact that the origin pair in the vertexPathAndCost array has a null EdgeSegment.
	 * It searches through vertexPathAndCost from the destination centroid until it finds a null EdgeSegment, which must 
	 * represent the origin centroid.  This is quicker than doing an instanceof test to determine whether the 
	 * upstream vertex is a physical node.
	 * 
	 * 
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	private void createPath(Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		int downstreamVertexId = (int) destination.getCentroid().getId();
		EdgeSegment edgeSegment = vertexPathAndCost[downstreamVertexId].getSecond();
		do {
			path.add(edgeSegment);
			downstreamVertexId = (int) edgeSegment.getUpstreamVertex().getId();
			edgeSegment = vertexPathAndCost[downstreamVertexId].getSecond();
		} while (edgeSegment != null);
		Collections.reverse(path);
	}
	
	/**
	 * Constructor
	 * 
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	public Path(Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		path = new ArrayList<EdgeSegment>();
		createPath(destination, vertexPathAndCost);
	}

	/**
	 * Returns the path as a List of EdgeSegment objects
	 * 
	 * @return the path as a List of EdgeSegment objects
	 */
	public List<EdgeSegment> getPathAsList() {
		return path;
	}
}