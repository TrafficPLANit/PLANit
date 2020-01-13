package org.planit.od.odpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
	 * Create the path from a specified origin to a specified destination, using the vertexPathAndCost array as input
	 * 
	 * @param origin the specified origin zone
	 * @param destination the specified destination zone
	 * @param vertexPathAndCost the vertexPathAndCost array (previously calculated by the traffic assignment)
	 */
	private void createPath(Zone origin, Zone destination, Pair<Double, EdgeSegment>[] vertexPathAndCost) {
		List<Pair<Double, EdgeSegment>> vertexList = Arrays.asList(vertexPathAndCost);
		List<Long> idList = vertexList.stream().filter(pair -> pair != null)
				                                                      .filter(pair -> pair.getSecond() != null)
				                                                      .map(pair -> pair.getSecond().getDownstreamVertex().getId())
				                                                      .collect(Collectors.toList());
		Centroid destinationCentroid = destination.getCentroid();
		for (Vertex vertex = destinationCentroid; idList.contains(vertex.getId());) {
			EdgeSegment edgeSegment = vertexPathAndCost[(int) vertex.getId()].getSecond();
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